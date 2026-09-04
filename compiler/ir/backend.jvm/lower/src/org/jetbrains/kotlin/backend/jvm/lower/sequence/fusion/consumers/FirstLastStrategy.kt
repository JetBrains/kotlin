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
import org.jetbrains.kotlin.ir.builders.irImplicitCast
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
    val foundVariable: IrVariable = data.builder.scope.createTemporaryVariable(data.builder.irFalse(), "found", isMutable = true)

    override fun getInitialDeclarations(): List<IrVariable> = listOf(foundVariable, resultVariable)

    override fun getConsumerBuilder(): ConsumerBodyBuilder? {
        val call = expression as IrCall
        val predicate = getPredicateArgument(call, 1)
        // there was a predicate argument, but it wasn't safe to move it
        if (predicate == null && call.arguments.size > 1) return null
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
                        +irSet(foundVariable, irTrue())
                    })
                    val shouldContinueSearching = if (isFirst) irNot(irGet(predicateResult)) else irTrue()
                    +shouldContinueSearching
                } else {
                    // this is the non-predicate overload of first or last
                    +irSet(resultVariable, irGet(sequenceElement))
                    +irSet(foundVariable, irTrue())
                    val shouldContinueSearching = if (isFirst) irFalse() else irTrue()
                    +shouldContinueSearching
                }
            }
        }
    }

    protected open val notFoundMessage: String
        get() = if ((expression as IrCall).arguments.size > 1) "Sequence contains no element matching the predicate." else "Sequence is empty."

    override fun createResult(): IrExpression = data.builder.irBlock(resultType = expression.type) {
        if (isOrNull || isFind) {
            +irGet(resultVariable)
        } else {
            // throw the exception on first or last
            val wasSkipped = irNot(irGet(foundVariable))
            val throwException = irThrow(
                irCallConstructor(data.context.symbols.noSuchElementExceptionCtorString, emptyList()).apply {
                    arguments[0] = irString(notFoundMessage)
                }
            )
            +irIfThen(wasSkipped, throwException)
            // resultVariable is null only initially, wasSkipped assures us that it is not null if the original element was not nullable
            +irImplicitCast(irGet(resultVariable), expression.type)
        }
    }
}
