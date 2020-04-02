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
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
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
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal fun MethodNode.acceptWithStateMachine(
    irFunction: IrFunction,
    classCodegen: ClassCodegen,
    methodVisitor: MethodVisitor,
    obtainContinuationClassBuilder: () -> ClassBuilder
) {
    val state = classCodegen.context.state
    val languageVersionSettings = state.languageVersionSettings
    assert(languageVersionSettings.isReleaseCoroutines()) { "Experimental coroutines are unsupported in JVM_IR backend" }
    val element = if (irFunction.isSuspend)
        irFunction.symbol.descriptor.psiElement ?: classCodegen.irClass.descriptor.psiElement
    else
        classCodegen.context.suspendLambdaToOriginalFunctionMap[classCodegen.irClass.attributeOwnerId]!!.symbol.descriptor.psiElement
    val visitor = CoroutineTransformerMethodVisitor(
        methodVisitor, access, name, desc, signature, exceptions.toTypedArray(),
        obtainClassBuilderForCoroutineState = obtainContinuationClassBuilder,
        reportSuspensionPointInsideMonitor = { reportSuspensionPointInsideMonitor(element as KtElement, state, it) },
        lineNumber = element?.let { CodegenUtil.getLineNumberForElement(it, false) } ?: 0,
        sourceFile = classCodegen.irClass.file.name,
        languageVersionSettings = languageVersionSettings,
        shouldPreserveClassInitialization = state.constructorCallNormalizationMode.shouldPreserveClassInitialization,
        containingClassInternalName = classCodegen.visitor.thisName,
        isForNamedFunction = irFunction.isSuspend, // SuspendLambda.invokeSuspend is not suspend
        needDispatchReceiver = irFunction.isSuspend && (irFunction.dispatchReceiverParameter != null
                || irFunction.origin == JvmLoweredDeclarationOrigin.SUSPEND_IMPL_STATIC_FUNCTION),
        internalNameForDispatchReceiver = classCodegen.visitor.thisName,
        putContinuationParameterToLvt = false,
        disableTailCallOptimizationForFunctionReturningUnit = irFunction.isSuspend && irFunction.suspendFunctionOriginal().let {
            it.returnType.isUnit() && it.anyOfOverriddenFunctionsReturnsNonUnit()
        }
    )
    accept(visitor)
}

private fun IrFunction.anyOfOverriddenFunctionsReturnsNonUnit(): Boolean {
    return (this as? IrSimpleFunction)?.allOverridden()?.toList()?.let { functions ->
        functions.isNotEmpty() && functions.any { !it.returnType.isUnit() }
    } == true
}

internal fun IrFunction.suspendForInlineToOriginal(): IrSimpleFunction? {
    if (origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE &&
        origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
    ) return null
    return parentAsClass.declarations.find {
        it is IrSimpleFunction && it.attributeOwnerId == (this as IrSimpleFunction).attributeOwnerId &&
                it.name.asString() + FOR_INLINE_SUFFIX == name.asString()
    } as IrSimpleFunction?
}

internal fun IrFunction.alwaysNeedsContinuation(): Boolean =
    this is IrSimpleFunction && hasContinuation() && parentAsClass.declarations.any {
        it is IrSimpleFunction && it.attributeOwnerId == attributeOwnerId &&
                it.origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
    }

internal fun IrFunction.continuationClass(): IrClass? =
    (body as? IrBlockBody)?.statements?.find { it is IrClass && it.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS }
            as IrClass?

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
        origin != IrDeclarationOrigin.BRIDGE &&
        origin != IrDeclarationOrigin.BRIDGE_SPECIAL &&
        origin != IrDeclarationOrigin.DELEGATED_MEMBER &&
        !isInvokeOfSuspendCallableReference() &&
        !isBridgeToSuspendImplMethod()

internal fun IrFunction.hasContinuation(): Boolean = isSuspend && shouldContainSuspendMarkers() &&
        // This is inline-only function
        !isEffectivelyInlineOnly() &&
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

fun MethodNode.preprocessSuspendMarkers(method: IrFunction? = null) {
    if (instructions.first == null) return
    if (method?.origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE) {
        // Remove the fake continuation constructor, since this method either has a real state machine or is inline.
        // Include one instruction before the start marker (that's the id) and one after the end marker (that's a pop).
        val sequence = instructions.asSequence()
        val start = sequence.firstOrNull { it.next != null && isBeforeFakeContinuationConstructorCallMarker(it.next) }
        if (start != null) {
            val end = sequence.first { it.previous != null && isAfterFakeContinuationConstructorCallMarker(it.previous) }
            InsnSequence(start, end.next).forEach(instructions::remove)
        }
    }
    // Method is null if this node is going to be inlined into another node rather than written into a class file.
    val keepMarkers = method != null &&
            method.origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE &&
            method.origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
    for (insn in instructions.asSequence().filter { isBeforeInlineSuspendMarker(it) || isAfterInlineSuspendMarker(it) }) {
        if (keepMarkers) {
            val newId = if (isBeforeInlineSuspendMarker(insn)) INLINE_MARKER_BEFORE_SUSPEND_ID else INLINE_MARKER_AFTER_SUSPEND_ID
            instructions.set(insn.previous, InsnNode(Opcodes.ICONST_0 + newId))
        } else {
            instructions.remove(insn.previous)
            instructions.remove(insn)
        }
    }
}
