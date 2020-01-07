/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.copyValueParametersFrom
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.coroutines.CoroutineTransformerMethodVisitor
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.coroutines.reportSuspensionPointInsideMonitor
import org.jetbrains.kotlin.codegen.inline.addFakeContinuationConstructorCallMarker
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFunWithDescriptorForInlining
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

internal fun generateStateMachineForNamedFunction(
    irFunction: IrFunction,
    classCodegen: ClassCodegen,
    methodVisitor: MethodVisitor,
    access: Int,
    signature: JvmMethodGenericSignature,
    obtainContinuationClassBuilder: () -> ClassBuilder,
    element: KtElement
): MethodVisitor {
    assert(irFunction.isSuspend)
    val state = classCodegen.state
    val languageVersionSettings = state.languageVersionSettings
    assert(languageVersionSettings.isReleaseCoroutines()) { "Experimental coroutines are unsupported in JVM_IR backend" }
    return CoroutineTransformerMethodVisitor(
        methodVisitor, access, signature.asmMethod.name, signature.asmMethod.descriptor, null, null,
        obtainClassBuilderForCoroutineState = obtainContinuationClassBuilder,
        reportSuspensionPointInsideMonitor = { reportSuspensionPointInsideMonitor(element, state, it) },
        lineNumber = CodegenUtil.getLineNumberForElement(element, false) ?: 0,
        sourceFile = classCodegen.irClass.file.name,
        languageVersionSettings = languageVersionSettings,
        shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
        containingClassInternalName = classCodegen.visitor.thisName,
        isForNamedFunction = true,
        needDispatchReceiver = irFunction.dispatchReceiverParameter != null
                || irFunction.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION,
        internalNameForDispatchReceiver = classCodegen.visitor.thisName,
        putContinuationParameterToLvt = false,
        disableTailCallOptimizationForFunctionReturningUnit = irFunction.returnType.isUnit() &&
                (irFunction as? IrSimpleFunction)?.overriddenSymbols?.let { symbols ->
                    symbols.isNotEmpty() && symbols.any { !it.owner.returnType.isUnit() }
                } == true
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
        methodVisitor, access, signature.asmMethod.name, signature.asmMethod.descriptor, signature.genericsSignature, null,
        obtainClassBuilderForCoroutineState = { classCodegen.visitor },
        reportSuspensionPointInsideMonitor = { reportSuspensionPointInsideMonitor(element, state, it) },
        lineNumber = CodegenUtil.getLineNumberForElement(element, false) ?: 0,
        sourceFile = classCodegen.irClass.file.name,
        languageVersionSettings = languageVersionSettings,
        shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
        containingClassInternalName = classCodegen.visitor.thisName,
        isForNamedFunction = false,
        disableTailCallOptimizationForFunctionReturningUnit = false
    )
}

internal fun IrFunction.isInvokeSuspendOfLambda(): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

internal fun IrFunction.isInvokeSuspendForInlineOfLambda(): Boolean =
    origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
            && parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

internal fun IrFunction.isInvokeSuspendOfContinuation(): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS

internal fun IrFunction.isInvokeOfSuspendCallableReference(): Boolean = isSuspend && name.asString() == "invoke" &&
        parentAsClass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL

internal fun IrFunction.isKnownToBeTailCall(): Boolean =
    when (origin) {
        IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER,
        JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
        JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE,
        JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_TO_SYNTHETIC,
        IrDeclarationOrigin.BRIDGE,
        IrDeclarationOrigin.BRIDGE_SPECIAL,
        IrDeclarationOrigin.DELEGATED_MEMBER -> true
        else -> isInvokeOfSuspendCallableReference()
    }

internal fun IrFunction.shouldNotContainSuspendMarkers(): Boolean =
    isInvokeSuspendOfContinuation() || isKnownToBeTailCall()

// Transform `suspend fun foo(params): RetType` into `fun foo(params, $completion: Continuation<RetType>): Any?`
// the result is called 'view', just to be consistent with old backend.
internal fun IrFunction.getOrCreateSuspendFunctionViewIfNeeded(context: JvmBackendContext): IrFunction {
    if (!isSuspend || origin == JvmLoweredDeclarationOrigin.SUSPEND_FUNCTION_VIEW ||
        origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE_VIEW
    ) return this
    return context.suspendFunctionOriginalToView[this] ?: suspendFunctionView(context)
}

private fun IrFunction.suspendFunctionView(context: JvmBackendContext): IrFunction {
    require(this.isSuspend && this is IrSimpleFunction)
    // For SuspendFunction{N}.invoke we need to generate INVOKEINTERFACE Function{N+1}.invoke(...Ljava/lang/Object;)...
    // instead of INVOKEINTERFACE Function{N+1}.invoke(...Lkotlin/coroutines/Continuation;)...
    val isInvokeOfNumberedSuspendFunction = (parent as? IrClass)?.defaultType?.isSuspendFunction() == true
    // And we need to generate this function for callable references
    val isBridgeInvokeOfCallableReference = origin == IrDeclarationOrigin.BRIDGE &&
            (parent as? IrClass)?.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
    val continuationType = if (isInvokeOfNumberedSuspendFunction || isBridgeInvokeOfCallableReference)
        context.irBuiltIns.anyNType
    else
        context.ir.symbols.continuationClass.createType(false, listOf(makeTypeProjection(returnType, Variance.INVARIANT)))
    return buildFunWithDescriptorForInlining(descriptor) {
        updateFrom(this@suspendFunctionView)
        name = this@suspendFunctionView.name
        origin = if (origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE)
            JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE_VIEW
        else
            JvmLoweredDeclarationOrigin.SUSPEND_FUNCTION_VIEW
        returnType = context.irBuiltIns.anyNType
    }.also {
        it.parent = parent

        it.annotations.addAll(annotations)

        it.copyAttributes(this)
        it.copyTypeParametersFrom(this)
        it.copyValueParametersFrom(this)
        // TODO: this should come before the default masks, if they exist; otherwise, this is not binary
        //       compatible with the non-IR backend
        it.addValueParameter(SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME, continuationType)

        // Add the suspend function view to the map before transforming the body to make sure
        // that recursive suspend functions do not lead to unbounded recursion at compile time.
        context.recordSuspendFunctionView(this, it)

        val valueParametersMapping = explicitParameters.zip(it.explicitParameters).toMap()
        it.body = body?.deepCopyWithSymbols(this)
        it.body?.transformChildrenVoid(object : VariableRemapper(valueParametersMapping) {
            // Do not cross class boundaries inside functions. Otherwise, callable references will try to access wrong $completion.
            override fun visitClass(declaration: IrClass): IrStatement = declaration

            override fun visitCall(expression: IrCall): IrExpression =
                super.visitCall(expression.createSuspendFunctionCallViewIfNeeded(context, it, callerIsInlineLambda = false))
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
    return IrCallImpl(startOffset, endOffset, view.returnType, view.symbol, superQualifierSymbol = superQualifierSymbol).also {
        it.copyTypeArgumentsFrom(this)
        it.dispatchReceiver = dispatchReceiver
        it.extensionReceiver = extensionReceiver
        for (i in 0 until valueArgumentsCount) {
            it.putValueArgument(i, getValueArgument(i))
        }
        val continuationParameter =
            when {
                caller.isInvokeSuspendOfLambda() || caller.isInvokeSuspendOfContinuation() ||
                        caller.isInvokeSuspendForInlineOfLambda() ->
                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, caller.dispatchReceiverParameter!!.symbol)
                callerIsInlineLambda -> context.fakeContinuation
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

internal fun generateFakeContinuationConstructorCall(
    v: InstructionAdapter,
    containingClassBuilder: ClassBuilder,
    classBuilder: ClassBuilder,
    irFunction: IrFunction
) {
    val continuationType = Type.getObjectType(classBuilder.thisName)
    // TODO: This is different in case of DefaultImpls
    val thisNameType = Type.getObjectType(containingClassBuilder.thisName.replace(".", "/"))
    val continuationIndex = listOfNotNull(irFunction.dispatchReceiverParameter, irFunction.extensionReceiverParameter).size +
            irFunction.valueParameters.size - 1
    with(v) {
        addFakeContinuationConstructorCallMarker(this, true)
        anew(continuationType)
        dup()
        if (irFunction.dispatchReceiverParameter != null) {
            load(0, AsmTypes.OBJECT_TYPE)
            load(continuationIndex, Type.getObjectType("kotlin/coroutines/Continuation"))
            invokespecial(continuationType.internalName, "<init>", "(${thisNameType}Lkotlin/coroutines/Continuation;)V", false)
        } else {
            load(continuationIndex, Type.getObjectType("kotlin/coroutines/Continuation"))
            invokespecial(continuationType.internalName, "<init>", "(Lkotlin/coroutines/Continuation;)V", false)
        }
        pop()
        addFakeContinuationConstructorCallMarker(this, false)
    }
}