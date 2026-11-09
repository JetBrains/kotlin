/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.util.OperatorNameConventions

/** Builds a [HeaderInfo] for progressions built using the `rangeTo` function. */
internal class RangeToHandler(private val context: CommonBackendContext) : HeaderInfoHandler<IrCall, ProgressionType> {
    private val preferJavaLikeCounterLoop = context.preferJavaLikeCounterLoop
    private val progressionElementTypes = context.symbols.progressionElementTypes

    override fun matchIterable(expression: IrCall): Boolean { TODO() }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol) =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            val first = expression.arguments[0]!!
            val last = expression.arguments[1]!!
            val step = irInt(1)
            val direction = ProgressionDirection.INCREASING

            if (preferJavaLikeCounterLoop) {
                // Convert range with inclusive upper bound to exclusive upper bound if possible.
                // This affects loop code performance on JVM.
                val lastExclusive = last.convertToExclusiveUpperBound(data)
                if (lastExclusive != null) {
                    return@with ProgressionHeaderInfo(
                        data,
                        first = first,
                        last = lastExclusive,
                        step = step,
                        direction = direction,
                        isLastInclusive = false,
                        canOverflow = false,
                        originalLastInclusive = last
                    )
                }
            }

            ProgressionHeaderInfo(data, first = first, last = last, step = step, direction = direction)
        }

    private fun IrExpression.convertToExclusiveUpperBound(progressionType: ProgressionType): IrExpression? { TODO() }

    private val allowedMethods = listOf(
        "kotlin.ByteArray.<get-size>",
        "kotlin.CharArray.<get-size>",
        "kotlin.String.<get-length>",
        "kotlin.ShortArray.<get-size>",
        "kotlin.IntArray.<get-size>",
        "kotlin.LongArray.<get-size>",
        "kotlin.FloatArray.<get-size>",
        "kotlin.DoubleArray.<get-size>",
        "kotlin.BooleanArray.<get-size>",
        "kotlin.collections.List.<get-size>",
        "kotlin.collections.MutableList.<get-size>",
        "kotlin.CharSequence.<get-length>",
        "kotlin.collections.Set.<get-size>",
        "kotlin.collections.MutableSet.<get-size>",
        "kotlin.collections.Map.<get-size>",
        "kotlin.collections.MutableMap.<get-size>",
    )

    private fun convertIrCall(irCall: IrCall): IrExpression? { TODO() }

    private fun convertIrConst(irConst: IrConst): IrExpression? { TODO() }
}
