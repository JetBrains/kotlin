/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.hasChild
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
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type
import kotlin.collections.set

internal val suspendLambdaPhase = makeIrFilePhase(
    ::SuspendLambdaLowering,
    "SuspendLambda",
    "Transform suspend lambdas into continuation classes"
)

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
            valueParameters = function.valueParameters.map { it.copyTo(this, type = it.type.substitute(typeSubstitution)) }
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
                "still on the original function. Use suspendForInlineToOriginal() to retrieve it."
        body = IrExpressionBodyImpl(startOffset, endOffset, IrErrorExpressionImpl(startOffset, endOffset, returnType, message))
    }

    protected fun IrFunction.addCompletionValueParameter(): IrValueParameter =
        addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType())

    protected fun IrFunction.continuationType(): IrType =
        context.ir.symbols.continuationClass.typeWith(returnType).makeNullable()
}

private class SuspendLambdaLowering(context: JvmBackendContext) : SuspendLoweringUtils(context), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitBlock(expression: IrBlock): IrExpression {
                val reference = expression.statements.lastOrNull() as? IrFunctionReference ?: return super.visitBlock(expression)
                if (reference.isSuspend && reference.origin.isLambda) {
                    assert(expression.statements.size == 2 && expression.statements[0] is IrFunction)
                    expression.transformChildrenVoid(this)
                    val parent = currentDeclarationParent ?: error("No current declaration parent at ${reference.dump()}")
                    return generateAnonymousObjectForLambda(reference, parent)
                }
                return super.visitBlock(expression)
            }
        })
    }

    private fun generateAnonymousObjectForLambda(reference: IrFunctionReference, parent: IrDeclarationParent) =
        context.createIrBuilder(reference.symbol).irBlock(reference.startOffset, reference.endOffset) {
            assert(reference.getArgumentsWithIr().isEmpty()) { "lambda with bound arguments: ${reference.render()}" }
            val continuation = generateContinuationClassForLambda(reference, parent)
            +continuation
            +irCall(continuation.constructors.single().symbol).apply {
                // Pass null as completion parameter
                putValueArgument(0, irNull())
            }
        }

    private fun generateContinuationClassForLambda(reference: IrFunctionReference, parent: IrDeclarationParent): IrClass =
        context.irFactory.buildClass {
            name = SpecialNames.NO_NAME_PROVIDED
            origin = JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            this.parent = parent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            copyAttributes(reference)

            val function = reference.symbol.owner
            val extensionReceiver = function.extensionReceiverParameter?.type?.classOrNull
            val isRestricted = extensionReceiver != null && extensionReceiver.owner.annotations.any {
                it.type.classOrNull?.isClassWithFqName(FqNameUnsafe("kotlin.coroutines.RestrictsSuspension")) == true
            }
            val suspendLambda =
                if (isRestricted) context.ir.symbols.restrictedSuspendLambdaClass.owner
                else context.ir.symbols.suspendLambdaClass.owner
            val arity = (reference.type as IrSimpleType).arguments.size - 1
            val functionNClass = context.ir.symbols.getJvmFunctionClass(arity + 1)
            val functionNType = functionNClass.typeWith(
                function.explicitParameters.subList(0, arity).map { it.type }
                        + function.continuationType()
                        + context.irBuiltIns.anyNType
            )
            superTypes = listOf(suspendLambda.defaultType, functionNType)
            val usedParams = mutableSetOf<IrSymbolOwner>()

            // marking the parameters referenced in the function
            function.acceptChildrenVoid(
                object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

                    override fun visitGetValue(expression: IrGetValue) {
                        if (expression.symbol is IrValueParameterSymbol && expression.symbol.owner in function.explicitParameters) {
                            usedParams += expression.symbol.owner
                        }
                    }
                },
            )

            addField(COROUTINE_LABEL_FIELD_NAME, context.irBuiltIns.intType, JavaDescriptorVisibilities.PACKAGE_VISIBILITY)
            val varsCountByType = HashMap<Type, Int>()

            val parametersFields = function.explicitParameters.map {
                val field = if (it in usedParams) addField {
                    val normalizedType = context.typeMapper.mapType(it.type).normalize()
                    val index = varsCountByType[normalizedType]?.plus(1) ?: 0
                    varsCountByType[normalizedType] = index
                    // Rename `$this` to avoid being caught by inlineCodegenUtils.isCapturedFieldName()
                    name = Name.identifier("${normalizedType.descriptor[0]}$$index")
                    type = if (normalizedType == AsmTypes.OBJECT_TYPE) context.irBuiltIns.anyNType else it.type
                    origin = LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE
                    isFinal = false
                    visibility = if (it.index < 0) DescriptorVisibilities.PRIVATE else JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                } else null
                ParameterInfo(field, it.type, it.name, it.origin)
            }

            context.continuationClassesVarsCountByType[attributeOwnerId] = varsCountByType
            val constructor = addPrimaryConstructorForLambda(suspendLambda, arity)
            val invokeToOverride = functionNClass.functions.single {
                it.owner.valueParameters.size == arity + 1 && it.owner.name.asString() == "invoke"
            }
            val createToOverride = suspendLambda.symbol.functions.singleOrNull {
                it.owner.valueParameters.size == arity + 1 && it.owner.name.asString() == "create"
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
            context.suspendLambdaToOriginalFunctionMap[attributeOwnerId as IrFunctionReference] = function
        }

    private fun IrClass.addInvokeSuspendForLambda(
        irFunction: IrFunction,
        suspendLambda: IrClass,
        parameterInfos: List<ParameterInfo>
    ): IrSimpleFunction {
        val superMethod = suspendLambda.functions.single {
            it.name.asString() == INVOKE_SUSPEND_METHOD_NAME && it.valueParameters.size == 1 &&
                    it.valueParameters[0].type.isKotlinResult()
        }
        return addFunctionOverride(superMethod, irFunction.startOffset, irFunction.endOffset).apply {
            val localVals: List<IrVariable?> = parameterInfos.map { param ->
                if (param.isUsed) {
                    buildVariable(
                        parent = this,
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        origin = param.origin,
                        name = param.name,
                        type = param.type
                    ).apply {
                        val receiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchReceiverParameter!!.symbol)
                        val initializerBlock = IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type)
                        initializerBlock.statements += IrGetFieldImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, param.field!!.symbol, type, receiver
                        )
                        initializer = initializerBlock
                    }
                } else null
            }

            body = irFunction.moveBodyTo(this, mapOf())?.let { body ->
                body.transform(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        val parameter = (expression.symbol.owner as? IrValueParameter)?.takeIf { it.parent == irFunction }
                            ?: return expression
                        val lvar = localVals[parameter.index + if (irFunction.extensionReceiverParameter != null) 1 else 0]
                            ?: return expression
                        return IrGetValueImpl(expression.startOffset, expression.endOffset, lvar.symbol)
                    }
                }, null)
                context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, localVals.filterNotNull() + body.statements)
            }
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
            valueParameters = invokeSuspend.valueParameters.map { it.copyTo(this) }
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
            createCall.dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
            for ((index, param) in function.valueParameters.withIndex()) {
                createCall.putValueArgument(index, irGet(param))
            }
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
        val constructorCall = irCall(constructor).also {
            for (typeParameter in constructor.parentAsClass.typeParameters) {
                it.putTypeArgument(typeParameter.index, typeParameter.defaultType)
            }
            it.putValueArgument(0, irGet(scope.valueParameters.last()))
        }
        if (fieldsForUnbound.none { it.isUsed }) {
            return constructorCall
        }
        val result = irTemporary(constructorCall, "result")
        for ((index, field) in fieldsForUnbound.withIndex()) {
            if (field.isUsed) {
                +irSetField(irGet(result), field.field!!, irGet(scope.valueParameters[index]))
            }
        }
        return irGet(result)
    }

    private fun IrBlockBodyBuilder.callInvokeSuspend(invokeSuspend: IrSimpleFunction, lambda: IrExpression): IrExpression =
        irCallOp(invokeSuspend.symbol, invokeSuspend.returnType, lambda, irCall(
            this@SuspendLambdaLowering.context.ir.symbols.unsafeCoerceIntrinsic,
            this@SuspendLambdaLowering.context.ir.symbols.resultOfAnyType
        ).apply {
            putTypeArgument(0, context.irBuiltIns.anyNType)
            putTypeArgument(1, type)
            putValueArgument(0, irUnit())
        })

    private fun IrClass.addPrimaryConstructorForLambda(superClass: IrClass, arity: Int): IrConstructor =
        addConstructor {
            origin = JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA
            isPrimary = true
            returnType = defaultType
            visibility = DescriptorVisibilities.LOCAL
        }.also { constructor ->
            val completionParameterSymbol = constructor.addCompletionValueParameter()
            val superClassConstructor = superClass.constructors.single {
                it.valueParameters.size == 2 && it.valueParameters[0].type.isInt() && it.valueParameters[1].type.isNullableContinuation()
            }
            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(superClassConstructor).also {
                    it.putValueArgument(0, irInt(arity + 1))
                    it.putValueArgument(1, irGet(completionParameterSymbol))
                }
                +IrInstanceInitializerCallImpl(startOffset, endOffset, symbol, context.irBuiltIns.unitType)
            }
        }
}

private data class ParameterInfo(val field: IrField?, val type: IrType, val name: Name, val origin: IrDeclarationOrigin) {
    val isUsed = field != null
}
