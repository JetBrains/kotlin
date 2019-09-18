/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.descriptors.WrappedFunctionDescriptorWithContainerSource
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.coroutines.CoroutineTransformerMethodVisitor
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.MethodVisitor

internal fun generateStateMachineForNamedFunction(
    irFunction: IrFunction,
    classCodegen: ClassCodegen,
    methodVisitor: MethodVisitor,
    access: Int,
    signature: JvmMethodGenericSignature,
    continuationClassBuilder: ClassBuilder?,
    element: KtElement
): MethodVisitor {
    assert(irFunction.isSuspend)
    assert(continuationClassBuilder != null) {
        "Class builder for continuation is null"
    }
    val state = classCodegen.state
    val languageVersionSettings = state.languageVersionSettings
    assert(languageVersionSettings.isReleaseCoroutines()) { "Experimental coroutines are unsupported in JVM_IR backend" }
    return CoroutineTransformerMethodVisitor(
        methodVisitor, access, signature.asmMethod.name, signature.asmMethod.descriptor, null, null,
        obtainClassBuilderForCoroutineState = { continuationClassBuilder!! },
        element = element,
        diagnostics = state.diagnostics,
        languageVersionSettings = languageVersionSettings,
        shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
        containingClassInternalName = classCodegen.visitor.thisName,
        isForNamedFunction = true,
        needDispatchReceiver = irFunction.dispatchReceiverParameter != null,
        internalNameForDispatchReceiver = classCodegen.visitor.thisName,
        putContinuationParameterToLvt = false
    )
}

internal fun generateStateMachineForLambda(
    classCodegen: ClassCodegen,
    methodVisitor: MethodVisitor,
    access: Int,
    signature: JvmMethodGenericSignature,
    element: KtElement
): MethodVisitor {
    val state = classCodegen.state
    val languageVersionSettings = state.languageVersionSettings
    assert(languageVersionSettings.isReleaseCoroutines()) { "Experimental coroutines are unsupported in JVM_IR backend" }
    return CoroutineTransformerMethodVisitor(
        methodVisitor, access, signature.asmMethod.name, signature.asmMethod.descriptor, null, null,
        obtainClassBuilderForCoroutineState = { classCodegen.visitor },
        element = element,
        diagnostics = state.diagnostics,
        languageVersionSettings = languageVersionSettings,
        shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
        containingClassInternalName = classCodegen.visitor.thisName,
        isForNamedFunction = false
    )
}

internal fun IrFunction.isInvokeSuspendOfLambda(context: JvmBackendContext): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parent in context.suspendLambdaToOriginalFunctionMap

internal fun IrFunction.isInvokeOfSuspendLambda(context: JvmBackendContext): Boolean =
    name.asString() == "invoke" && parent in context.suspendLambdaToOriginalFunctionMap

internal fun IrFunction.isInvokeSuspendOfContinuation(context: JvmBackendContext): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass in context.suspendFunctionContinuations.values

// Transform `suspend fun foo(params): RetType` into `fun foo(params, $completion: Continuation<RetType>): Any?`
// the result is called 'view', just to be consistent with old backend.
internal fun IrFunction.getOrCreateSuspendFunctionViewIfNeeded(context: JvmBackendContext): IrFunction {
    if (!isSuspend || origin == SUSPEND_FUNCTION_VIEW) return this
    return if (isSuspend) context.suspendFunctionViews.getOrPut(this) { suspendFunctionView(context) } else this
}

private object SUSPEND_FUNCTION_VIEW : IrDeclarationOriginImpl("SUSPEND_FUNCTION_VIEW")

private fun IrFunction.suspendFunctionView(context: JvmBackendContext): IrFunction {
    require(this.isSuspend && this is IrSimpleFunction)
    val originalDescriptor = this.descriptor
    // For SuspendFunction{N}.invoke we need to generate INVOKEINTERFACE Function{N+1}.invoke(...Ljava/lang/Object;)...
    // instead of INVOKEINTERFACE Function{N+1}.invoke(...Lkotlin/coroutines/Continuation;)...
    val isInvokeOfNumberedSuspendFunction = (symbol.owner.parent as? IrClass)?.defaultType?.isSuspendFunction() == true
    val descriptor =
        if (originalDescriptor is DescriptorWithContainerSource && originalDescriptor.containerSource != null)
            WrappedFunctionDescriptorWithContainerSource(originalDescriptor.containerSource!!)
        else
            WrappedSimpleFunctionDescriptor(sourceElement = originalDescriptor.source)
    return IrFunctionImpl(
        startOffset, endOffset, SUSPEND_FUNCTION_VIEW, IrSimpleFunctionSymbolImpl(descriptor),
        name, visibility, modality, context.irBuiltIns.anyNType,
        isInline, isExternal, isTailrec, isSuspend
    ).also {
        descriptor.bind(it)
        it.parent = parent
        it.copyTypeParametersFrom(this)

        it.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(it)
        it.extensionReceiverParameter = extensionReceiverParameter?.copyTo(it)

        valueParameters.mapTo(it.valueParameters) { p -> p.copyTo(it) }
        it.addValueParameter(
            SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME,
            if (isInvokeOfNumberedSuspendFunction) context.irBuiltIns.anyNType
            else context.ir.symbols.continuationClass.createType(false, listOf(makeTypeProjection(returnType, Variance.INVARIANT)))
        )
        val valueParametersMapping = explicitParameters.zip(it.explicitParameters).toMap()
        it.body = body?.deepCopyWithSymbols(this)
        it.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrGetValue =
                valueParametersMapping[expression.symbol.owner]?.let { newParam ->
                    expression.run { IrGetValueImpl(startOffset, endOffset, type, newParam.symbol, origin) }
                } ?: expression

            override fun visitCall(expression: IrCall): IrExpression {
                if (!expression.isSuspend) return super.visitCall(expression)
                return super.visitCall(expression.createSuspendFunctionCallViewIfNeeded(context, it, callerIsInlineLambda = false))
            }
        })
    }
}

internal fun IrCall.createSuspendFunctionCallViewIfNeeded(
    context: JvmBackendContext,
    caller: IrFunction,
    callerIsInlineLambda: Boolean
): IrCall {
    if (!isSuspend) return this
    val view = (symbol.owner as IrSimpleFunction).getOrCreateSuspendFunctionViewIfNeeded(context)
    if (view == symbol.owner) return this
    return IrCallImpl(startOffset, endOffset, view.returnType, view.symbol).also {
        it.copyTypeArgumentsFrom(this)
        it.dispatchReceiver = dispatchReceiver
        it.extensionReceiver = extensionReceiver
        for (i in 0 until valueArgumentsCount) {
            it.putValueArgument(i, getValueArgument(i))
        }
        val continuationParameter =
            when {
                caller.isInvokeSuspendOfLambda(context) || caller.isInvokeSuspendOfContinuation(context) ->
                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, caller.dispatchReceiverParameter!!.symbol)
                callerIsInlineLambda -> context.FAKE_CONTINUATION
                else -> IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, caller.valueParameters.last().symbol)
            }
        it.putValueArgument(valueArgumentsCount, continuationParameter)
    }
}

internal fun createFakeContinuation(context: JvmBackendContext): IrExpression = IrErrorExpressionImpl(
    UNDEFINED_OFFSET,
    UNDEFINED_OFFSET,
    context.ir.symbols.continuationClass.createType(true, listOf(makeTypeProjection(context.irBuiltIns.anyNType, Variance.INVARIANT))),
    "FAKE_CONTINUATION"
)
