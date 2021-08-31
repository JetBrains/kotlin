/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.util.OperatorNameConventions

/** Builds a [HeaderInfo] for progressions built using the `rangeTo` function. */
internal class RangeToHandler(private val context: CommonBackendContext) :
    ProgressionHandler {

    private val preferJavaLikeCounterLoop = context.preferJavaLikeCounterLoop

    private val progressionElementTypes = context.ir.symbols.progressionElementTypes

    override val matcher = SimpleCalleeMatcher {
        dispatchReceiver { it != null && it.type in progressionElementTypes }
        fqName { it.pathSegments().last() == OperatorNameConventions.RANGE_TO }
        parameterCount { it == 1 }
        parameter(0) { it.type in progressionElementTypes }
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol) =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            val last = expression.getValueArgument(0)!!

            if (preferJavaLikeCounterLoop && canUseExclusiveUpperBound(last, data)) {
                // Convert range with inclusive upper bound to exclusive upper bound if possible.
                // This affects loop code performance on JVM.
                val lastExclusive = last.convertToExclusiveUpperBound()
                if (lastExclusive != null) {
                    return@with ProgressionHeaderInfo(
                        data,
                        first = expression.dispatchReceiver!!,
                        last = lastExclusive,
                        step = irInt(1),
                        direction = ProgressionDirection.INCREASING,
                        isLastInclusive = false,
                        originalLastInclusive = last
                    )
                }
            }

            ProgressionHeaderInfo(
                data,
                first = expression.dispatchReceiver!!,
                last = last,
                step = irInt(1),
                direction = ProgressionDirection.INCREASING
            )
        }

    private fun canUseExclusiveUpperBound(last: IrExpression, progressionType: ProgressionType): Boolean {
        val lastLongValue = last.constLongValue
            ?: return false
        return if (progressionType is UnsignedProgressionType) {
            lastLongValue != -1L
        } else {
            lastLongValue != progressionType.maxValueAsLong
        }
    }

    private fun IrExpression.convertToExclusiveUpperBound(): IrConstImpl<out Any>? {
        val irConst = this as? IrConst<*> ?: return null
        return when (irConst.kind) {
            IrConstKind.Char -> {
                val charValue = IrConstKind.Char.valueOf(irConst)
                if (charValue != Char.MAX_VALUE)
                    IrConstImpl.char(startOffset, endOffset, type, charValue.inc())
                else
                    null
            }
            IrConstKind.Byte -> {
                val byteValue = IrConstKind.Byte.valueOf(irConst)
                if (byteValue != Byte.MAX_VALUE)
                    IrConstImpl.byte(startOffset, endOffset, type, byteValue.inc())
                else
                    null
            }
            IrConstKind.Short -> {
                val shortValue = IrConstKind.Short.valueOf(irConst)
                if (shortValue != Short.MAX_VALUE)
                    IrConstImpl.short(startOffset, endOffset, type, shortValue.inc())
                else
                    null
            }
            IrConstKind.Int -> {
                val intValue = IrConstKind.Int.valueOf(irConst)
                if (intValue != Int.MAX_VALUE)
                    IrConstImpl.int(startOffset, endOffset, type, intValue.inc())
                else
                    null
            }
            IrConstKind.Long -> {
                val longValue = IrConstKind.Long.valueOf(irConst)
                if (longValue != Long.MAX_VALUE)
                    IrConstImpl.long(startOffset, endOffset, type, longValue.inc())
                else
                    null
            }
            else ->
                null
        }
    }


}