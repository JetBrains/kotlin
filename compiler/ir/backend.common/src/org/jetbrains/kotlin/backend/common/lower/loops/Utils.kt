/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun IrExpression.castIfNecessary(targetType: IrType, numberCastFunctionName: Name): IrExpression {
    return if (type == targetType) {
        this
    } else {
        val function = type.getClass()!!.functions.first { it.name == numberCastFunctionName }
        IrCallImpl(startOffset, endOffset, function.returnType, function.symbol)
            .apply { dispatchReceiver = this@castIfNecessary }
    }
}

/** Return the negated value if the expression is const, otherwise call unaryMinus(). */
internal fun IrExpression.negate(): IrExpression {
    val value = (this as? IrConst<*>)?.value as? Number
    return when (value) {
        is Int -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, -value)
        is Long -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, -value)
        else -> {
            val unaryMinusFun = type.getClass()!!.functions.first { it.name == OperatorNameConventions.UNARY_MINUS }
            IrCallImpl(startOffset, endOffset, type, unaryMinusFun.symbol, unaryMinusFun.descriptor).apply {
                dispatchReceiver = this@negate
            }
        }
    }
}

/** Return `this - 1` if the expression is const, otherwise call dec(). */
internal fun IrExpression.decrement(): IrExpression {
    val thisValue = (this as? IrConst<*>)?.value
    return when (thisValue) {
        is Int -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, thisValue - 1)
        is Long -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, thisValue - 1)
        is Char -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Char, thisValue - 1)
        else -> {
            val decFun = type.getClass()!!.functions.first { it.name == OperatorNameConventions.DEC }
            IrCallImpl(startOffset, endOffset, type, decFun.symbol, decFun.descriptor).apply {
                dispatchReceiver = this@decrement
            }
        }
    }
}

internal val IrExpression.canHaveSideEffects: Boolean
    get() = this !is IrConst<*> && this !is IrGetValue

internal val IrExpression.constLongValue: Long?
    get() = if (this is IrConst<*>) {
        val value = this.value
        when (value) {
            is Number -> value.toLong()
            is Char -> value.toLong()
            else -> null
        }
    } else null