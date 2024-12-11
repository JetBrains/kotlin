/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.isInlineLambdaBlock
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.isPrivate
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

interface CallInlinerStrategy {
    /**
     * TypeOf function requires some custom backend-specific processing. This is a customization point for that.
     *
     * @param expression is a copy of original IrCall with types substituted by normal rules
     * @param nonSubstitutedTypeArgument is typeArgument of call with only reified type parameters substituted
     *
     * @return new node to insert instead of typeOf call.
     */
    fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression
    fun at(scope: Scope, expression: IrExpression) {}

    object DEFAULT : CallInlinerStrategy {
        override fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression {
            return expression.apply {
                typeArguments[0] = nonSubstitutedTypeArgument
            }
        }
    }
}

enum class InlineMode {
    PRIVATE_INLINE_FUNCTIONS,
    ALL_INLINE_FUNCTIONS,
    ALL_FUNCTIONS,
}

abstract class InlineFunctionResolver(val inlineMode: InlineMode) {
    open val callInlinerStrategy: CallInlinerStrategy
        get() = CallInlinerStrategy.DEFAULT
    open val allowExternalInlining: Boolean
        get() = false

    open fun needsInlining(function: IrFunction) = function.isInline && (allowExternalInlining || !function.isExternal)

    open fun needsInlining(expression: IrFunctionAccessExpression) = needsInlining(expression.symbol.owner)

    open fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (shouldExcludeFunctionFromInlining(symbol)) return null

        val owner = symbol.owner
        return (owner as? IrSimpleFunction)?.resolveFakeOverride() ?: owner
    }

    protected open fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean {
        return !needsInlining(symbol.owner) || Symbols.isTypeOfIntrinsic(symbol)
    }
}

abstract class InlineFunctionResolverReplacingCoroutineIntrinsics<Ctx : LoweringContext>(
    protected val context: Ctx,
    inlineMode: InlineMode,
) : InlineFunctionResolver(inlineMode) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol) ?: return null
        // TODO: Remove these hacks when coroutine intrinsics are fixed.
        return when {
            function.isBuiltInSuspendCoroutineUninterceptedOrReturn() ->
                context.ir.symbols.suspendCoroutineUninterceptedOrReturn.owner

            symbol == context.ir.symbols.coroutineContextGetter ->
                context.ir.symbols.coroutineGetContext.owner

            else -> function
        }
    }

    override fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean {
        return super.shouldExcludeFunctionFromInlining(symbol) ||
                (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS && !symbol.owner.isConsideredAsPrivateForInlining())
    }
}

/**
 * These resolvers are supposed to be run at the first compilation stage for all non-JVM targets.
 */
internal class PreSerializationPrivateInlineFunctionResolver(
    context: LoweringContext,
    override val allowExternalInlining: Boolean,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(context, InlineMode.PRIVATE_INLINE_FUNCTIONS) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol)
        if (function != null) {
            check(function.body != null) { "Unexpected inline function without body: ${function.render()}" }
        }
        return function
    }
}

internal class PreSerializationNonPrivateInlineFunctionResolver(
    context: LoweringContext,
    override val allowExternalInlining: Boolean,
    irMangler: KotlinMangler.IrMangler,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(context, InlineMode.ALL_INLINE_FUNCTIONS) {

    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(irMangler)
    )

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol)
        if (function != null && function.body == null) {
            deserializer.deserializeInlineFunction(function)
        }
        return function
    }
}

open class FunctionInlining(
    val context: LoweringContext,
    private val inlineFunctionResolver: InlineFunctionResolver,
    private val insertAdditionalImplicitCasts: Boolean = true,
    private val regenerateInlinedAnonymousObjects: Boolean = false,
    private val produceOuterThisFields: Boolean = true,
) : IrElementTransformerVoidWithContext(), BodyLoweringPass {
    init {
        require(!produceOuterThisFields || context is CommonBackendContext) {
            "The inliner can generate outer fields only with param `context` of type `CommonBackendContext`"
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // TODO container: IrSymbolDeclaration
        withinScope(container) {
            irBody.accept(this, null)
        }

        irBody.patchDeclarationParents(container as? IrDeclarationParent ?: container.parent)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        return when (declaration) {
            is IrFunction, is IrClass, is IrProperty -> context.irFactory.stageController.restrictTo(declaration) {
                super.visitDeclaration(declaration)
            }
            else -> super.visitDeclaration(declaration)
        }
    }

    fun inline(irModule: IrModuleFragment) = irModule.accept(this, data = null)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid(this)

        if (!inlineFunctionResolver.needsInlining(expression)) return expression

        val calleeSymbol = when (expression) {
            is IrCall -> expression.symbol
            is IrConstructorCall -> expression.symbol
            else -> return expression
        }

        val actualCallee = inlineFunctionResolver.getFunctionDeclaration(calleeSymbol)
        if (actualCallee?.body == null) {
            if (expression is IrCall && Symbols.isTypeOfIntrinsic(calleeSymbol)) {
                inlineFunctionResolver.callInlinerStrategy.at(currentScope!!.scope, expression)
                return inlineFunctionResolver.callInlinerStrategy.postProcessTypeOf(expression, expression.typeArguments[0]!!)
            }
            return expression
        }

        withinScope(actualCallee) {
            actualCallee.body?.transformChildrenVoid()
            actualCallee.parameters.forEachIndexed { index, param ->
                if (expression.arguments[index] == null) {
                    // Default values can recursively reference [callee] - transform only needed.
                    param.defaultValue = param.defaultValue?.transform(this@FunctionInlining, null)
                }
            }
        }

        val parent = allScopes.map { it.irElement }.filterIsInstance<IrDeclarationParent>().lastOrNull()
            ?: allScopes.map { it.irElement }.filterIsInstance<IrDeclaration>().lastOrNull()?.parent

        inlineFunctionResolver.callInlinerStrategy.at(currentScope!!.scope, expression)
        val inliner = CallInlining(
            expression, actualCallee, currentScope!!, parent,
            context,
            inlineFunctionResolver,
            insertAdditionalImplicitCasts,
            produceOuterThisFields
        )
        return inliner.inline().markAsRegenerated()
    }

    private fun IrBlock.markAsRegenerated(): IrBlock {
        if (!regenerateInlinedAnonymousObjects) return this
        acceptVoid(object : IrElementVisitorVoid {
            private fun IrAttributeContainer.setUpCorrectAttributeOwner() {
                if (this.attributeOwnerId == this) return
                this.originalBeforeInline = this.attributeOwnerId
                this.attributeOwnerId = this
            }

            override fun visitElement(element: IrElement) {
                if (element is IrAttributeContainer && element !is IrInlinedFunctionBlock) element.setUpCorrectAttributeOwner()
                element.acceptChildrenVoid(this)
            }
        })
        return this
    }

    private class CallInlining(
        val callSite: IrFunctionAccessExpression,
        val callee: IrFunction,
        val currentScope: ScopeWithIr,
        val parent: IrDeclarationParent?,
        val context: LoweringContext,
        private val inlineFunctionResolver: InlineFunctionResolver,
        private val insertAdditionalImplicitCasts: Boolean,
        private val produceOuterThisFields: Boolean
    ) {
        private val elementsWithLocationToPatch = hashSetOf<IrGetValue>()

        val inlineFunctionBodyPreprocessor = run {
            val typeParameters =
                when (callee) {
                    is IrConstructor -> callee.parentAsClass.typeParameters
                    is IrSimpleFunction -> callee.typeParameters
                }
            val typeArguments =
                callSite.typeArguments.indices.associate {
                    typeParameters[it].symbol to callSite.typeArguments[it]
                }
            InlineFunctionBodyPreprocessor(typeArguments, parent, inlineFunctionResolver.callInlinerStrategy)
        }

        val substituteMap = mutableMapOf<IrValueParameter, IrExpression>()

        fun inline() = inlineFunction(callSite, callee, callee.originalFunction)

        private fun inlineFunction(
            callSite: IrFunctionAccessExpression,
            callee: IrFunction,
            originalInlinedElement: IrElement,
        ): IrBlock {
            val copiedCallee = inlineFunctionBodyPreprocessor.preprocess(callee).apply {
                parent = callee.parent
            }

            val (newStatementsFromCallSite, newStatementsFromDefault, copiedParameters) = evaluateArguments(callSite, copiedCallee)
            val statements = (copiedCallee.body as? IrBlockBody)?.statements
                ?: error("Body not found for function ${callee.render()}")

            val irReturnableBlockSymbol = IrReturnableBlockSymbolImpl()
            val endOffset = statements.lastOrNull()?.endOffset ?: callee.endOffset
            /* creates irBuilder appending to the end of the given returnable block: thus why we initialize
             * irBuilder with (..., endOffset, endOffset).
             */
            val irBuilder = context.createIrBuilder(irReturnableBlockSymbol, endOffset, endOffset)

            val returnType = callSite.type

            val transformer = ParameterSubstitutor()
            val newStatements = statements.map { it.transform(transformer, data = null) as IrStatement }

            val inlineFunctionToStore = if (inlineFunctionResolver.inlineMode == InlineMode.ALL_FUNCTIONS) callee else callee.originalFunction
            val inlinedBlock = IrInlinedFunctionBlockImpl(
                startOffset = callSite.startOffset,
                endOffset = callSite.endOffset,
                inlinedFunctionStartOffset = inlineFunctionToStore.startOffset,
                inlinedFunctionEndOffset = inlineFunctionToStore.endOffset,
                type = returnType,
                inlineFunctionSymbol = inlineFunctionToStore.symbol,
                fileEntry = inlineFunctionToStore.fileEntry,
                origin = null,
                statements = copiedParameters + newStatementsFromDefault + newStatements
            ).apply {
                // `inlineCall` and `inlinedElement` is required only for JVM backend only, but this inliner is common, so we need opt-in.
                @OptIn(JvmIrInlineExperimental::class)
                this.inlineCall = callSite
                @OptIn(JvmIrInlineExperimental::class)
                this.inlinedElement = originalInlinedElement

                // Insert a return statement for the function that is supposed to return Unit
                if (inlineFunctionToStore.returnType.isUnit()) {
                    val potentialReturn = this.statements.lastOrNull() as? IrReturn
                    if (potentialReturn == null) {
                        irBuilder.at(inlineFunctionToStore.endOffset, inlineFunctionToStore.endOffset).run {
                            this@apply.statements += irReturn(irGetObject(context.irBuiltIns.unitClass))
                        }
                    }
                }
            }

            val retBlock = IrReturnableBlockImpl(
                startOffset = callSite.startOffset,
                endOffset = callSite.endOffset,
                type = returnType,
                symbol = irReturnableBlockSymbol,
                origin = null,
                statements = listOf(inlinedBlock),
            ).apply {
                transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        expression.transformChildrenVoid(this)

                        if (expression.returnTargetSymbol == copiedCallee.symbol) {
                            val expr = if (returnType.isUnit()) {
                                expression.value.coerceToUnit(context.irBuiltIns)
                            } else {
                                expression.value.doImplicitCastIfNeededTo(returnType)
                            }
                            return irBuilder.at(expression).irReturn(expr)
                        }
                        return expression
                    }
                })
                patchDeclarationParents(parent) // TODO: Why it is not enough to just run SetDeclarationsParentVisitor?
            }

            if (newStatementsFromCallSite.isEmpty()) {
                return retBlock
            }

            return IrBlockImpl(
                startOffset = callSite.startOffset,
                endOffset = callSite.endOffset,
                type = callSite.type,
                statements = newStatementsFromCallSite + retBlock,
                origin = IrStatementOrigin.INLINE_ARGS_CONTAINER
            )
        }

        //---------------------------------------------------------------------//

        private inner class ParameterSubstitutor : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val newExpression = super.visitGetValue(expression) as IrGetValue
                val argument = substituteMap[newExpression.symbol.owner] ?: return newExpression

                argument.transformChildrenVoid(this) // Default argument can contain subjects for substitution.

                val ret =
                    if (argument is IrGetValue && argument in elementsWithLocationToPatch)
                        argument.copyWithOffsets(newExpression.startOffset, newExpression.endOffset)
                    else
                        argument.deepCopyWithSymbols()

                return ret.doImplicitCastIfNeededTo(newExpression.type)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                // TODO extract to common utils OR reuse ContractDSLRemoverLowering
                if (expression.symbol.owner.hasAnnotation(ContractsDslNames.CONTRACTS_DSL_ANNOTATION_FQN)) {
                    return IrCompositeImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType)
                }

                if (!isLambdaCall(expression))
                    return super.visitCall(expression)

                val dispatchReceiver = expression.dispatchReceiver?.unwrapAdditionalImplicitCastsIfNeeded() as IrGetValue
                val functionArgument = substituteMap[dispatchReceiver.symbol.owner] ?: return super.visitCall(expression)
                if ((dispatchReceiver.symbol.owner as? IrValueParameter)?.isNoinline == true) return super.visitCall(expression)

                fun asCallOf(function: IrSimpleFunction, boundArguments: List<IrExpression>): IrCall {
                    return IrCallImpl.fromSymbolOwner(expression.startOffset, expression.endOffset, function.symbol).also {
                        it.type = expression.type
                        val arguments = boundArguments.map { it.deepCopyWithSymbols() } +
                                expression.getAllArgumentsWithIr().map { it.second }.drop(1) // dropping dispatch receiver - it's lambda itself
                        for ((index, argument) in arguments.withIndex()) {
                            it.arguments[index] = argument
                        }
                    }
                }

                return when {
                    functionArgument is IrCallableReference<*> ->
                        error("Can't inline given reference, it should've been lowered\n${functionArgument.render()}")

                    functionArgument.isAdaptedFunctionReference() -> {
                        inlineAdaptedFunctionReference(expression, functionArgument as IrBlock)
                    }

                    functionArgument.isLambdaBlock() -> {
                        inlineAdaptedFunctionReference(expression, functionArgument as IrBlock).statements.last() as IrExpression
                    }

                    functionArgument is IrFunctionExpression ->
                        inlineFunctionExpression(asCallOf(functionArgument.function, emptyList()), functionArgument.function, functionArgument)

                    functionArgument is IrRichFunctionReference ->
                        inlineFunctionExpression(asCallOf(functionArgument.invokeFunction, functionArgument.boundValues), functionArgument.invokeFunction, functionArgument.attributeOwnerId)

                    functionArgument is IrRichPropertyReference ->
                        inlineFunctionExpression(asCallOf(functionArgument.getterFunction, functionArgument.boundValues), functionArgument.getterFunction, functionArgument.attributeOwnerId)

                    else ->
                        super.visitCall(expression)
                }
            }

            fun inlineFunctionExpression(irCall: IrCall, function: IrSimpleFunction, originalInlinedElement: IrElement): IrExpression {
                // Inline the lambda. Lambda parameters will be substituted with lambda arguments.
                val newExpression = inlineFunction(irCall, function, originalInlinedElement)
                // Substitute lambda arguments with target function arguments.
                return newExpression.transform(this, null)
            }

            fun inlineAdaptedFunctionReference(irCall: IrCall, irBlock: IrBlock): IrBlock {
                val irFunction = irBlock.statements[0].let {
                    it.transformChildrenVoid(this)
                    (it as IrFunction).deepCopyWithSymbols(it.parent)
                }
                val irFunctionReference = irBlock.statements[1] as IrFunctionReference
                val inlinedFunctionReference = inlineFunctionReference(irCall, irFunctionReference, irFunction)
                irFunction.origin = IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE
                return IrBlockImpl(
                    irCall.startOffset, irCall.endOffset,
                    inlinedFunctionReference.type, origin = null,
                    statements = listOf(irFunction, inlinedFunctionReference)
                )
            }

            fun inlineFunctionReference(
                irCall: IrCall,
                irFunctionReference: IrFunctionReference,
                inlinedFunction: IrFunction
            ): IrExpression {
                irFunctionReference.transformChildrenVoid(this)

                val function = irFunctionReference.symbol.owner
                val functionParameters = function.parameters
                val boundFunctionParameters = irFunctionReference.getArgumentsWithIr()
                val unboundFunctionParameters = functionParameters - boundFunctionParameters.map { it.first }
                val boundFunctionParametersMap = boundFunctionParameters.associate { it.first to it.second }

                var unboundIndex = 0
                val unboundArgsSet = unboundFunctionParameters.toSet()
                val valueParameters = irCall.getArgumentsWithIr().drop(1) // Skip dispatch receiver.

                val superType = irFunctionReference.type as IrSimpleType
                val superTypeArgumentsMap = irCall.symbol.owner.parentAsClass.typeParameters.associate { typeParam ->
                    typeParam.symbol to superType.arguments[typeParam.index].typeOrNull!!
                }

                require(superType.arguments.isNotEmpty()) { "type should have at least one type argument: ${superType.render()}" }
                // This expression equals to return type of function reference with substituted type arguments
                val functionReferenceReturnType = superType.arguments.last().typeOrFail

                val immediateCall = when (inlinedFunction) {
                    is IrConstructor -> {
                        val classTypeParametersCount = inlinedFunction.parentAsClass.typeParameters.size
                        IrConstructorCallImpl.fromSymbolOwner(
                            irCall.startOffset,
                            irCall.endOffset,
                            functionReferenceReturnType,
                            inlinedFunction.symbol,
                            classTypeParametersCount,
                            IrStatementOrigin.INLINED_FUNCTION_REFERENCE
                        )
                    }
                    is IrSimpleFunction ->
                        IrCallImpl(
                            irCall.startOffset,
                            irCall.endOffset,
                            functionReferenceReturnType,
                            inlinedFunction.symbol,
                            inlinedFunction.typeParameters.size,
                            IrStatementOrigin.INLINED_FUNCTION_REFERENCE
                        )
                }.apply {
                    for (parameter in functionParameters) {
                        val argument = when {
                            parameter !in unboundArgsSet -> {
                                val arg = boundFunctionParametersMap[parameter]!!
                                if (arg is IrGetValue && arg in elementsWithLocationToPatch) {
                                    arg.copyWithOffsets(irCall.startOffset, irCall.endOffset)
                                } else {
                                    arg.deepCopyWithSymbols()
                                }
                            }
                            unboundIndex == valueParameters.size && parameter.defaultValue != null -> {
                                continue
                            }
                            !parameter.isVararg -> {
                                assert(unboundIndex < valueParameters.size) {
                                    "Attempt to use unbound parameter outside of the callee's value parameters"
                                }
                                valueParameters[unboundIndex++].second
                            }
                            else -> {
                                val elements = mutableListOf<IrVarargElement>()
                                while (unboundIndex < valueParameters.size) {
                                    val (param, value) = valueParameters[unboundIndex++]
                                    val substitutedParamType = param.type.substitute(superTypeArgumentsMap)
                                    if (substitutedParamType == parameter.varargElementType!!)
                                        elements += value
                                    else
                                        elements += IrSpreadElementImpl(irCall.startOffset, irCall.endOffset, value)
                                }
                                IrVarargImpl(
                                    irCall.startOffset, irCall.endOffset,
                                    parameter.type,
                                    parameter.varargElementType!!,
                                    elements
                                )
                            }
                        }
                        val parameterToSet = inlinedFunction.parameters[parameter.indexInParameters]
                        arguments[parameterToSet] = argument.doImplicitCastIfNeededTo(parameterToSet.type)
                    }
                    assert(unboundIndex == valueParameters.size) { "Not all arguments of the callee are used" }
                    for (index in irFunctionReference.typeArguments.indices) {
                        typeArguments[index] = irFunctionReference.typeArguments[index]
                    }
                }

                return if (inlineFunctionResolver.needsInlining(inlinedFunction) || inlinedFunction.isStubForInline()) {
                    // `attributeOwnerId` is used to get the original reference instead of a reference on `stub_for_inlining`
                    inlineFunction(immediateCall, inlinedFunction, irFunctionReference.attributeOwnerId)
                } else {
                    super.visitExpression(immediateCall)
                }.doImplicitCastIfNeededTo(irCall.type)
            }

            override fun visitElement(element: IrElement) = element.accept(this, null)
        }

        private fun IrExpression.doImplicitCastIfNeededTo(type: IrType): IrExpression {
            if (!insertAdditionalImplicitCasts) return this
            return this.implicitCastIfNeededTo(type)
        }

        // With `insertAdditionalImplicitCasts` flag we sometimes insert
        // casts to inline lambda parameters before calling `invoke` on them.
        // Unwrapping these casts helps us satisfy inline lambda call detection logic.
        private fun IrExpression.unwrapAdditionalImplicitCastsIfNeeded(): IrExpression {
            if (insertAdditionalImplicitCasts && this is IrTypeOperatorCall && this.operator == IrTypeOperator.IMPLICIT_CAST) {
                return this.argument.unwrapAdditionalImplicitCastsIfNeeded()
            }
            return this
        }

        private fun isLambdaCall(irCall: IrCall): Boolean {
            val callee = irCall.symbol.owner
            val dispatchReceiver = callee.dispatchReceiverParameter ?: return false
            // Uncomment or delete depending on KT-57249 status
//            assert(!dispatchReceiver.type.isKFunction())

            return (dispatchReceiver.type.isFunctionOrKFunction() || dispatchReceiver.type.isSuspendFunctionOrKFunction())
                    && callee.name == OperatorNameConventions.INVOKE
                    && irCall.dispatchReceiver?.unwrapAdditionalImplicitCastsIfNeeded() is IrGetValue
        }

        private inner class ParameterToArgument(
            val parameter: IrValueParameter,
            originalArgumentExpression: IrExpression,
            val isDefaultArg: Boolean = false
        ) {
            val argumentExpression: IrExpression = originalArgumentExpression.unwrapAdditionalImplicitCastsIfNeeded()

            val isInlinableLambdaArgument: Boolean
                // must take "original" parameter because it can have generic type and so considered as no inline; see `lambdaAsGeneric.kt`
                get() = parameter.getOriginalParameter().isInlineParameter() &&
                        (argumentExpression is IrFunctionReference
                                || argumentExpression is IrFunctionExpression
                                || argumentExpression is IrRichFunctionReference
                                || argumentExpression.isAdaptedFunctionReference()
                                || argumentExpression.isInlineLambdaBlock()
                                || argumentExpression.isLambdaBlock())

            val isInlinablePropertyReference: Boolean
                // must take "original" parameter because it can have generic type and so considered as no inline; see `lambdaAsGeneric.kt`
                get() = parameter.getOriginalParameter().isInlineParameter() && (
                        argumentExpression is IrPropertyReference || argumentExpression is IrRichPropertyReference
                        )

            val isImmutableVariableLoad: Boolean
                get() = argumentExpression.let { argument ->
                    argument is IrGetValue && !argument.symbol.owner.let { it is IrVariable && it.isVar }
                }

            private fun IrValueParameter.getOriginalParameter(): IrValueParameter {
                if (this.parent !is IrFunction) return this
                val original = (this.parent as IrFunction).originalFunction
                return original.allParameters.singleOrNull { it.name == this.name && it.startOffset == this.startOffset } ?: this
            }
        }


        private fun ParameterToArgument.andAllOuterClasses(): List<ParameterToArgument> {
            val allParametersReplacements = mutableListOf(this)

            if (!produceOuterThisFields) return allParametersReplacements

            var currentThisSymbol = parameter.symbol
            var parameterClassDeclaration = parameter.type.classifierOrNull?.owner as? IrClass ?: return allParametersReplacements

            while (parameterClassDeclaration.isInner) {
                val outerClass = parameterClassDeclaration.parentAsClass
                val outerClassThis = outerClass.thisReceiver ?: error("${outerClass.name} has a null `thisReceiver` property")

                val parameterToArgument = ParameterToArgument(
                    parameter = outerClassThis,
                    originalArgumentExpression = IrGetFieldImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        (context as CommonBackendContext).innerClassesSupport.getOuterThisField(parameterClassDeclaration).symbol,
                        outerClassThis.type,
                        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, currentThisSymbol)
                    )
                )

                allParametersReplacements.add(parameterToArgument)

                currentThisSymbol = outerClassThis.symbol
                parameterClassDeclaration = outerClass
            }


            return allParametersReplacements
        }

        // callee might be a copied version of callsite.symbol.owner
        private fun buildParameterToArgument(callSite: IrFunctionAccessExpression, callee: IrFunction): List<ParameterToArgument> {

            val parameterToArgument = mutableListOf<ParameterToArgument>()

            if (callSite.dispatchReceiver != null && callee.dispatchReceiverParameter != null)
                parameterToArgument += ParameterToArgument(
                    parameter = callee.dispatchReceiverParameter!!,
                    originalArgumentExpression = callSite.dispatchReceiver!!
                ).andAllOuterClasses()

            val valueArguments =
                callSite.symbol.owner.valueParameters.map { callSite.getValueArgument(it.indexInOldValueParameters) }.toMutableList()

            if (callee.extensionReceiverParameter != null) {
                parameterToArgument += ParameterToArgument(
                    parameter = callee.extensionReceiverParameter!!,
                    originalArgumentExpression = if (callSite.extensionReceiver != null) {
                        callSite.extensionReceiver!!
                    } else {
                        // Special case: lambda with receiver is called as usual lambda:
                        valueArguments.removeAt(0)!!
                    }
                )
            } else if (callSite.extensionReceiver != null) {
                // Special case: usual lambda is called as lambda with receiver:
                valueArguments.add(0, callSite.extensionReceiver!!)
            }

            val parametersWithDefaultToArgument = mutableListOf<ParameterToArgument>()
            for (parameter in callee.valueParameters) {
                val argument = valueArguments[parameter.indexInOldValueParameters]
                when {
                    argument != null -> {
                        parameterToArgument += ParameterToArgument(
                            parameter = parameter,
                            originalArgumentExpression = argument
                        )
                    }

                    // After ExpectDeclarationsRemoving pass default values from expect declarations
                    // are represented correctly in IR.
                    parameter.defaultValue != null -> {  // There is no argument - try default value.
                        parametersWithDefaultToArgument += ParameterToArgument(
                            parameter = parameter,
                            originalArgumentExpression = parameter.defaultValue!!.expression,
                            isDefaultArg = true
                        )
                    }

                    parameter.varargElementType != null -> {
                        val emptyArray = IrVarargImpl(
                            startOffset = callSite.startOffset,
                            endOffset = callSite.endOffset,
                            type = parameter.type,
                            varargElementType = parameter.varargElementType!!
                        )
                        parameterToArgument += ParameterToArgument(
                            parameter = parameter,
                            originalArgumentExpression = emptyArray
                        )
                    }

                    else -> {
                        val message = "Incomplete expression: call to ${callee.render()} " +
                                "has no argument at index ${parameter.indexInParameters}"
                        throw Error(message)
                    }
                }
            }
            // All arguments except default are evaluated at callsite,
            // but default arguments are evaluated inside callee.
            return parameterToArgument + parametersWithDefaultToArgument
        }

        //-------------------------------------------------------------------------//

        private fun evaluateArguments(reference: IrCallableReference<*>): List<IrVariable> {
            val arguments = reference.getArgumentsWithIr().map { ParameterToArgument(it.first, it.second) }
            val evaluationStatements = mutableListOf<IrVariable>()
            val substitutor = ParameterSubstitutor()
            arguments.forEach {
                // Arguments may reference the previous ones - substitute them.
                val irExpression = it.argumentExpression.transform(substitutor, data = null)
                val newArgument = if (it.isImmutableVariableLoad) {
                    irGetValueWithoutLocation((irExpression as IrGetValue).symbol)
                } else {
                    val newVariable =
                        currentScope.scope.createTemporaryVariable(
                            startOffset = if (it.isDefaultArg) irExpression.startOffset else UNDEFINED_OFFSET,
                            endOffset = if (it.isDefaultArg) irExpression.startOffset else UNDEFINED_OFFSET,
                            irExpression = irExpression.doImplicitCastIfNeededTo(it.parameter.type),
                            // If original type of parameter is T, then `it.parameter.type` is T after substitution or erasure,
                            // depending on whether T reified or not.
                            irType = it.parameter.type,
                            nameHint = callee.symbol.owner.name.asStringStripSpecialMarkers() + "_" + it.parameter.name.asStringStripSpecialMarkers(),
                            isMutable = false,
                            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER,
                        )

                    evaluationStatements.add(newVariable)

                    irGetValueWithoutLocation(newVariable.symbol)
                }
                reference.arguments[it.parameter] = newArgument
            }
            return evaluationStatements
        }

        private fun evaluateCapturedValues(parameters: List<IrValueParameter>, expressions: MutableList<IrExpression>): List<IrVariable> {
            return buildList {
                for (i in expressions.indices) {
                    val irExpression = expressions[i].transform(ParameterSubstitutor(), data = null)

                    val newVariable =
                        if (irExpression is IrGetValue && irExpression.symbol.owner.isImmutable && irExpression.type == parameters[i].type) {
                            irExpression.symbol.owner
                        } else {
                            currentScope.scope.createTemporaryVariable(
                                startOffset = irExpression.startOffset,
                                endOffset = irExpression.endOffset,
                                irExpression = irExpression.doImplicitCastIfNeededTo(parameters[i].type),
                                nameHint = callee.symbol.owner.name.asStringStripSpecialMarkers() + "_${parameters[i].name.asStringStripSpecialMarkers()}",
                                isMutable = false,
                            ).also { add(it) }
                        }
                    expressions[i] = irGetValueWithoutLocation(newVariable.symbol)
                }
            }
        }

        private fun evaluateArguments(
            callSite: IrFunctionAccessExpression, callee: IrFunction
        ): Triple<List<IrVariable>, List<IrVariable>, List<IrVariable>> {
            // This list stores temp variables that represent non-default arguments of inline call.
            // The expressions that represent these arguments must be evaluated outside the inline block (at call-site).
            val evaluationStatements = mutableListOf<IrVariable>()
            // This list stores temp variables that represent default arguments of inline call, and they must be evaluated at the callee-site.
            val evaluationStatementsFromDefault = mutableListOf<IrVariable>()
            // This list stores variables that have the same names as parameters of inline call.
            // The initializer of such a variable is just a temporary variable from the first list.
            // This is required to keep debug information correct.
            val copiedParameters = mutableMapOf<IrValueParameter, IrVariable>()

            val arguments = buildParameterToArgument(callSite, callee)
            val substitutor = ParameterSubstitutor()
            arguments.forEach { argument ->
                val parameter = argument.parameter
                val container = if (argument.isDefaultArg) evaluationStatementsFromDefault else evaluationStatements
                /*
                 * We need to create temporary variable for each argument except inlinable lambda arguments.
                 * For simplicity and to produce simpler IR we don't create temporaries for every immutable variable,
                 * not only for those referring to inlinable lambdas.
                 */
                if ((argument.isInlinableLambdaArgument || argument.isInlinablePropertyReference)
                    && inlineFunctionResolver.inlineMode != InlineMode.ALL_FUNCTIONS
                ) {
                    substituteMap[parameter] = argument.argumentExpression
                    when (val arg = argument.argumentExpression) {
                        is IrCallableReference<*> -> error("Can't inline given reference, it should've been lowered\n${arg.render()}")
                        is IrRichFunctionReference -> {
                            container += evaluateCapturedValues(arg.invokeFunction.valueParameters, arg.boundValues)
                        }
                        is IrRichPropertyReference -> {
                            container += evaluateCapturedValues(arg.getterFunction.valueParameters, arg.boundValues)
                        }
                        is IrBlock -> if (arg.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || arg.origin == IrStatementOrigin.LAMBDA) {
                            container += evaluateArguments(arg.statements.last() as IrFunctionReference)
                        }
                    }

                    return@forEach
                }

                // Arguments may reference the previous ones - substitute them.
                val variableInitializer = argument.argumentExpression.transform(substitutor, data = null)

                val shouldCreateTemporaryVariable = !argument.doesNotNeedTemporaryVariable()
                if (shouldCreateTemporaryVariable) {
                    val (newVariable, copiedParameter) = createTemporaryVariable(
                        parameter, variableInitializer, argument.isDefaultArg, callee
                    )
                    container += newVariable
                    copiedParameter?.let { copiedParameters[parameter] = it }
                    substituteMap[parameter] = irGetValueWithoutLocation(newVariable.symbol)
                    return@forEach
                }

                substituteMap[parameter] = if (variableInitializer is IrGetValue) {
                    irGetValueWithoutLocation(variableInitializer.symbol)
                } else {
                    variableInitializer
                }
            }

            copiedParameters.forEach { (parameter, variable) ->
                substituteMap[parameter] = irGetValueWithoutLocation(variable.symbol)
            }

            return Triple(evaluationStatements, evaluationStatementsFromDefault, copiedParameters.values.toList())
        }

        private fun ParameterToArgument.doesNotNeedTemporaryVariable(): Boolean =
            argumentExpression.isPure(false, symbols = context.ir.symbols)
                    && (inlineFunctionResolver.inlineMode == InlineMode.ALL_FUNCTIONS || parameter.isInlineParameter())

        private fun createTemporaryVariable(
            parameter: IrValueParameter,
            variableInitializer: IrExpression,
            isDefaultArg: Boolean,
            callee: IrFunction
        ): Pair<IrVariable, IrVariable?> {
            val tmpVar = currentScope.scope.createTemporaryVariable(
                irExpression = IrBlockImpl(
                    if (isDefaultArg) variableInitializer.startOffset else UNDEFINED_OFFSET,
                    if (isDefaultArg) variableInitializer.endOffset else UNDEFINED_OFFSET,
                    // If original type of parameter is T, then `parameter.type` is T after substitution or erasure,
                    // depending on whether T reified or not.
                    type = parameter.type
                ).apply {
                    statements.add(variableInitializer.doImplicitCastIfNeededTo(parameter.type))
                },
                isMutable = false,
                origin = if (parameter.kind == IrParameterKind.ExtensionReceiver) {
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER
                } else {
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER
                },
            )

            if (isDefaultArg) {
                tmpVar.name = Name.identifier(parameter.name.asStringStripSpecialMarkers())
                return tmpVar to null
            }

            val variable = buildVariable(
                startOffset = tmpVar.startOffset,
                endOffset = tmpVar.endOffset,
                parent = tmpVar.parent,
                origin = tmpVar.origin,
                name = Name.identifier(parameter.name.asStringStripSpecialMarkers()),
                type = tmpVar.type,
            ).apply {
                // IR_TEMPORARY_VARIABLE origin makes this temporary variable invisible during debug
                tmpVar.origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                initializer = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, tmpVar.symbol)
            }

            return tmpVar to variable
        }

        private fun irGetValueWithoutLocation(
            symbol: IrValueSymbol,
            origin: IrStatementOrigin? = null,
        ): IrGetValue {
            return IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, origin).also {
                elementsWithLocationToPatch += it
            }
        }
    }
}

/**
 * Checks if the given function should be treated by 1st phase of inlining (inlining of private functions):
 * - Either the function is private.
 * - Or the function is declared inside a local class.
 */
fun IrFunction.isConsideredAsPrivateForInlining(): Boolean = isPrivate(visibility) || isLocal
