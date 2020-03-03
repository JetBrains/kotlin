/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.allOverridden
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.coroutines.CoroutineTransformerMethodVisitor
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_IMPL_NAME_SUFFIX
import org.jetbrains.kotlin.codegen.coroutines.reportSuspensionPointInsideMonitor
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.MethodVisitor

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
        disableTailCallOptimizationForFunctionReturningUnit = irFunction.suspendFunctionOriginal().let {
            it.returnType.isUnit() && it.anyOfOverriddenFunctionsReturnsNonUnit()
        }
    )
}

internal fun IrFunction.anyOfOverriddenFunctionsReturnsNonUnit(): Boolean {
    return (this as? IrSimpleFunction)?.allOverridden()?.toList()?.let { functions ->
        functions.isNotEmpty() && functions.any { !it.returnType.isUnit() }
    } == true
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

internal fun IrFunction.continuationParameter(): IrValueParameter? = when {
    isInvokeSuspendOfLambda() || isInvokeSuspendForInlineOfLambda() -> dispatchReceiverParameter
    else -> valueParameters.singleOrNull { it.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS }
}

internal fun IrFunction.isInvokeSuspendOfLambda(): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

private fun IrFunction.isInvokeSuspendForInlineOfLambda(): Boolean =
    origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
            && parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

internal fun IrFunction.isInvokeSuspendOfContinuation(): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS

private fun IrFunction.isInvokeOfSuspendCallableReference(): Boolean =
    isSuspend && name.asString() == "invoke" && parentAsClass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL

private fun IrFunction.isBridgeToSuspendImplMethod(): Boolean =
    isSuspend && this is IrSimpleFunction && parentAsClass.functions.any {
        it.name.asString() == name.asString() + SUSPEND_IMPL_NAME_SUFFIX && it.attributeOwnerId == attributeOwnerId
    }

internal fun IrFunction.shouldContainSuspendMarkers(): Boolean = !isInvokeSuspendOfContinuation() &&
        // These are tail-call bridges and do not require any bytecode modifications.
        origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER &&
        origin != JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER &&
        origin != JvmLoweredDeclarationOrigin.MULTIFILE_BRIDGE &&
        origin != JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR &&
        origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE &&
        origin != JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE_TO_SYNTHETIC &&
        origin != JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE &&
        origin != IrDeclarationOrigin.BRIDGE &&
        origin != IrDeclarationOrigin.BRIDGE_SPECIAL &&
        origin != IrDeclarationOrigin.DELEGATED_MEMBER &&
        !isInvokeOfSuspendCallableReference() &&
        !isBridgeToSuspendImplMethod()

internal fun IrFunction.hasContinuation(): Boolean = isSuspend && shouldContainSuspendMarkers() &&
        // These are templates for the inliner; the continuation will be generated after it runs.
        origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA &&
        origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE &&
        origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE

internal fun IrExpression?.isReadOfCrossinline(): Boolean = when (this) {
    is IrGetValue -> (symbol.owner as? IrValueParameter)?.isCrossinline == true
    is IrGetField -> symbol.owner.origin == LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE
    else -> false
}

internal fun IrExpression?.isReadOfInlineLambda(): Boolean = isReadOfCrossinline() ||
        (this is IrGetValue && origin == IrStatementOrigin.VARIABLE_AS_FUNCTION && (symbol.owner as? IrValueParameter)?.isNoinline == false)

internal fun createFakeContinuation(context: JvmBackendContext): IrExpression = IrErrorExpressionImpl(
    UNDEFINED_OFFSET,
    UNDEFINED_OFFSET,
    context.ir.symbols.continuationClass.createType(true, listOf(makeTypeProjection(context.irBuiltIns.anyNType, Variance.INVARIANT))),
    "FAKE_CONTINUATION"
)
