/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getPredicateArgument
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.expressions.IrCall

internal class FirstNotNullOfStrategy(data: ConsumerData, expression: IrCall, isOrNull: Boolean) :
    FirstLastStrategy(data, expression, true, isOrNull) {
    val builder = data.builder

    override fun getConsumerBuilder(): ConsumerBodyBuilder? {
        val transform = getPredicateArgument(expression as IrCall, 1) ?: return null
        with(builder) {
            return { sequenceElement ->
                irBlock {
                    val transformResult = callPredicate(transform, data.parent, irGet(sequenceElement))
                    val transformResultVariable = scope.createTemporaryVariable(transformResult, "transformResult")
                    +transformResultVariable
                    val isTransformNotNull = irNotEquals(irGet(transformResultVariable), irNull())
                    val thenPart = irBlock {
                        +irSet(resultVariable, irGet(transformResultVariable))
                        +irSet(skippedVariable, irFalse())
                    }
                    val isFoundVariable = scope.createTemporaryVariable(isTransformNotNull, "isFound")
                    +isFoundVariable
                    +irIfThen(irGet(isFoundVariable), thenPart)
                    // if didn't find anything, continue searching
                    +irNot(irGet(isFoundVariable))
                }
            }
        }
    }
}
