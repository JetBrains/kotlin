/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName

/** Builds a [HeaderInfo] for progressions built using the `until` extension function. */
internal class UntilHandler(private val context: CommonBackendContext) : HeaderInfoHandler<IrCall, ProgressionType> {
    private val progressionElementTypes = context.symbols.progressionElementTypes

    override fun matchIterable(expression: IrCall): Boolean {
        val callee = expression.symbol.owner
        return callee.hasShape(extensionReceiver = true, regularParameters = 1) &&
                callee.parameters[0].type in progressionElementTypes &&
                callee.parameters[1].type in progressionElementTypes &&
                callee.kotlinFqName == FqName("kotlin.ranges.until")
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = expression.arguments[0]!!,
                last = expression.arguments[1]!!,
                step = irInt(1),
                canOverflow = false,
                isLastInclusive = false,
                direction = ProgressionDirection.INCREASING
            )
        }
}
