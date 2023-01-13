/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfo
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfoHandler
import org.jetbrains.kotlin.backend.common.lower.loops.IterableHeaderInfo
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.util.OperatorNameConventions

/** Builds a [HeaderInfo] for Sequences not handled by more specialized handlers. */
internal class DefaultSequenceHandler(private val context: CommonBackendContext) : HeaderInfoHandler<IrExpression, Nothing?> {
    private val sequenceClassSymbol = context.ir.symbols.sequence

    override fun matchIterable(expression: IrExpression): Boolean =
        sequenceClassSymbol != null && expression.type.isSubtypeOfClass(sequenceClassSymbol)

    override fun build(expression: IrExpression, data: Nothing?, scopeOwner: IrSymbol): HeaderInfo =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            val iteratorFun =
                sequenceClassSymbol!!.getSimpleFunction(OperatorNameConventions.ITERATOR.asString())!!.owner
            IterableHeaderInfo(
                scope.createTmpVariable(irCall(iteratorFun).apply { dispatchReceiver = expression }, nameHint = "iterator")
            )
        }
}
