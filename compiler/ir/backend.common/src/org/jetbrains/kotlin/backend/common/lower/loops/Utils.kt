/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

/** Return the negated value if the expression is const, otherwise call unaryMinus(). */
internal fun IrExpression.negate(): IrExpression {
    return when (val value = (this as? IrConst<*>)?.value as? Number) {
        is Int -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, -value)
        is Long -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, -value)
        else -> {
            // This expression's type could be Nothing from an exception throw, in which case the unary minus function will not exist.
            if (type.isNothing()) return this

            val unaryMinusFun = type.getClass()!!.functions.single {
                it.name == OperatorNameConventions.UNARY_MINUS &&
                        it.valueParameters.isEmpty()
            }
            IrCallImpl(
                startOffset, endOffset, type,
                unaryMinusFun.symbol,
                valueArgumentsCount = 0,
                typeArgumentsCount = 0
            ).apply {
                dispatchReceiver = this@negate
            }
        }
    }
}

/** Return `this - 1` if the expression is const, otherwise call dec(). */
internal fun IrExpression.decrement(): IrExpression {
    return when (val thisValue = (this as? IrConst<*>)?.value) {
        is Int -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, thisValue - 1)
        is Long -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, thisValue - 1)
        is Char -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Char, thisValue - 1)
        else -> {
            val decFun = type.getClass()!!.functions.single {
                it.name == OperatorNameConventions.DEC &&
                        it.valueParameters.isEmpty()
            }
            IrCallImpl(
                startOffset, endOffset, type,
                decFun.symbol,
                valueArgumentsCount = 0,
                typeArgumentsCount = 0
            ).apply {
                dispatchReceiver = this@decrement
            }
        }
    }
}

internal val IrExpression.canHaveSideEffects: Boolean
    get() = this !is IrExpressionWithCopy

private fun Any?.toLong(): Long? =
    when (this) {
        is Number -> toLong()
        is Char -> toLong()
        else -> null
    }

internal val IrExpressionWithCopy.constLongValue: Long?
    get() = if (this is IrConst<*>) value.toLong() else null

internal val IrExpression.constLongValue: Long?
    get() = if (this is IrConst<*>) value.toLong() else null

/**
 * If [expression] can have side effects ([IrExpression.canHaveSideEffects]), this function creates a temporary local variable for that
 * expression and returns that variable and an [IrGetValue] for it. Otherwise, it returns no variable and [expression].
 *
 * This helps reduce local variable usage.
 */
internal fun DeclarationIrBuilder.createTemporaryVariableIfNecessary(
    expression: IrExpression, nameHint: String? = null,
    irType: IrType? = null, isMutable: Boolean = false
): Pair<IrVariable?, IrExpressionWithCopy> =
    if (expression !is IrExpressionWithCopy) {
        scope.createTmpVariable(expression, nameHint = nameHint, irType = irType, isMutable = isMutable).let { Pair(it, irGet(it)) }
    } else {
        Pair(null, expression)
    }

internal fun IrExpression.castIfNecessary(targetClass: IrClass) =
    // This expression's type could be Nothing from an exception throw.
    if (type == targetClass.defaultType || type.isNothing()) {
        this
    } else {
        val numberCastFunctionName = Name.identifier("to${targetClass.name.asString()}")
        val castFun = type.getClass()!!.functions.single {
            it.name == numberCastFunctionName &&
                    it.dispatchReceiverParameter != null && it.extensionReceiverParameter == null && it.valueParameters.isEmpty()
        }
        IrCallImpl(
            startOffset, endOffset,
            castFun.returnType, castFun.symbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 0
        ).apply { dispatchReceiver = this@castIfNecessary }
    }