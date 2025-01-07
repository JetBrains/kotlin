/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.isInlineLambdaBlock
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.contracts.parsing.ContractsDslNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.originalBeforeInline
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name.identifier
import org.jetbrains.kotlin.util.OperatorNameConventions

@PhaseDescription("FunctionInlining")
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
            expression, actualCallee, parent,
            context,
            inlineFunctionResolver,
            insertAdditionalImplicitCasts,
            produceOuterThisFields
        )
        return inliner.inline().markAsRegenerated()
    }

    private fun IrBlock.markAsRegenerated(): IrBlock {
        if (!regenerateInlinedAnonymousObjects) return this
        acceptVoid(object : IrVisitorVoid() {
            private fun IrElement.setUpCorrectAttributeOwner() {
                if (this.attributeOwnerId == this) return
                this.originalBeforeInline = this.attributeOwnerId
                this.attributeOwnerId = this
            }

            override fun visitElement(element: IrElement) {
                if (element !is IrInlinedFunctionBlock) element.setUpCorrectAttributeOwner()
                element.acceptChildrenVoid(this)
            }
        })
        return this
    }
}

private class CallInlining(
    val callSite: IrFunctionAccessExpression,
    val callee: IrFunction,
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


    fun inline() = inlineFunction(callSite, callee, callee.originalFunction)

    private fun inlineFunction(
        callSite: IrFunctionAccessExpression,
        callee: IrFunction,
        originalInlinedElement: IrElement,
    ): IrBlock {
        val copiedCallee = inlineFunctionBodyPreprocessor.preprocess(callee).apply {
            parent = callee.parent
        }

        val outerIrBuilder = context.createIrBuilder(copiedCallee.symbol, callSite.startOffset, callSite.endOffset)
        val inlineFunctionToStore = if (inlineFunctionResolver.inlineMode == InlineMode.ALL_FUNCTIONS) callee else callee.originalFunction

        val substituteMap = mutableMapOf<IrValueParameter, IrExpression>()
        val functionStatements = (copiedCallee.body as? IrBlockBody)?.statements
            ?: error("Body not found for function ${callee.render()}")

        val returnType = callSite.type

        val inlineResult = outerIrBuilder.irBlockOrSingleExpression(origin = IrStatementOrigin.INLINE_ARGS_CONTAINER) {
            +irReturnableBlock(returnType) {
                val inlinedFunctionBlock = irInlinedFunctionBlock(
                    inlinedFunctionStartOffset = inlineFunctionToStore.startOffset,
                    inlinedFunctionEndOffset = inlineFunctionToStore.endOffset,
                    resultType = returnType,
                    inlinedFunctionSymbol = inlineFunctionToStore.symbol.takeIf { originalInlinedElement is IrFunction },
                    inlinedFunctionFileEntry = inlineFunctionToStore.fileEntry,
                    origin = null,
                ) {
                    evaluateArguments(
                        callSiteBuilder = this@irBlockOrSingleExpression,
                        inlinedBlockBuilder = this@irInlinedFunctionBlock,
                        callSite, copiedCallee, substituteMap
                    )
                    +functionStatements
                    // Insert a return statement for the function that is supposed to return Unit
                    if (inlineFunctionToStore.returnType.isUnit()) {
                        val potentialReturn = functionStatements.lastOrNull() as? IrReturn
                        if (potentialReturn == null) {
                            at(inlineFunctionToStore.endOffset, inlineFunctionToStore.endOffset)
                            +irReturn(irGetObject(context.irBuiltIns.unitClass))
                        }
                    }
                }
                // `inlineCall` and `inlinedElement` is required only for JVM backend only, but this inliner is common, so we need opt-in.
                @OptIn(JvmIrInlineExperimental::class)
                inlinedFunctionBlock.inlineCall = callSite
                @OptIn(JvmIrInlineExperimental::class)
                inlinedFunctionBlock.inlinedElement = originalInlinedElement
                val transformer = InlinePostprocessor(
                    substituteMap, returnType, copiedCallee.symbol,
                    returnableBlockSymbol
                )
                inlinedFunctionBlock.transformChildrenVoid(transformer)
                +inlinedFunctionBlock
            }
            at(callSite) // block is using offsets at the end, let's restore them just in case
        } as IrBlock

        return inlineResult.patchDeclarationParents(parent)
    }

    //---------------------------------------------------------------------//

    /**
     * The inlining itself just copies function body into call-site.
     * To follow language semantics and IR consistency, the following transformations needed:
     * * Replace references to non-inlineable function parameters with corresponding local variables
     * * Replace returns from function with returns from corresponding returnable block
     * * Replace invoke calls on inlineable function parameters with recursive inlining
     */
    private inner class InlinePostprocessor(
        val substituteMap: Map<IrValueParameter, IrExpression>,
        val returnType: IrType,
        val inlinedFunctionSymbol: IrFunctionSymbol,
        val returnableBlockSymbol: IrReturnableBlockSymbol,
    ) : IrElementTransformerVoid() {
        override fun visitReturn(expression: IrReturn): IrExpression {
            expression.transformChildrenVoid(this)
            if (expression.returnTargetSymbol == inlinedFunctionSymbol) {
                expression.returnTargetSymbol = returnableBlockSymbol
                expression.value = expression.value.doImplicitCastIfNeededTo(returnType)
            }
            return expression
        }

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val newExpression = super.visitGetValue(expression) as IrGetValue
            val argument = substituteMap[newExpression.symbol.owner] ?: return newExpression

            argument.transformChildrenVoid(this) // Default argument can contain subjects for substitution.

            return argument.deepCopyWithSymbols().apply {
                if (argument is IrGetValue && argument in elementsWithLocationToPatch) {
                    startOffset = newExpression.startOffset
                    endOffset = newExpression.endOffset
                }
            }.doImplicitCastIfNeededTo(newExpression.type)
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
                                expression.arguments.drop(1) // dropping dispatch receiver - it's lambda itself
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
                            arg.deepCopyWithSymbols().apply {
                                if (arg is IrGetValue && arg in elementsWithLocationToPatch) {
                                    startOffset = irCall.startOffset
                                    endOffset = irCall.endOffset
                                }
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

            immediateCall.transformChildrenVoid(this)
            return if (inlineFunctionResolver.needsInlining(inlinedFunction.symbol) || inlinedFunction.isStubForInline()) {
                // `attributeOwnerId` is used to get the original reference instead of a reference on `stub_for_inlining`
                inlineFunction(immediateCall, inlinedFunction, irFunctionReference.attributeOwnerId)
            } else {
                immediateCall
            }.doImplicitCastIfNeededTo(irCall.type)
        }

        override fun visitElement(element: IrElement) = element.accept(this, null)
    }

    private fun IrExpression.doImplicitCastIfNeededTo(type: IrType): IrExpression {
        return when {
            !insertAdditionalImplicitCasts -> this
            type.isUnit() -> this.coerceToUnit(context.irBuiltIns)
            else -> this.implicitCastIfNeededTo(type)
        }
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


    private fun ParameterToArgument.allOuterClasses(): List<ParameterToArgument> {
        val allParametersReplacements = mutableListOf<ParameterToArgument>()

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
                ),
                isDefaultArg = true // corresponding temporary variable must be created inside inlined block, when variable for dispatch receiver is already created.
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

        val parametersWithDefaultToArgument = mutableListOf<ParameterToArgument>()
        for ((parameter, argument) in callee.parameters.zip(callSite.arguments)) {
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
        if (callSite.dispatchReceiver != null && callee.dispatchReceiverParameter != null)
            parameterToArgument += ParameterToArgument(
                parameter = callee.dispatchReceiverParameter!!,
                originalArgumentExpression = callSite.dispatchReceiver!!
            ).allOuterClasses()

        // All arguments except default are evaluated at callsite,
        // but default arguments are evaluated inside callee.
        return parameterToArgument + parametersWithDefaultToArgument
    }

    //-------------------------------------------------------------------------//

    private fun IrStatementsBuilder<*>.evaluateArguments(
        reference: IrCallableReference<*>
    ) {
        val arguments = reference.getArgumentsWithIr().map { ParameterToArgument(it.first, it.second) }
        for (argument in arguments) {
            val irExpression = argument.argumentExpression
            val variableSymbol = if (argument.isImmutableVariableLoad) {
                (irExpression as IrGetValue).symbol
            } else {
                if (argument.isDefaultArg) {
                    at(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                } else {
                    at(irExpression)
                }
                irTemporary(
                    irExpression.doImplicitCastIfNeededTo(argument.parameter.type),
                    nameHint = callee.symbol.owner.name.asStringStripSpecialMarkers() + "_" + argument.parameter.name.asStringStripSpecialMarkers(),
                    origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER,
                ).symbol
            }
            reference.arguments[argument.parameter] = irGetValueWithoutLocation(variableSymbol)
        }
    }

    private fun IrStatementsBuilder<*>.evaluateCapturedValues(
        parameters: List<IrValueParameter>,
        expressions: MutableList<IrExpression>
    ) {
        for (i in expressions.indices) {
            val irExpression = expressions[i]
            val variableSymbol =
                if (irExpression is IrGetValue && irExpression.symbol.owner.isImmutable && irExpression.type == parameters[i].type) {
                    irExpression.symbol.owner
                } else {
                    at(irExpression).irTemporary(
                        value = irExpression.doImplicitCastIfNeededTo(parameters[i].type),
                        nameHint = callee.symbol.owner.name.asStringStripSpecialMarkers() + "_${parameters[i].name.asStringStripSpecialMarkers()}",
                        isMutable = false,
                    )
                }
            expressions[i] = irGetValueWithoutLocation(variableSymbol.symbol)
        }
    }

    private fun IrExpression.tryGetLoadedInlineParameter() =
        (this as? IrGetValue)?.symbol?.takeIf { it is IrValueParameterSymbol && it.owner.isInlineParameter() }

    private fun evaluateArguments(
        callSiteBuilder: IrStatementsBuilder<*>,
        inlinedBlockBuilder: IrStatementsBuilder<*>,
        callSite: IrFunctionAccessExpression, callee: IrFunction,
        substituteMap: MutableMap<IrValueParameter, IrExpression>
    ) {

        val arguments = buildParameterToArgument(callSite, callee)
        for (argument in arguments) {
            val parameter = argument.parameter
            val variableInitializer = argument.argumentExpression
            /*
             * We need to create temporary variable for each argument except inlinable lambda arguments.
             * For simplicity and to produce simpler IR we don't create temporaries for every immutable variable,
             * not only for those referring to inlinable lambdas.
             */
            if ((argument.isInlinableLambdaArgument || argument.isInlinablePropertyReference)
                && inlineFunctionResolver.inlineMode != InlineMode.ALL_FUNCTIONS
            ) {
                val evaluationBuilder = if (argument.isDefaultArg) inlinedBlockBuilder else callSiteBuilder
                substituteMap[parameter] = variableInitializer
                when (variableInitializer) {
                    is IrCallableReference<*> -> error("Can't inline given reference, it should've been lowered\n${variableInitializer.render()}")
                    is IrRichFunctionReference -> {
                        evaluationBuilder.evaluateCapturedValues(
                            variableInitializer.invokeFunction.parameters,
                            variableInitializer.boundValues
                        )
                    }
                    is IrRichPropertyReference -> {
                        evaluationBuilder.evaluateCapturedValues(
                            variableInitializer.getterFunction.parameters,
                            variableInitializer.boundValues
                        )
                    }
                    is IrBlock -> if (variableInitializer.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || variableInitializer.origin == IrStatementOrigin.LAMBDA) {
                        evaluationBuilder.evaluateArguments(variableInitializer.statements.last() as IrFunctionReference)
                    }
                }
                continue
            }
            // inline parameters should never be stored to temporaries, as it would prevent their inlining
            variableInitializer.tryGetLoadedInlineParameter()?.let {
                substituteMap[parameter] = irGetValueWithoutLocation(it)
                continue
            }

            fun IrBuilderWithScope.computeInitializer() = irBlock(resultType = parameter.type) {
                +variableInitializer.doImplicitCastIfNeededTo(parameter.type)
            }

            val valueForTmpVar = if (argument.isDefaultArg) {
                inlinedBlockBuilder
                    .at(variableInitializer)
                    .computeInitializer()
            } else {
                // This variable is required to trigger argument evaluation outside the scope of the inline function
                val tempVarOutsideInlineBlock = callSiteBuilder
                    .at(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                    .irTemporary(callSiteBuilder.computeInitializer())
                inlinedBlockBuilder
                    .at(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                    .irGet(tempVarOutsideInlineBlock)
            }

            val tempVarInsideInlineBlock = inlinedBlockBuilder.irTemporary(
                value = valueForTmpVar,
                origin = if (parameter.kind == IrParameterKind.ExtensionReceiver) {
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER
                } else {
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER
                }
            ).apply {
                name = identifier(parameter.name.asStringStripSpecialMarkers())
            }

            substituteMap[parameter] = irGetValueWithoutLocation(tempVarInsideInlineBlock.symbol)
        }
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

