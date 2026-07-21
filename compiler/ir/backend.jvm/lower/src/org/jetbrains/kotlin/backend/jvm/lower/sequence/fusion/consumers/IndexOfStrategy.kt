/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
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

internal class IndexOfStrategy(data: ConsumerData, expression: IrCall, val indexOfVersion: IndexOfVersion) :
    ConsumerStrategy(data, expression) {
    val builder = data.builder
    val indexVariable = builder.scope.createTemporaryVariable(builder.irInt(-1), "index", isMutable = true)
    val resultVariable = builder.scope.createTemporaryVariable(builder.irInt(-1), "result", isMutable = true)

    override fun getInitialDeclarations(): List<IrVariable> = listOf(indexVariable, resultVariable)

    override fun getConsumerBuilder(): ConsumerBodyBuilder {
        val functionName = when (indexOfVersion) {
            is IndexOfVersion.IndexOf -> "indexOf"
            is IndexOfVersion.IndexOfFirst -> "indexOfFirst"
            is IndexOfVersion.IndexOfLast -> "indexOfLast"
        }
        val predicateOrElementArgument = (expression as IrCall).arguments.getOrNull(1)
            ?: error("Didn't find second argument for function $functionName: ${expression.dump()}")
        return { sequenceElement ->
            val [shouldContinue, condition] = when (indexOfVersion) {
                is IndexOfVersion.IndexOf -> builder.irFalse() to
                        builder.irEquals(builder.irGet(sequenceElement), predicateOrElementArgument)
                is IndexOfVersion.IndexOfFirst -> builder.irFalse() to
                        builder.callPredicate(predicateOrElementArgument, data.parent, builder.irGet(sequenceElement))
                is IndexOfVersion.IndexOfLast -> builder.irTrue() to
                        builder.callPredicate(predicateOrElementArgument, data.parent, builder.irGet(sequenceElement))
            }
            builder.irBlock {
                +increment(indexVariable)
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

    override val canBeRemovedOnEmptySequence = false
}
