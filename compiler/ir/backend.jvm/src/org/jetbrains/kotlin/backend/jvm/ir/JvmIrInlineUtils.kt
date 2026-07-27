/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.common.ir.isReifiable
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.StandardClassIds

fun IrValueParameter.isInlineParameter(): Boolean =
    kind == IrParameterKind.Regular && !isNoinline && (type.isFunction() || type.isSuspendFunction()) &&
            // Parameters with default values are always nullable, so check the expression too.
            // Note that the frontend has a diagnostic for nullable inline parameters, so actually
            // making this return `false` requires using `@Suppress`.
            (!type.isNullable() || defaultValue?.expression?.type?.isNullable() == false)

fun IrStatement.unwrapRichInlineLambda(): IrRichFunctionReference? = when (this) {
    is IrBlock -> statements.lastOrNull()?.unwrapRichInlineLambda()
    is IrRichFunctionReference -> takeIf { it.origin == IrStatementOrigin.INLINE_LAMBDA }
    else -> null
}

fun IrFunction.isInlineFunctionCall(context: JvmBackendContext): Boolean =
    (!context.config.isInlineDisabled || typeParameters.any { it.isReified }) && (isInline || isInlineArrayConstructor())

fun IrDeclaration.isInlineOnly(): Boolean =
    this is IrFunction && (
            (isInline && hasAnnotation(StandardClassIds.Annotations.InlineOnly)) ||
                    (this is IrSimpleFunction && correspondingPropertySymbol?.owner?.hasAnnotation(StandardClassIds.Annotations.InlineOnly) == true)
            )

fun IrDeclarationWithVisibility.isEffectivelyInlineOnly(): Boolean =
    this is IrFunction && (isReifiable() || isInlineOnly() || isPrivateInlineSuspend())

fun IrFunction.isPrivateInlineSuspend(): Boolean =
    isSuspend && isInline && visibility == DescriptorVisibilities.PRIVATE
