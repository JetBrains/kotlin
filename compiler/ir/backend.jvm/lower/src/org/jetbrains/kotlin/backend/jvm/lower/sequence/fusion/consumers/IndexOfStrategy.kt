/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getPredicateArgument
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.increment
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dump

internal sealed class IndexOfVersion {
    object IndexOf : IndexOfVersion()
    object IndexOfFirst : IndexOfVersion()
    object IndexOfLast : IndexOfVersion()
}

/**
 * Fuses `indexOf`, `indexOfFirst` and `indexOfLast`: counts the elements and records the index of the matching one,
 * stopping at the first match except for `indexOfLast`; the result is `-1` if nothing matched.
 */
internal class IndexOfStrategy(data: ConsumerData, expression: IrCall, val indexOfVersion: IndexOfVersion, val context: JvmBackendContext) :
    ConsumerStrategy(data, expression) {
    val builder = data.builder

    // incremented before every element, so that the first element has index 0
    val indexVariable = builder.scope.createTemporaryVariable(builder.irInt(-1), "index", isMutable = true)
    val resultVariable = builder.scope.createTemporaryVariable(builder.irInt(-1), "result", isMutable = true)

    // the element of `indexOf` is evaluated once
    private val elementVariable: IrVariable? = if (indexOfVersion is IndexOfVersion.IndexOf) {
        val element = expression.arguments.getOrNull(1) ?: error("Didn't find the element argument of indexOf: ${expression.dump()}")
        builder.scope.createTemporaryVariable(element, "indexOfElement")
    } else null

    override fun getInitialDeclarations(): List<IrVariable> = listOfNotNull(indexVariable, resultVariable, elementVariable)

    override fun getConsumerBuilder(): ConsumerBodyBuilder? {
        val predicate =
            if (indexOfVersion is IndexOfVersion.IndexOf) null
            else getPredicateArgument(expression as IrCall, 1) ?: return null
        return { sequenceElement ->
            val [shouldContinue, condition] = when (indexOfVersion) {
                is IndexOfVersion.IndexOf -> builder.irFalse() to
                        builder.irEquals(builder.irGet(elementVariable!!), builder.irGet(sequenceElement))
                is IndexOfVersion.IndexOfFirst -> builder.irFalse() to
                        builder.callPredicate(predicate!!, data.parent, builder.irGet(sequenceElement))
                is IndexOfVersion.IndexOfLast -> builder.irTrue() to
                        builder.callPredicate(predicate!!, data.parent, builder.irGet(sequenceElement))
            }
            builder.irBlock {
                +increment(indexVariable, this@IndexOfStrategy.context)
                val thenPart = irBlock {
                    +irSet(resultVariable, irGet(indexVariable))
                    +shouldContinue
                }
                +irIfThenElse(
                    data.context.irBuiltIns.booleanType,
                    condition,
                    thenPart,
                    irTrue()
                )
            }
        }
    }

    override fun createResult(): IrExpression = builder.irGet(resultVariable)
}
