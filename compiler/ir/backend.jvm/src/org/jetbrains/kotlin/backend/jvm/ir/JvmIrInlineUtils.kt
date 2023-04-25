/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME

fun IrValueParameter.isInlineParameter(): Boolean =
    index >= 0 && !isNoinline && (type.isFunction() || type.isSuspendFunction()) &&
            // Parameters with default values are always nullable, so check the expression too.
            // Note that the frontend has a diagnostic for nullable inline parameters, so actually
            // making this return `false` requires using `@Suppress`.
            (!type.isNullable() || defaultValue?.expression?.type?.isNullable() == false)

// Declarations in the scope of an externally visible inline function are implicitly part of the
// public ABI of a Kotlin module. This function returns the visibility of a containing inline function
// (determined *before* lowering), or null if the given declaration is not in the scope of an inline function.
//
// Currently, we mark all declarations in the scope of a public inline function as public, even if they are
// contained in a nested private inline function. This is an over approximation, since private declarations
// inside of a public inline function can still escape if they are used without being regenerated.
// See `plugins/jvm-abi-gen/testData/compile/inlineNoRegeneration` for an example.
val IrDeclaration.inlineScopeVisibility: DescriptorVisibility?
    get() {
        var owner: IrDeclaration? = original
        var result: DescriptorVisibility? = null
        while (owner != null) {
            if (owner is IrFunction && owner.isInline) {
                result = if (!DescriptorVisibilities.isPrivate(owner.visibility)) {
                    if (owner.parentClassOrNull?.visibility?.let(DescriptorVisibilities::isPrivate) == true)
                        DescriptorVisibilities.PRIVATE
                    else
                        return owner.visibility
                } else {
                    owner.visibility
                }
            }
            owner = (owner.parent as? IrDeclaration)?.original
        }
        return result
    }

// True for declarations which are in the scope of an externally visible inline function.
val IrDeclaration.isInPublicInlineScope: Boolean
    get() = inlineScopeVisibility?.let(DescriptorVisibilities::isPrivate) == false

// Map declarations to original declarations before lowering.
private val IrDeclaration.original: IrDeclaration
    get() = (this as? IrAttributeContainer)?.attributeOwnerId as? IrDeclaration ?: this

fun IrStatement.unwrapInlineLambda(): IrFunctionReference? = when (this) {
    is IrBlock -> statements.lastOrNull()?.unwrapInlineLambda()
    is IrFunctionReference -> takeIf { it.origin == JvmLoweredStatementOrigin.INLINE_LAMBDA }
    else -> null
}

fun IrFunction.isInlineFunctionCall(context: JvmBackendContext): Boolean =
    (!context.state.isInlineDisabled || typeParameters.any { it.isReified }) && (isInline || isInlineArrayConstructor(context.irBuiltIns))

fun IrDeclaration.isInlineOnly(): Boolean =
    this is IrFunction && (
            (isInline && hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)) ||
                    (this is IrSimpleFunction && correspondingPropertySymbol?.owner?.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME) == true)
            )

fun IrDeclarationWithVisibility.isEffectivelyInlineOnly(): Boolean =
    this is IrFunction && (isReifiable() || isInlineOnly() || isPrivateInlineSuspend())

fun IrFunction.isPrivateInlineSuspend(): Boolean =
    isSuspend && isInline && visibility == DescriptorVisibilities.PRIVATE

fun IrFunction.isReifiable(): Boolean =
    typeParameters.any { it.isReified }

private fun IrAttributeContainer.getDeclarationBeforeInline(): IrDeclaration? {
    val original = this.originalBeforeInline ?: return null
    return original.extractRelatedDeclaration()
}

fun IrAttributeContainer.getAttributeOwnerBeforeInline(): IrAttributeContainer? {
    if (this.originalBeforeInline == null) return null
    return generateSequence(this) { it.originalBeforeInline }.last()
}

val IrDeclaration.fileParentBeforeInline: IrFile
    get() {
        val original = (this as? IrAttributeContainer)?.getDeclarationBeforeInline()
            ?: this.parentClassOrNull?.getDeclarationBeforeInline()
            ?: this
        return original.fileParent
    }