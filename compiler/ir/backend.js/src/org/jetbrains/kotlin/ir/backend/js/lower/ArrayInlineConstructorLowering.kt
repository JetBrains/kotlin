/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.irCall

// Replace array inline constructors with stdlib function invocations
class ArrayConstructorTransformer(
    val context: JsIrBackendContext
) {
    // Inline constructor for CharArray is implemented in runtime
    private val primitiveArrayInlineToSizeConstructorMap =
        context.intrinsics.primitiveArrays.filter { it.value != PrimitiveType.CHAR }.keys.associate {
            it.inlineConstructor to it.sizeConstructor
        }

    fun transformCall(expression: IrCall): IrCall {
        if (expression.symbol == context.intrinsics.array.inlineConstructor) {
            return irCall(expression, context.intrinsics.jsArray)
        } else {
            primitiveArrayInlineToSizeConstructorMap[expression.symbol]?.let { sizeConstructor ->
                return IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    context.intrinsics.jsFillArray
                ).apply {
                    putValueArgument(0, IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        sizeConstructor
                    ).apply {
                        putValueArgument(0, expression.getValueArgument(0))
                    })
                    putValueArgument(1, expression.getValueArgument(1))
                }
            }
        }

        return expression
    }
}

// TODO it.isInline doesn't work =(
private val IrClassSymbol.inlineConstructor
    get() = owner.declarations.filterIsInstance<IrConstructor>().first { it.valueParameters.size == 2 }.symbol

private val IrClassSymbol.sizeConstructor
    get() = owner.declarations.filterIsInstance<IrConstructor>().first { it.valueParameters.size == 1 }.symbol
