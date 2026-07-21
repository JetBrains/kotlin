/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.functions

internal class ToCollectionStrategy(
    data: ConsumerData, expression: IrCall, val collectionVersion: CollectionVersion,
) :
    ConsumerStrategy(data, expression) {
    private var destinationVariable: IrVariable? = null
    override fun getInitialDeclarations(): List<IrVariable> {
        when (collectionVersion) {
            CollectionVersion.List -> {
                val arrayListConstructor = data.context.symbols.arrayListConstructor

                val createArrayListCall = data.builder.irCall(arrayListConstructor)
                destinationVariable = data.builder.scope.createTemporaryVariable(createArrayListCall, "toListDestination")
            }
            CollectionVersion.Set -> {

                val setConstructor = data.context.symbols.linkedHashSetConstructor

                val createSetCall = data.builder.irCall(setConstructor)
                destinationVariable = data.builder.scope.createTemporaryVariable(createSetCall, "toSetDestination")
            }
            CollectionVersion.Collection -> {
                val destinationArgument = (expression as IrCall).arguments[1]
                    ?: error("toCollection missing destination argument")

                destinationVariable = data.builder.scope.createTemporaryVariable(destinationArgument, "toCollectionDestination")
            }
        }
        return listOf(destinationVariable!!)
    }

    override fun getConsumerBuilder(): ConsumerBodyBuilder? {
        val addFunction = data.context.irBuiltIns.mutableCollectionClass.owner.functions.singleOrNull {
            it.name.asString() == "add" && it.parameters.size == 2
        } ?: return null
        with(data.builder) {
            return { sequenceElement ->
                val destinationAddCall = irCall(addFunction).apply {
                    arguments[0] = irGet(destinationVariable!!)
                    arguments[1] = irGet(sequenceElement)
                }
                irBlock {
                    +destinationAddCall
                    +irTrue()
                }
            }
        }
    }

    override fun createResult(): IrExpression = data.builder.irGet(destinationVariable!!)
}
