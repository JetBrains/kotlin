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
import org.jetbrains.kotlin.ir.util.*
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

internal val IrExpression.canChangeValueDuringExecution: Boolean
    get() = when (this) {
        is IrGetValue ->
            !this.symbol.owner.isImmutable
        is IrConst<*>,
        is IrGetObjectValue,
        is IrGetEnumValue ->
            false
        else ->
            true
    }

internal val IrExpression.canHaveSideEffects: Boolean
    get() = !isTrivial()

private fun Any?.toLong(): Long? =
    when (this) {
        is Number -> toLong()
        is Char -> code.toLong()
        else -> null
    }

internal val IrExpression.constLongValue: Long?
    get() = if (this is IrConst<*>) value.toLong() else null

/**
 * If [expression] can have side effects ([IrExpression.canHaveSideEffects]), this function creates a temporary local variable for that
 * expression and returns that variable and an [IrGetValue] for it. Otherwise, it returns no variable and [expression].
 *
 * This helps reduce local variable usage.
 */
internal fun DeclarationIrBuilder.createTemporaryVariableIfNecessary(
    expression: IrExpression,
    nameHint: String? = null,
    irType: IrType? = null,
    isMutable: Boolean = false
): Pair<IrVariable?, IrExpression> =
    if (expression.canHaveSideEffects) {
        scope.createTmpVariable(expression, nameHint = nameHint, irType = irType, isMutable = isMutable).let { Pair(it, irGet(it)) }
    } else {
        Pair(null, expression)
    }

/**
 * If [expression] can change value during execution ([IrExpression.canChangeValueDuringExecution]),
 * this function creates a temporary local variable for that expression and returns that variable and an [IrGetValue] for it.
 * Otherwise, it returns no variable and [expression].
 * Note that a variable expression doesn't have side effects per se, but can change value during execution,
 * so if it's denotes a value that would be used in a loop (say, a loop bound), it should be cached in a temporary at the loop header.
 *
 * This helps reduce local variable usage.
 */
internal fun DeclarationIrBuilder.createLoopTemporaryVariableIfNecessary(
    expression: IrExpression,
    nameHint: String? = null,
    irType: IrType? = null,
    isMutable: Boolean = false
): Pair<IrVariable?, IrExpression> =
    if (expression.canChangeValueDuringExecution) {
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
        val classifier = type.getClass() ?: error("Has to be a class ${type.render()}")
        val castFun = classifier.functions.single {
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