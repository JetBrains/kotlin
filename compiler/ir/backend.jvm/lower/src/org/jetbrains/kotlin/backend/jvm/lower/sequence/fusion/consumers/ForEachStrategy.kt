/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getPredicateArgument
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression

internal class ForEachStrategy(data: ConsumerData, expression: IrCall) : ConsumerStrategy(data, expression) {
    override fun getInitialDeclarations(): List<IrVariable> = emptyList()

    override fun getConsumerBuilder(): ConsumerBodyBuilder? {
        val expression = expression as IrCall
        val predicate = getPredicateArgument(expression, 1) ?: return null
        with(data.builder) {
            return { sequenceElement ->
                irBlock {
                    +callPredicate(predicate, data.parent, irGet(sequenceElement))
                    +irTrue()
                }
            }
        }
    }

    override fun createResult(): IrExpression = data.builder.irUnit()
}
