/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.lower.allOverridden
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.coroutines.CoroutineTransformerMethodVisitor
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.reportSuspensionPointInsideMonitor
import org.jetbrains.kotlin.codegen.inline.addFakeContinuationConstructorCallMarker
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
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
        addFakeContinuationConstructorCallMarker(this, false)
        pop()
    }
}