/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

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

var IrModuleFragment.erasedTopLevelInlineFunctions: List<IrSimpleFunction>? by irAttribute(copyByDefault = true)
var IrSimpleFunction.erasedTopLevelCopy: IrSimpleFunction? by irAttribute(copyByDefault = true)
var IrFunction.originalOfErasedTopLevelCopy: IrFunction? by irAttribute(copyByDefault = true)

fun IrInlinedFunctionBlock.isFunctionInlining(): Boolean {
    return this.inlinedFunctionSymbol != null
}

fun IrInlinedFunctionBlock.isLambdaInlining(): Boolean {
    return !isFunctionInlining()
}

val IrContainerExpression.innerInlinedBlockOrThis: IrContainerExpression
    get() = (this as? IrReturnableBlock)?.statements?.singleOrNull() as? IrInlinedFunctionBlock ?: this

fun IrType.isInlinableParameterType() = !isNullable() && (isFunction() || isSuspendFunction())

fun IrValueParameter.isInlineParameter() =
    kind == IrParameterKind.Regular
            && !isNoinline
            && type.isInlinableParameterType()
            && parent.isInlineFunction()

private fun IrDeclarationParent.isInlineFunction(): Boolean {
    if (this !is IrFunction) return false
    return this.isInline || this.isInlineArrayConstructor()
}

fun IrExpression.isAdaptedFunctionReference() =
    this is IrBlock && this.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE

fun IrExpression.isLambdaBlock() =
    this is IrBlock && this.origin == IrStatementOrigin.LAMBDA
