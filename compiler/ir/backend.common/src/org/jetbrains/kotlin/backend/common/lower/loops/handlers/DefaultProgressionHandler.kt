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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
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

    // Function inliner often erases type of range in for loop expression, like
    //   val temp: Iterable<Any> = IntRange(...)
    //   for (i in temp) { ... }
    // Type `Iterable<Any>` would prevent the optimization, so initial type should be taken (IntRange in the example above),
    // which would give `Int` as loop variable type, since IntRange implements Iterable<Int>
    override fun matchIterable(expression: IrExpression): Boolean =
        ProgressionType.fromIrType(expression.getMostPreciseTypeFromValInitializer(), symbols, allowUnsignedBounds) != null

    override fun build(expression: IrExpression, data: Nothing?, scopeOwner: IrSymbol): HeaderInfo =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // Directly use the `first/last/step` properties of the progression.
            val unwrappedType = expression.getMostPreciseTypeFromValInitializer()
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
