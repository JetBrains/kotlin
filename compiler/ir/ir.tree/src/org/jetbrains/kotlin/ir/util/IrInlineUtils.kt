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
 * There is some compiler info from IR inliner that may not be available in non-JVM backends due to serialization in KLIBs.
 * For example, in the JVM backend it is safe to check the original call of an inlined function, and on other backends it's not.
 * To discourage usages of such APIs in non-JVM backends, this opt-in annotation was introduced.
 */
@RequiresOptIn(
    message = "This API is supposed to be used only inside JVM backend.",
)
annotation class JvmIrInlineExperimental

@JvmIrInlineExperimental
var IrInlinedFunctionBlock.inlineCall: IrFunctionAccessExpression? by irAttribute(copyByDefault = true)
@JvmIrInlineExperimental
var IrInlinedFunctionBlock.inlinedElement: IrElement? by irAttribute(copyByDefault = true)

/**
 * If this was a local declaration in an inline function, marks which file it was originally defined in.
 * In other words, to which file do [IrElement.startOffset] and [IrElement.endOffset] of elements in this
 * IR subtree point to.
 *
 * It is an analogy of [IrInlinedFunctionBlock.inlinedFunctionFileEntry], but for declarations instead of expressions.
 */
var IrDeclaration.sourceFileWhenInlined: IrFileEntry? by irAttribute(copyByDefault = true)

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
