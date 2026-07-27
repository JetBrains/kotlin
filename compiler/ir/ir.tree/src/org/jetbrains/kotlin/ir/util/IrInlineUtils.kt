/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.IrType

/**
 * If this was a local declaration in an inline function, marks which file it was originally defined in.
 * In other words, to which file do [IrElement.startOffset] and [IrElement.endOffset] of elements in this
 * IR subtree point to.
 *
 * It is an analogy of [IrInlinedFunctionBlock.inlinedFunctionFileEntry], but for declarations instead of expressions.
 */
var IrDeclaration.sourceFileWhenInlined: IrFileEntry? by irAttribute(copyByDefault = true)

/**
 * Find the file entry where this declaration was originally defined.
 *
 * ### Limitations:
 * * Returns null for declarations from other modules, expect for inlined functions.
 * * Does not work for local declarations before they are extracted by [LocalDeclarationsLowering].
 */
tailrec fun IrDeclaration.getSourceFile(): IrFileEntry? {
    sourceFileWhenInlined?.let {
        return it
    }
    return when (val parent = parent) {
        is IrFile -> parent.fileEntry
        is IrExternalPackageFragment -> null
        else -> (parent as IrDeclaration).getSourceFile()
    }
}

var IrModuleFragment.preparedInlineFunctionCopies: List<IrSimpleFunction>? by irAttribute(copyByDefault = true)
var IrSimpleFunction.preparedInlineFunctionCopy: IrSimpleFunction? by irAttribute(copyByDefault = true)
var IrSimpleFunction.originalOfPreparedInlineFunctionCopy: IrSimpleFunction? by irAttribute(copyByDefault = true)

fun IrInlinedFunctionBlock.isFunctionInlining(): Boolean {
    return this.inlinedFunctionSymbol != null
}

val IrContainerExpression.innerInlinedBlockOrThis: IrContainerExpression
    get() = (this as? IrReturnableBlock)?.statements?.singleOrNull() as? IrInlinedFunctionBlock ?: this

fun IrType.isInlinableParameterType() = !isNullable() && (isFunction() || isSuspendFunction())

fun IrValueParameter.isInlineParameter() =
    kind == IrParameterKind.Regular
            && !isNoinline
            && type.isInlinableParameterType()
            && parent.isInlineFunction()

fun IrValueParameter.isInlineSuspendParameter() =
    kind == IrParameterKind.Regular
            && !isNoinline
            && !type.isNullable() && type.isSuspendFunction()
            && parent.isInlineFunction()

private fun IrDeclarationParent.isInlineFunction(): Boolean {
    if (this !is IrFunction) return false
    return this.isInline || this.isInlineArrayConstructor()
}

fun IrExpression.isLambdaBlock() =
    this is IrBlock && this.origin == IrStatementOrigin.LAMBDA

// Declarations in the scope of an externally visible inline function are implicitly part of the
// public ABI of a Kotlin module. This function returns the visibility of a containing inline function
// (determined *before* lowering), or null if the given declaration is not in the scope of an inline function.
//
// Currently, for JVM we mark all declarations in the scope of a public inline function as public, even if they are
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
    get() = (this.attributeOwnerId as? IrDeclaration) ?: this
