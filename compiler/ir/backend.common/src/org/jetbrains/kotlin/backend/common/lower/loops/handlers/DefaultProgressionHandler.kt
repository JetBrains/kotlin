/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.getPropertyGetter

/** Builds a [HeaderInfo] for progressions not handled by more specialized handlers. */
internal class DefaultProgressionHandler(private val context: CommonBackendContext) :
    ExpressionHandler {

    private val symbols = context.ir.symbols

    override fun matchIterable(expression: IrExpression) = ProgressionType.fromIrType(
        expression.type,
        symbols
    ) != null

    override fun build(expression: IrExpression, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            // Directly use the `first/last/step` properties of the progression.
            val (progressionVar, progressionExpression) = createTemporaryVariableIfNecessary(expression, nameHint = "progression")
            val progressionClass = progressionExpression.type.getClass()!!
            val first = irCall(progressionClass.symbol.getPropertyGetter("first")!!).apply {
                dispatchReceiver = progressionExpression
            }
            val last = irCall(progressionClass.symbol.getPropertyGetter("last")!!).apply {
                dispatchReceiver = progressionExpression.deepCopyWithSymbols()
            }
            val step = irCall(progressionClass.symbol.getPropertyGetter("step")!!).apply {
                dispatchReceiver = progressionExpression.deepCopyWithSymbols()
            }

            ProgressionHeaderInfo(
                ProgressionType.fromIrType(progressionExpression.type, symbols)!!,
                first,
                last,
                step,
                additionalVariables = listOfNotNull(progressionVar),
                direction = ProgressionDirection.UNKNOWN
            )
        }
}