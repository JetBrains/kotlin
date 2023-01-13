/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol

/** Builds a [HeaderInfo] for progressions built using the `rangeUntil` member function (`..<` operator). */
internal class RangeUntilHandler(private val context: CommonBackendContext) : HeaderInfoHandler<IrCall, ProgressionType> {
    private val progressionElementTypes = context.ir.symbols.progressionElementTypes

    override fun matchIterable(expression: IrCall): Boolean {
        val callee = expression.symbol.owner
        return callee.valueParameters.singleOrNull()?.type in progressionElementTypes &&
                callee.extensionReceiverParameter == null &&
                callee.dispatchReceiverParameter?.type in progressionElementTypes &&
                callee.name.asString() == "rangeUntil"
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = expression.dispatchReceiver!!,
                last = expression.getValueArgument(0)!!,
                step = irInt(1),
                canOverflow = false,
                isLastInclusive = false,
                direction = ProgressionDirection.INCREASING
            )
        }
}
