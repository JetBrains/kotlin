/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getPredicateArgument
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.makeNullable

/**
 * Consumer strategy for lowering `first`/`firstOrNull`/`find`/`last`/`lastOrNull`/`findLast`.
 *
 * `isFirst` differentiates between `first`/`last`, `find`/`findLast` and `firstOrNull`/`lastOrNull`.
 *
 * `isFind` differentiates between `first`/`last` and `find`/`findLast`.
 *
 * `isOrNull` differentiates between `first`/`last` and `firstOrNull`/`lastOrNull`.
 */
internal open class FirstLastStrategy(
    data: ConsumerData,
    expression: IrCall,
    val isFirst: Boolean,
    val isOrNull: Boolean,
    val isFind: Boolean,
) : ConsumerStrategy(data, expression) {
    val resultVariable: IrVariable = data.builder.scope.createTemporaryVariable(
        data.builder.irNull(),
        "result",
        isMutable = true,
        irType = expression.type.makeNullable()
    )

    // this is needed for all the versions of first/last which throw an exception on empty sequence
    val skippedVariable: IrVariable = data.builder.scope.createTemporaryVariable(data.builder.irTrue(), "skipped", isMutable = true)

    override fun getInitialDeclarations(): List<IrVariable> = listOf(skippedVariable, resultVariable)

    override fun getConsumerBuilder(): ConsumerBodyBuilder? {
        val call = expression as IrCall
        val functionName = call.symbol.owner.name.asString()
        val predicate = getPredicateArgument(call, 1)
        if (predicate == null && (functionName == FIND || functionName == FIND_LAST)) return null
        return { sequenceElement ->
            data.builder.irBlock {
                val predicateCall = if (predicate != null) {
                    callPredicate(predicate, data.parent, irGet(sequenceElement))
                } else null

                if (predicateCall != null) {
                    // in the case of this being first or last, this is the predicate overload
                    val predicateResult = scope.createTemporaryVariable(predicateCall, nameHint = "predicateResult")
                    +predicateResult
                    +irIfThen(irGet(predicateResult), irBlock {
                        +irSet(resultVariable, irGet(sequenceElement))
                        +irSet(skippedVariable, irFalse())
                    })
                    val shouldContinueSearching = if (isFirst) irNot(irGet(predicateResult)) else irTrue()
                    +shouldContinueSearching
                } else {
                    // this is the non-predicate overload of first or last
                    +irSet(resultVariable, irGet(sequenceElement))
                    +irSet(skippedVariable, irFalse())
                    val shouldContinueSearching = if (isFirst) irFalse() else irTrue()
                    +shouldContinueSearching
                }
            }
        }
    }

    override fun createResult(): IrExpression = data.builder.irBlock {
        if (isOrNull || isFind) {
            +irGet(resultVariable)
        } else {
            // throw the exception on first or last
            val wasSkipped = irGet(skippedVariable)
            val throwException = irThrow(
                irCallConstructor(data.context.symbols.noSuchElementExceptionCtorString, emptyList()).apply {
                    arguments[0] = irString("Sequence is empty.")
                }
            )
            +irIfThen(wasSkipped, throwException)
            +irGet(resultVariable)
        }
    }

    override val canBeRemovedOnEmptySequence: Boolean = false
}


