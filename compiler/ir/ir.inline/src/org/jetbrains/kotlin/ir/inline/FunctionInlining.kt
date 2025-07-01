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
import org.jetbrains.kotlin.builtins.StandardNames
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
import org.jetbrains.kotlin.backend.common.originalBeforeInline
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name.identifier
import org.jetbrains.kotlin.util.OperatorNameConventions

@PhaseDescription("FunctionInlining")
class FunctionInlining @JvmIrInlineExperimental constructor(
    val context: LoweringContext,
    private val inlineFunctionResolver: InlineFunctionResolver,
    private val insertAdditionalImplicitCasts: Boolean,
    private val regenerateInlinedAnonymousObjects: Boolean,
    private val produceOuterThisFields: Boolean,
) : IrTransformer<IrDeclaration>(), BodyLoweringPass {

    @OptIn(JvmIrInlineExperimental::class)
    constructor(
        context: LoweringContext,
        inlineFunctionResolver: InlineFunctionResolver,
    ) : this(
        context,
        inlineFunctionResolver,
        insertAdditionalImplicitCasts = true,
        regenerateInlinedAnonymousObjects = false,
        produceOuterThisFields = false
    )

    init {
        require(!produceOuterThisFields || context is CommonBackendContext) {
            "The inliner can generate outer fields only with param `context` of type `CommonBackendContext`"
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.accept(this, container)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration): IrStatement {
        return when (declaration) {
            is IrFunction, is IrClass, is IrProperty -> context.irFactory.stageController.restrictTo(declaration) {
                super.visitDeclaration(declaration, declaration)
            }
            else -> super.visitDeclaration(declaration, declaration)
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration): IrExpression {
        expression.transformChildren(this, data)

        val actualCallee = inlineFunctionResolver.getFunctionDeclarationToInline(expression) ?: return expression
        if (expression is IrCall && Symbols.isTypeOfIntrinsic(actualCallee.symbol)) {
            return expression
        }
        if (actualCallee.body == null) {
            return expression
        }
        actualCallee.body?.transformChildren(this, actualCallee)
        actualCallee.parameters.forEachIndexed { index, param ->
            if (expression.arguments[index] == null) {
                // Default values can recursively reference [callee] - transform only needed.
                param.defaultValue = param.defaultValue?.transform(this@FunctionInlining, actualCallee)
            }
        }

        return CallInlining(
            context,
            actualCallee,
            data.file,
            parent = data as? IrDeclarationParent ?: data.parent,
            inlineFunctionResolver,
            insertAdditionalImplicitCasts,
            produceOuterThisFields
        ).inline(
            callSite = expression,
        ).markAsRegenerated()
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
    private val context: LoweringContext,
    private val callee: IrFunction,
    private val currentFile: IrFile,
    private val parent: IrDeclarationParent,
    private val inlineFunctionResolver: InlineFunctionResolver,
    private val insertAdditionalImplicitCasts: Boolean,
    private val produceOuterThisFields: Boolean
) {
    private val parents = (parent as? IrDeclaration)?.parentsWithSelf?.toSet() ?: setOf(parent)
    private val elementsWithLocationToPatch = hashSetOf<IrGetValue>()

    fun inline(callSite: IrFunctionAccessExpression) =
        inlineFunction(callSite, callee, callee).patchDeclarationParents(parent)

    private fun inlineFunction(
        callSite: IrFunctionAccessExpression,
        callee: IrFunction,
        originalInlinedElement: IrElement,
    ): IrBlock {
        val copiedCallee = run {
            val allTypeParameters = extractTypeParameters(callee)
            val notAccessibleTypeParameters = allTypeParameters.filter { it.parent !in parents }

            val typeArgumentsMap = buildMap {
                // Erase type parameters that are not accessible after inlining.
                // This includes non-reified parameters of the inline function itself and some type parameters of the outer classes.
                notAccessibleTypeParameters.filter { !it.isReified }.forEach { put(it.symbol, null) }

                // Substitute reified type parameters with concrete type arguments
                for ((index, typeArgument) in callSite.typeArguments.withIndex()) {
                    if (!callee.typeParameters[index].isReified || typeArgument == null) continue
                    put(callee.typeParameters[index].symbol, typeArgument)
                }

                // Leave every other parameter as is, they are visible in the inlined scope.
            }
            InlineFunctionBodyPreprocessor(typeArgumentsMap)
                .preprocess(callee)
        }

        val outerIrBuilder = context.createIrBuilder(copiedCallee.symbol, callSite.startOffset, callSite.endOffset)

        val substituteMap = mutableMapOf<IrValueParameter, IrExpression>()
        val functionStatements = (copiedCallee.body as? IrBlockBody)?.statements
            ?: error("Body not found for function ${callee.render()}")

        val returnType = callSite.type

        return outerIrBuilder.irBlockOrSingleExpression(origin = IrStatementOrigin.INLINE_ARGS_CONTAINER) {
            +irReturnableBlock(returnType) {
                val inlinedFunctionBlock = irInlinedFunctionBlock(
                    inlinedFunctionStartOffset = callee.startOffset,
                    inlinedFunctionEndOffset = callee.endOffset,
                    resultType = returnType,
                    inlinedFunctionSymbol = (callee.originalOfErasedTopLevelCopy ?: callee).symbol.takeIf { originalInlinedElement is IrFunction },
                    inlinedFunctionFileEntry = callee.fileEntry,
                    origin = null,
                ) {
                    evaluateArguments(
                        callSiteBuilder = this@irBlockOrSingleExpression,
                        inlinedBlockBuilder = this@irInlinedFunctionBlock,
                        callSite, copiedCallee, substituteMap
                    )
                    +functionStatements
                    // Insert a return statement for the function that is supposed to return Unit
                    if (callee.returnType.isUnit()) {
                        val potentialReturn = functionStatements.lastOrNull() as? IrReturn
                        if (potentialReturn == null) {
                            at(callee.endOffset, callee.endOffset)
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
        override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock): IrInlinedFunctionBlock {
            inlinedBlock.transformChildrenVoid(this)
            if (currentFile.fileEntry == inlinedBlock.inlinedFunctionFileEntry) return inlinedBlock

            val symbol = inlinedBlock.inlinedFunctionSymbol
            if (symbol != null && symbol.isConsideredAsPrivateAndNotLocalForInlining()) {
                inlinedBlock.inlinedFunctionSymbol = null
            }
            return inlinedBlock
        }

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
            if (expression.isContractCall()) {
                return IrCompositeImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType)
            }

            if (!isLambdaCall(expression))
                return super.visitCall(expression)

            // Here `isLambdaCall` guarantees that `expression` is call of `Function.invoke`.
            // So `expression.arguments.first()` is exactly dispatch receiver and not some other parameter.
            val dispatchReceiver = expression.arguments.first()?.unwrapAdditionalImplicitCastsIfNeeded() as IrGetValue
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
                    inlineAdaptedFunctionReference(expression, functionArgument as IrBlock)
                }

                functionArgument is IrFunctionExpression ->
                    inlineFunctionExpression(asCallOf(functionArgument.function, emptyList()), functionArgument.function, functionArgument)

                functionArgument is IrRichCallableReference<*> ->
                    inlineFunctionExpression(asCallOf(functionArgument.invokeFunction, functionArgument.boundValues), functionArgument.invokeFunction, functionArgument)

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

        fun inlineAdaptedFunctionReference(irCall: IrCall, irBlock: IrBlock): IrExpression {
            val irFunction = irBlock.statements[0] as IrSimpleFunction
            val irFunctionReference = irBlock.statements[1] as IrFunctionReference
            return inlineFunctionReference(irCall, irFunctionReference, irFunction)
        }

        fun inlineFunctionReference(
            irCall: IrCall,
            irFunctionReference: IrFunctionReference,
            inlinedFunction: IrSimpleFunction
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

            val immediateCall = IrCallImpl(
                irCall.startOffset,
                irCall.endOffset,
                functionReferenceReturnType,
                inlinedFunction.symbol,
                inlinedFunction.typeParameters.size,
                IrStatementOrigin.INLINED_FUNCTION_REFERENCE
            ).apply {
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
            return inlineFunctionExpression(
                immediateCall, inlinedFunction, irFunctionReference.attributeOwnerId
            ).doImplicitCastIfNeededTo(irCall.type)
        }

        override fun visitElement(element: IrElement) = element.accept(this, null)
    }

    // Contracts can appear only in K1 mode. In K2, they are dropped on the FIR2IR phase.
    private fun IrCall.isContractCall(): Boolean {
        return symbol.isBound && symbol.owner.annotations.any {
            it.symbol.isBound && it.symbol.owner.parentAsClass.hasEqualFqName(ContractsDslNames.CONTRACTS_DSL_ANNOTATION_FQN)
        }
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
        val symbol = irCall.symbol
        if (symbol.isBound) {
            val callee = symbol.owner
            val dispatchReceiver = callee.dispatchReceiverParameter ?: return false
            // Uncomment or delete depending on KT-57249 status
            // assert(!dispatchReceiver.type.isKFunction())

            return (dispatchReceiver.type.isFunctionOrKFunction() || dispatchReceiver.type.isSuspendFunctionOrKFunction())
                    && callee.name == OperatorNameConventions.INVOKE
                    && irCall.dispatchReceiver?.unwrapAdditionalImplicitCastsIfNeeded() is IrGetValue
        }

        fun hasDispatchGetValueReceiver(): Boolean {
            val firstArgument = irCall.arguments.first()
            val unwrapped = firstArgument?.unwrapAdditionalImplicitCastsIfNeeded() as? IrGetValue ?: return false
            return unwrapped.symbol.owner is IrValueParameter
        }

        fun IdSignature.isFunctionOrKFunction(): Boolean {
            return with(this as? IdSignature.CommonSignature ?: return false) {
                packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME.asString() && shortName.startsWith("Function") ||
                        packageFqName == StandardNames.KOTLIN_REFLECT_FQ_NAME.asString() && shortName.startsWith("KFunction")
            }
        }

        fun IdSignature.isSuspendFunctionOrKFunction(): Boolean {
            return with(this as? IdSignature.CommonSignature ?: return false) {
                packageFqName == StandardNames.COROUTINES_PACKAGE_FQ_NAME.asString() && shortName.startsWith("SuspendFunction") ||
                        packageFqName == StandardNames.KOTLIN_REFLECT_FQ_NAME.asString() && shortName.startsWith("KSuspendFunction")
            }
        }

        val signature = symbol.signature?.asPublic() ?: return false
        return signature.shortName == OperatorNameConventions.INVOKE.asString() &&
                with(signature.topLevelSignature()) { isFunctionOrKFunction() || isSuspendFunctionOrKFunction() } &&
                hasDispatchGetValueReceiver()
    }

    private inner class ParameterToArgument(
        val parameter: IrValueParameter,
        originalArgumentExpression: IrExpression,
        val isDefaultArg: Boolean = false
    ) {
        val argumentExpression: IrExpression = originalArgumentExpression.unwrapAdditionalImplicitCastsIfNeeded()

        val isInlinable: Boolean
            // must take "original" parameter because it can have generic type and so considered as no inline; see `lambdaAsGeneric.kt`
            get() = parameter.isInlineParameter() &&
                    (argumentExpression is IrFunctionReference
                            || argumentExpression is IrFunctionExpression
                            || argumentExpression is IrRichFunctionReference
                            || argumentExpression is IrPropertyReference
                            || argumentExpression is IrRichPropertyReference
                            || argumentExpression.isAdaptedFunctionReference()
                            || argumentExpression.isInlineLambdaBlock()
                            || argumentExpression.isLambdaBlock()
                            )

        val isImmutableVariableLoad: Boolean
            get() = argumentExpression.let { argument ->
                argument is IrGetValue && !argument.symbol.owner.let { it is IrVariable && it.isVar }
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
        if (callSite.dispatchReceiver != null)
            parameterToArgument += ParameterToArgument(
                parameter = callee.parameters[0],
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
        callSite: IrFunctionAccessExpression,
        callee: IrFunction,
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
            if (argument.isInlinable) {
                val evaluationBuilder = if (argument.isDefaultArg) inlinedBlockBuilder else callSiteBuilder
                substituteMap[parameter] = variableInitializer
                when (variableInitializer) {
                    is IrCallableReference<*> -> error("Can't inline given reference, it should've been lowered\n${variableInitializer.render()}")
                    is IrRichCallableReference<*> -> {
                        evaluationBuilder.evaluateCapturedValues(
                            variableInitializer.invokeFunction.parameters,
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

