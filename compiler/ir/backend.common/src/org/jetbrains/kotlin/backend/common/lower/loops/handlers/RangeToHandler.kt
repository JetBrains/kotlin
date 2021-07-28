/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.backend.common.lower.loops.ProgressionDirection
import org.jetbrains.kotlin.backend.common.lower.loops.ProgressionHandler
import org.jetbrains.kotlin.backend.common.lower.loops.ProgressionHeaderInfo
import org.jetbrains.kotlin.backend.common.lower.loops.ProgressionType
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

            // Convert range with inclusive upper bound to exclusive upper bound if possible.
            // This affects loop code performance on JVM.
            if (canUseExclusiveUpperBound(last, data)) {
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
            IrConstKind.Char ->
                IrConstImpl.char(startOffset, endOffset, type, IrConstKind.Char.valueOf(irConst).inc())
            IrConstKind.Byte ->
                IrConstImpl.byte(startOffset, endOffset, type, IrConstKind.Byte.valueOf(irConst).inc())
            IrConstKind.Short ->
                IrConstImpl.short(startOffset, endOffset, type, IrConstKind.Short.valueOf(irConst).inc())
            IrConstKind.Int ->
                IrConstImpl.int(startOffset, endOffset, type, IrConstKind.Int.valueOf(irConst).inc())
            IrConstKind.Long ->
                IrConstImpl.long(startOffset, endOffset, type, IrConstKind.Long.valueOf(irConst).inc())
            else ->
                null
        }
    }


}