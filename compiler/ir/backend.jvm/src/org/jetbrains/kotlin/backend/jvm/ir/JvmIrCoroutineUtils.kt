/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.isMultifileBridge
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_IMPL_NAME_SUFFIX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass

fun IrFunction.continuationParameter(): IrValueParameter? = when {
    isInvokeSuspendOfLambda() || isInvokeSuspendForInlineOfLambda() -> dispatchReceiverParameter
    else -> valueParameters.singleOrNull { it.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS }
}

fun IrFunction.isInvokeSuspendOfLambda(): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

private fun IrFunction.isInvokeSuspendForInlineOfLambda(): Boolean =
    origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE
            && parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

fun IrFunction.isInvokeSuspendOfContinuation(): Boolean =
    name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS

private fun IrFunction.isInvokeOfSuspendCallableReference(): Boolean =
    isSuspend && name.asString().let { name -> name == "invoke" || name.startsWith("invoke-") }
            && parentAsClass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL

private fun IrFunction.isBridgeToSuspendImplMethod(): Boolean =
    isSuspend && this is IrSimpleFunction && (parent as? IrClass)?.functions?.any {
        it.name.asString() == name.asString() + SUSPEND_IMPL_NAME_SUFFIX && it.attributeOwnerId == attributeOwnerId
    } == true

private fun IrFunction.isStaticInlineClassReplacementDelegatingCall(): Boolean {
    if (this !is IrAttributeContainer || isStaticInlineClassReplacement) return false

    val parentClass = parent as? IrClass ?: return false
    if (!parentClass.isSingleFieldValueClass) return false

    return parentClass.declarations.find {
        it is IrAttributeContainer && it.attributeOwnerId == attributeOwnerId && it !== this
    }?.isStaticInlineClassReplacement == true
}

private val BRIDGE_ORIGINS = setOf(
    IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER,
    JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER,
    JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER,
    JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
    JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR,
    JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE,
    IrDeclarationOrigin.BRIDGE,
    IrDeclarationOrigin.BRIDGE_SPECIAL,
)

// These functions contain a single `suspend` tail call, the value of which should be returned as is
// (i.e. if it's an unboxed inline class value, it should remain unboxed).
fun IrFunction.isNonBoxingSuspendDelegation(): Boolean =
    origin in BRIDGE_ORIGINS ||
            isMultifileBridge() ||
            isBridgeToSuspendImplMethod() ||
            isStaticInlineClassReplacementForDefaultInterfaceMethod()

// Suspend static inline class replacements for fake overrides have to be for interface methods as inline classes cannot have a
// non-Object super type.
fun IrFunction.isStaticInlineClassReplacementForDefaultInterfaceMethod(): Boolean =
    isStaticInlineClassReplacement && this is IrSimpleFunction && (attributeOwnerId as IrSimpleFunction).isFakeOverride

fun IrFunction.shouldContainSuspendMarkers(): Boolean = !isNonBoxingSuspendDelegation() &&
        // These functions also contain a single `suspend` tail call, but if it returns an unboxed inline class value,
        // the return of it should be checked for a suspension and potentially boxed to satisfy an interface.
        origin != IrDeclarationOrigin.DELEGATED_MEMBER &&
        !isInvokeSuspendOfContinuation() &&
        !isInvokeOfSuspendCallableReference() &&
        !isStaticInlineClassReplacementDelegatingCall()

fun IrFunction.hasContinuation(): Boolean = isInvokeSuspendOfLambda() ||
        isSuspend && shouldContainSuspendMarkers() &&
        // These are templates for the inliner; the continuation is borrowed from the caller method.
        !isEffectivelyInlineOnly() &&
        origin != JvmLoweredDeclarationOrigin.INLINE_LAMBDA &&
        origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE &&
        origin != JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE

fun IrExpression?.isReadOfCrossinline(): Boolean = when (this) {
    is IrGetValue -> (symbol.owner as? IrValueParameter)?.isCrossinline == true
    is IrGetField -> symbol.owner.origin == LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE
    else -> false
}
