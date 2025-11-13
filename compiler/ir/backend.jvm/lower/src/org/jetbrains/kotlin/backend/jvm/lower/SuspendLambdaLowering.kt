/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.lower.BOUND_RECEIVER_PARAMETER
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.ir.hasChild
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.backend.jvm.ir.isReadOfCrossinline
import org.jetbrains.kotlin.codegen.coroutines.COROUTINE_LABEL_FIELD_NAME
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.coroutines.normalize
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.org.objectweb.asm.Type

private fun IrFunction.capturesCrossinline(): Boolean {
    val parents = parents.toSet()
    return hasChild { it is IrGetValue && it.isReadOfCrossinline() && it.symbol.owner.parent in parents }
}

internal abstract class SuspendLoweringUtils(protected val context: JvmBackendContext) {
    protected fun IrClass.addFunctionOverride(
        function: IrSimpleFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrSimpleFunction {
        val overriddenType = superTypes.single { it.classifierOrFail == function.parentAsClass.symbol }
        val typeSubstitution = (overriddenType.classifierOrFail.owner as IrClass).typeParameters
            .map { it.symbol }
            .zip((overriddenType as IrSimpleType).arguments.map { (it as IrTypeProjection).type }) // No star projections in this lowering
            .toMap()
        return addFunction(
            function.name.asString(), function.returnType.substitute(typeSubstitution),
            startOffset = startOffset, endOffset = endOffset
        ).apply {
            overriddenSymbols = listOf(function.symbol)
            parameters += function.nonDispatchParameters.map { it.copyTo(this, type = it.type.substitute(typeSubstitution)) }
        }
    }

    protected fun IrClass.addFunctionOverride(
        function: IrSimpleFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        makeBody: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrSimpleFunction =
        addFunctionOverride(function, startOffset, endOffset).apply {
            body = context.createIrBuilder(symbol).irBlockBody { makeBody(this@apply) }
        }

    protected fun IrSimpleFunction.generateErrorForInlineBody() {
        val message = "This is a stub representing a copy of a suspend method without the state machine " +
                "(used by the inliner). Since the difference is at the bytecode level, the body is " +
                "still on the original function. Use originalOfSuspendForInline to retrieve it."
        body = context.irFactory.createExpressionBody(
            startOffset,
            endOffset,
            IrErrorExpressionImpl(startOffset, endOffset, returnType, message),
        )
    }

    protected fun IrFunction.addCompletionValueParameter(): IrValueParameter =
        addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType())

    protected fun IrFunction.continuationType(): IrType =
        context.symbols.continuationClass.typeWith(returnType).makeNullable()
}

/**
 * Transforms suspend lambdas into continuation classes.
 */
internal class SuspendLambdaLowering(context: JvmBackendContext) : SuspendLoweringUtils(context), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
                if (expression.invokeFunction.isSuspend && expression.origin.isLambda) {
                    expression.transformChildrenVoid(this)
                    val parent = currentDeclarationParent ?: error("No current declaration parent at ${expression.dump()}")
                    return generateAnonymousObjectForLambda(expression, parent)
                }
                return super.visitRichFunctionReference(expression)
            }
        })
    }

    private fun generateAnonymousObjectForLambda(reference: IrRichFunctionReference, parent: IrDeclarationParent): IrContainerExpression {
        val symbol = reference.invokeFunction.symbol
        return context.createIrBuilder(symbol).irBlock(reference.startOffset, reference.endOffset) {
            assert(reference.boundValues.isEmpty()) { "lambda with bound arguments: ${reference.render()}" }
            val continuation = generateContinuationClassForLambda(reference, symbol, parent)
            +continuation
            +irCall(continuation.constructors.single().symbol).apply {
                // Pass null as completion parameter
                arguments[0] = irNull()
            }
        }
    }

    private fun generateContinuationClassForLambda(
        reference: IrRichFunctionReference,
        symbol: IrFunctionSymbol,
        parent: IrDeclarationParent,
    ): IrClass =
        context.irFactory.buildClass {
            name = SpecialNames.NO_NAME_PROVIDED
            origin = JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            this.parent = parent
            createThisReceiverParameter()
            copyAttributes(reference)
            val function = symbol.owner
            val suspendLambda =
                if (reference.isRestrictedSuspension) context.symbols.restrictedSuspendLambdaClass.owner else context.symbols.suspendLambdaClass.owner
            val arity = (reference.type as IrSimpleType).arguments.size - 1
            val functionNClass = context.symbols.getJvmFunctionClass(arity + 1)
            val functionNType = functionNClass.typeWith(
                function.parameters.subList(0, arity).map { it.type }
                        + function.continuationType()
                        + context.irBuiltIns.anyNType
            )
            superTypes = listOf(suspendLambda.defaultType, functionNType)
            val usedParams = mutableSetOf<IrSymbolOwner>()

            // marking the parameters referenced in the function
            function.acceptChildrenVoid(
                object : IrVisitorVoid() {
                    override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

                    override fun visitGetValue(expression: IrGetValue) {
                        if (expression.symbol is IrValueParameterSymbol && expression.symbol.owner in function.parameters) {
                            usedParams += expression.symbol.owner
                        }
                    }
                },
            )

            addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType, JavaDescriptorVisibilities.PACKAGE_VISIBILITY)
            val varsCountByType = HashMap<Type, Int>()

            val parametersFields = function.parameters.map {
                val unboxedType = it.type.unboxInlineClass()
                val field = if (it in usedParams) addField {
                    val normalizedType = context.defaultTypeMapper.mapType(unboxedType).normalize()
                    val index = varsCountByType[normalizedType]?.plus(1) ?: 0
                    varsCountByType[normalizedType] = index
                    // Rename `$this` to avoid being caught by inlineCodegenUtils.isCapturedFieldName()
                    name = Name.identifier("${normalizedType.descriptor[0]}$$index")
                    type = if (normalizedType == AsmTypes.OBJECT_TYPE) context.irBuiltIns.anyNType else unboxedType
                    origin = LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE
                    isFinal = false
                    visibility =
                        if (it.origin == BOUND_RECEIVER_PARAMETER) DescriptorVisibilities.PRIVATE
                        else JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                } else null
                ParameterInfo(field, unboxedType, it.type, it.name, it.origin)
            }

            this.continuationClassVarsCountByType = varsCountByType
            val constructor = addPrimaryConstructorForLambda(suspendLambda, arity)
            val invokeToOverride = functionNClass.functions.single {
                it.owner.nonDispatchParameters.size == arity + 1 && it.owner.name.asString() == "invoke"
            }
            val createToOverride = suspendLambda.symbol.functions.singleOrNull {
                it.owner.nonDispatchParameters.size == arity + 1 && it.owner.name.asString() == "create"
            }
            val invokeSuspend = addInvokeSuspendForLambda(function, suspendLambda, parametersFields)
            if (function.capturesCrossinline()) {
                addInvokeSuspendForInlineLambda(invokeSuspend)
            }
            if (createToOverride != null) {
                addInvokeCallingCreate(addCreate(constructor, createToOverride, parametersFields), invokeSuspend, invokeToOverride)
            } else {
                addInvokeCallingConstructor(constructor, invokeSuspend, invokeToOverride, parametersFields)
            }

            this.metadata = function.metadata
        }

    private fun IrClass.addInvokeSuspendForLambda(
        irFunction: IrFunction,
        suspendLambda: IrClass,
        parameterInfos: List<ParameterInfo>
    ): IrSimpleFunction {
        val superMethod = suspendLambda.functions.single {
            it.name.asString() == INVOKE_SUSPEND_METHOD_NAME && it.hasShape(regularParameters = 1, dispatchReceiver = true) &&
                    it.parameters[1].type.isKotlinResult()
        }
        return addFunctionOverride(superMethod, irFunction.startOffset, irFunction.endOffset).apply override@{
            val localVals: List<IrVariable?> = parameterInfos.map { param ->
                if (param.isUsed) {
                    buildVariable(
                        parent = this,
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        origin = JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA_PARAMETER,
                        name = param.name,
                        type = param.fieldType
                    ).apply {
                        context.createIrBuilder(this@override.symbol).apply {
                            val receiver = irGet(dispatchReceiverParameter!!)
                            initializer = irBlock(resultType = type) {
                                +irGetField(receiver, param.field!!, param.fieldType)
                            }
                        }
                    }
                } else null
            }

            body = irFunction.moveBodyTo(this, mapOf())?.let { body ->
                body.transform(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        val parameter = (expression.symbol.owner as? IrValueParameter)?.takeIf { it.parent == irFunction }
                            ?: return expression
                        val lvar = localVals[parameter.indexInParameters] ?: return expression
                        val param = parameterInfos[parameter.indexInParameters]
                        // suspend lambda's parameters are all Any?, meaning, that inline classes are boxed
                        // however, lowered suspend lambda's fields contain unboxed inline classes.
                        // Thus, we need to update all types of suspend lambda argument's usages.
                        return IrGetValueImpl(expression.startOffset, expression.endOffset, lvar.symbol).coerceToBoxedIfNeeded(param)
                    }
                }, null)

                val generatedCodeMarkers =
                    if (this@SuspendLambdaLowering.context.config.enhancedCoroutinesDebugging) {
                        this@SuspendLambdaLowering.context.symbols.generatedCodeMarkersInCoroutinesClass.functions.map {
                            IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.owner.returnType, it)
                        }.toList()
                    } else emptyList()

                context.irFactory.createBlockBody(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, generatedCodeMarkers + localVals.filterNotNull() + body.statements
                )
            }

            copyAnnotationsFrom(irFunction)
        }
    }

    private fun IrClass.addInvokeSuspendForInlineLambda(invokeSuspend: IrSimpleFunction): IrSimpleFunction {
        return addFunction(
            INVOKE_SUSPEND_METHOD_NAME + FOR_INLINE_SUFFIX,
            context.irBuiltIns.anyNType,
            Modality.FINAL,
            origin = JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
        ).apply {
            copyAttributes(invokeSuspend)
            generateErrorForInlineBody()
            parameters += invokeSuspend.nonDispatchParameters.map { it.copyTo(this) }
            originalOfSuspendForInline = invokeSuspend
        }
    }

    // Invoke function in lambdas is responsible for
    //   1) calling `create`
    //   2) starting newly created coroutine by calling `invokeSuspend`.
    // Thus, it creates a clone of suspend lambda and starts it.
    // TODO: fix the generic signature -- type parameters of FunctionN should be substituted
    private fun IrClass.addInvokeCallingCreate(
        create: IrFunction,
        invokeSuspend: IrSimpleFunction,
        invokeToOverride: IrSimpleFunctionSymbol
    ) = addFunctionOverride(invokeToOverride.owner) { function ->
        val newlyCreatedObject = irCall(create).also { createCall ->
            createCall.arguments.assignFrom(function.parameters, ::irGet)
        }
        +irReturn(callInvokeSuspend(invokeSuspend, irImplicitCast(newlyCreatedObject, defaultType)))
    }

    // Same as above, but with `create` inlined. `create` is only defined in `SuspendLambda` in unary and binary
    // versions; for other lambdas, there's no point in generating a non-overriding `create` separately.
    private fun IrClass.addInvokeCallingConstructor(
        constructor: IrFunction,
        invokeSuspend: IrSimpleFunction,
        invokeToOverride: IrSimpleFunctionSymbol,
        fieldsForUnbound: List<ParameterInfo>
    ) = addFunctionOverride(invokeToOverride.owner) { function ->
        +irReturn(callInvokeSuspend(invokeSuspend, cloneLambda(function, constructor, fieldsForUnbound)))
    }

    private fun IrClass.addCreate(
        constructor: IrFunction,
        createToOverride: IrSimpleFunctionSymbol,
        fieldsForUnbound: List<ParameterInfo>
    ) = addFunctionOverride(createToOverride.owner) { function ->
        +irReturn(cloneLambda(function, constructor, fieldsForUnbound))
    }

    private fun IrBlockBodyBuilder.cloneLambda(
        scope: IrFunction,
        constructor: IrFunction,
        fieldsForUnbound: List<ParameterInfo>
    ): IrExpression {
        val nonDispatchParameters = scope.nonDispatchParameters
        val constructorCall = irCall(constructor).also {
            for (typeParameter in constructor.parentAsClass.typeParameters) {
                it.typeArguments[typeParameter.index] = typeParameter.defaultType
            }
            it.arguments[0] = irGet(nonDispatchParameters.last())
        }
        if (fieldsForUnbound.none { it.isUsed }) {
            return constructorCall
        }
        val result = irTemporary(constructorCall, "result")
        for ((index, field) in fieldsForUnbound.withIndex()) {
            if (field.isUsed) {
                +irSetField(irGet(result), field.field!!, irGet(nonDispatchParameters[index]).coerceToUnboxedIfNeeded(field))
            }
        }
        return irGet(result)
    }

    private fun IrExpression.coerceToUnboxedIfNeeded(field: ParameterInfo): IrExpression =
        if (!field.boxedType.isInlineClassType()) this
        else IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.fieldType, context.symbols.unsafeCoerceIntrinsic).apply {
            typeArguments[0] = field.boxedType
            typeArguments[1] = field.fieldType
            arguments[0] = this@coerceToUnboxedIfNeeded
        }

    private fun IrExpression.coerceToBoxedIfNeeded(field: ParameterInfo): IrExpression =
        if (!field.boxedType.isInlineClassType()) this
        else IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.boxedType, context.symbols.unsafeCoerceIntrinsic).apply {
            typeArguments[0] = field.fieldType
            typeArguments[1] = field.boxedType
            arguments[0] = this@coerceToBoxedIfNeeded
        }

    private fun IrBlockBodyBuilder.callInvokeSuspend(invokeSuspend: IrSimpleFunction, lambda: IrExpression): IrExpression {
        val argument = irCall(
            this@SuspendLambdaLowering.context.symbols.unsafeCoerceIntrinsic,
            this@SuspendLambdaLowering.context.symbols.resultOfAnyType
        ).apply {
            typeArguments[0] = context.irBuiltIns.anyNType
            typeArguments[1] = type
            arguments[0] = irUnit()
        }
        return irCallOp(invokeSuspend.symbol, invokeSuspend.returnType, lambda, argument)
    }

    private fun IrClass.addPrimaryConstructorForLambda(superClass: IrClass, arity: Int): IrConstructor =
        addConstructor {
            origin = JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA
            isPrimary = true
            returnType = defaultType
            visibility = DescriptorVisibilities.LOCAL
        }.also { constructor ->
            val completionParameterSymbol = constructor.addCompletionValueParameter()
            val superClassConstructor = superClass.constructors.single {
                it.hasShape(regularParameters = 2) && it.parameters[0].type.isInt() && it.parameters[1].type.isNullableContinuation()
            }
            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(superClassConstructor).also {
                    it.arguments[0] = irInt(arity + 1)
                    it.arguments[1] = irGet(completionParameterSymbol)
                }
                +IrInstanceInitializerCallImpl(startOffset, endOffset, symbol, context.irBuiltIns.unitType)
            }
        }
}

private data class ParameterInfo(
    val field: IrField?,
    val fieldType: IrType,
    val boxedType: IrType,
    val name: Name,
    val origin: IrDeclarationOrigin,
) {
    val isUsed = field != null
}
