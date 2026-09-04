/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.LoopData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.irReturnTo
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.updateLoopVariableInBody
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.replaceForLoopNextCall
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturnableBlock
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * Fuses a `for` loop over a sequence: the loop body becomes the consumer body, with `break` and `continue`
 * of the loop replaced by returning `false` and `true` from it.
 */
internal class ForLoopStrategy(
    data: ConsumerData, val loopData: LoopData, expression: IrBlock,
) : ConsumerStrategy(data, expression) {
    override fun getInitialDeclarations(): List<IrVariable> = emptyList()

    override fun getConsumerBuilder(): ConsumerBodyBuilder = { sequenceElement ->
        val [body, loop] = updateLoopVariableInBody(data.builder, loopData.loopVariable, loopData.loopBody, loopData.loop, data.parent)
        data.builder.irReturnableBlock(data.context.irBuiltIns.booleanType) {
            val newLoopVariable = loopData.loopVariable
            newLoopVariable.initializer = newLoopVariable.initializer?.replaceForLoopNextCall(irGet(sequenceElement))
            +newLoopVariable
            val preparedBody = body(newLoopVariable)
            preparedBody.rebindJumps(
                loop,
                { irReturnTo(returnableBlockSymbol, irFalse()) },
                { irReturnTo(returnableBlockSymbol, irTrue()) })
            +preparedBody
            +irReturnTo(returnableBlockSymbol, irTrue())
        }
    }

    override fun createResult(): IrExpression = data.builder.irUnit()
}
