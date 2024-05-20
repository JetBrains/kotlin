/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.irCastIfNeeded
import org.jetbrains.kotlin.ir.util.shallowCopy

/** Builds a [HeaderInfo] for progressions not handled by more specialized handlers. */
internal class DefaultProgressionHandler(
    private val context: CommonBackendContext, private val allowUnsignedBounds: Boolean = false
) : HeaderInfoHandler<IrExpression, Nothing?> {
    private val symbols = context.ir.symbols
    private val rangeClassesTypes = symbols.rangeClasses.map { it.defaultType }.toSet()

    // Infer type of `IrGetValue(IrVariable())` from type of its initializer
    private fun unwrappedType(expression: IrExpression): IrType {
        val initializer = ((expression as? IrGetValue)?.symbol?.owner as? IrVariable)?.initializer
        return ((initializer as? IrStatementContainer)?.statements?.lastOrNull() as? IrExpression)?.type
            ?: expression.type
    }

    override fun matchIterable(expression: IrExpression): Boolean =
        ProgressionType.fromIrType(unwrappedType(expression), symbols, allowUnsignedBounds) != null

    override fun build(expression: IrExpression, data: Nothing?, scopeOwner: IrSymbol): HeaderInfo =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // Directly use the `first/last/step` properties of the progression.
            val unwrappedType = unwrappedType(expression)
            val (progressionVar, progressionExpression) = createTemporaryVariableIfNecessary(
                irCastIfNeeded(expression, unwrappedType),  // WASM backend needs this cast, otherwise test for KT-67695 fails in runtime
                nameHint = "progression"
            )
            val progressionClass = unwrappedType.getClass()!!
            val first = irCall(progressionClass.symbol.getPropertyGetter("first")!!).apply {
                dispatchReceiver = progressionExpression.shallowCopy()
            }
            val last = irCall(progressionClass.symbol.getPropertyGetter("last")!!).apply {
                dispatchReceiver = progressionExpression.shallowCopy()
            }

            // *Ranges (e.g., IntRange) have step == 1 and is always increasing.
            val isRange = unwrappedType in rangeClassesTypes
            val step = if (isRange) {
                irInt(1)
            } else {
                irCall(progressionClass.symbol.getPropertyGetter("step")!!).apply {
                    dispatchReceiver = progressionExpression.shallowCopy()
                }
            }
            val direction = if (isRange) ProgressionDirection.INCREASING else ProgressionDirection.UNKNOWN

            ProgressionHeaderInfo(
                ProgressionType.fromIrType(unwrappedType, symbols, allowUnsignedBounds)!!,
                first,
                last,
                step,
                additionalStatements = listOfNotNull(progressionVar),
                direction = direction
            )
        }
}
