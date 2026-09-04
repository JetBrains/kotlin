/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.FilterVersion
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getPredicateArgument
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.irAsNotNull
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.functions

internal class FilterToStrategy(data: ConsumerData, expression: IrCall, val version: FilterVersion) :
    ConsumerStrategy(data, expression) {
    private var destinationVariable: IrVariable? = null
    override fun getInitialDeclarations(): List<IrVariable> {
        val expression = expression as IrCall
        val destination = expression.arguments.getOrNull(1) ?: error("No destination argument for filterTo")
        destinationVariable = data.builder.scope.createTemporaryVariable(destination, "filterToDestination")
        return listOf(destinationVariable!!)
    }

    override fun getConsumerBuilder(): ConsumerBodyBuilder? {
        val addFunction = data.context.irBuiltIns.mutableCollectionClass.owner.functions.singleOrNull {
            it.name.asString() == "add" && it.parameters.size == 2
        } ?: return null
        val expression = expression as IrCall
        val predicate = if (version == FilterVersion.FilterNotNull) null else (getPredicateArgument(expression, 2) ?: return null)
        return { sequenceElement ->
            val builder = data.builder
            val parent = data.parent
            with(builder) {
                val condition = if (version == FilterVersion.FilterNotNull) {
                    irNot(irEquals(irGet(sequenceElement), irNull()))
                } else {
                    val call = callPredicate(predicate!!, parent, irGet(sequenceElement))
                    if (version == FilterVersion.FilterNot) irNot(call) else call
                }

                val destinationAddCall = irCall(addFunction).apply {
                    arguments[0] = irGet(destinationVariable!!)
                    if (version == FilterVersion.FilterNotNull) {
                        arguments[1] = irAsNotNull(irGet(sequenceElement))
                    } else {
                        arguments[1] = irGet(sequenceElement)
                    }
                }
                /*
                if (filterCondition) {
                    destination.add(sequenceElement)
                }
                false
                 */
                irBlock {
                    +irIfThen(
                        context.irBuiltIns.unitType,
                        condition,
                        destinationAddCall
                    )
                    +irTrue()
                }
            }
        }
    }

    override fun createResult(): IrExpression = data.builder.irGet(destinationVariable!!)

    override val canBeRemovedOnEmptySequence: Boolean = true
}
