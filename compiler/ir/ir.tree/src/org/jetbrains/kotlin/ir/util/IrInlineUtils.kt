/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
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
var IrInlinedFunctionBlock.inlineCall: IrFunctionAccessExpression? by irAttribute()
@JvmIrInlineExperimental
var IrInlinedFunctionBlock.inlinedElement: IrElement? by irAttribute()

var IrSimpleFunction.erasedTopLevelCopy: IrSimpleFunction? by irAttribute()

@OptIn(JvmIrInlineExperimental::class)
fun IrInlinedFunctionBlock.isFunctionInlining(): Boolean {
    return this.inlinedElement is IrFunction
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
