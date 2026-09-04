/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties

/**
 * Fuses `toList`, `toMutableList`, `toSet`, `toMutableSet` and `toCollection`: adds every element to the destination,
 * reproducing the collection types returned by the stdlib.
 */
internal class ToCollectionStrategy(
    data: ConsumerData, expression: IrCall, val collectionVersion: CollectionVersion, val isReadOnly: Boolean,
) :
    ConsumerStrategy(data, expression) {
    private val destinationVariable: IrVariable = when (collectionVersion) {
        CollectionVersion.List -> {
            val arrayListConstructor = data.context.symbols.arrayListConstructor

            val createArrayListCall = data.builder.irCall(arrayListConstructor)
            data.builder.scope.createTemporaryVariable(createArrayListCall, "toListDestination")
        }
        CollectionVersion.Set -> {

            val setConstructor = data.context.symbols.linkedHashSetConstructor

            val createSetCall = data.builder.irCall(setConstructor)
            data.builder.scope.createTemporaryVariable(createSetCall, "toSetDestination")
        }
        CollectionVersion.Collection -> {
            val destinationArgument = expression.arguments[1]
                ?: error("toCollection missing destination argument")

            data.builder.scope.createTemporaryVariable(destinationArgument, "toCollectionDestination", irType = expression.type)
        }
    }

    // for toSet() exactly, sequenceOf(1, 1).toSet() yields a LinkedHashSet, while sequenceOf(1).toSet() yields a setOf(1)
    private val tracksElementCount = collectionVersion is CollectionVersion.Set && isReadOnly

    private val hasAtLeastOneElement: IrVariable? = if (tracksElementCount)
        data.builder.scope.createTemporaryVariable(data.builder.irFalse(), "hasAtLeastOneElement", isMutable = true)
    else null

    private val hasMoreThanOneElement: IrVariable? = if (tracksElementCount)
        data.builder.scope.createTemporaryVariable(data.builder.irFalse(), "hasMoreThanOneElement", isMutable = true)
    else null

    private val sizeGetter = data.context.irBuiltIns.collectionClass.owner.properties.single { it.name.asString() == "size" }.getter!!

    override fun getInitialDeclarations(): List<IrVariable> =
        listOfNotNull(destinationVariable, hasAtLeastOneElement, hasMoreThanOneElement)

    override fun getConsumerBuilder(): ConsumerBodyBuilder? {
        val addFunction = data.context.irBuiltIns.mutableCollectionClass.owner.functions.singleOrNull {
            it.name.asString() == "add" && it.parameters.size == 2
        } ?: return null
        with(data.builder) {
            return { sequenceElement ->
                val destinationAddCall = irCall(addFunction).apply {
                    arguments[0] = irGet(destinationVariable)
                    arguments[1] = irGet(sequenceElement)
                }
                irBlock {
                    if (hasAtLeastOneElement != null && hasMoreThanOneElement != null) {
                        +irSet(hasMoreThanOneElement, irGet(hasAtLeastOneElement))
                        +irSet(hasAtLeastOneElement, irTrue())
                    }
                    +destinationAddCall
                    +irTrue()
                }
            }
        }
    }

    override fun createResult(): IrExpression {
        val destination = destinationVariable
        val resultType = expression.type
        if (!isReadOnly) {
            // the synthetic `ArrayList` and `LinkedHashSet` classes have neither type parameters nor supertypes
            return if (collectionVersion is CollectionVersion.Collection) data.builder.irGet(destination)
            else data.builder.irImplicitCast(data.builder.irGet(destination), resultType)
        }
        val symbols = data.context.symbols
        val irBuiltIns = data.context.irBuiltIns
        val [emptyFunction, singletonFunction] = when (collectionVersion) {
            CollectionVersion.List -> symbols.emptyList to symbols.listOfElement
            CollectionVersion.Set -> symbols.emptySet to symbols.setOfElement
            CollectionVersion.Collection -> return data.builder.irGet(destination)
        }
        val getFunction = irBuiltIns.listClass.owner.functions.single { it.name.asString() == "get" }
        val iteratorFunction = irBuiltIns.iterableClass.owner.functions.single { it.name.asString() == "iterator" }
        val nextFunction = irBuiltIns.iteratorClass.owner.functions.single { it.name.asString() == "next" }
        return data.builder.irBlock(resultType = resultType) {
            val size = irTemporary(irCall(sizeGetter).apply { arguments[0] = irGet(destination) }, nameHint = "toCollectionSize")
            val firstElement = when (collectionVersion) {
                CollectionVersion.List -> irCall(getFunction.symbol, irBuiltIns.anyNType).apply {
                    arguments[0] = irGet(destination)
                    arguments[1] = irInt(0)
                }
                else -> irCall(nextFunction.symbol, irBuiltIns.anyNType).apply {
                    arguments[0] = irCall(iteratorFunction).apply { arguments[0] = irGet(destination) }
                }
            }
            val isSingleton = hasMoreThanOneElement?.let { irNot(irGet(it)) } ?: irEquals(irGet(size), irInt(1))
            // for read-only sets and lists, an empty or singleton collection returns emptySet()/emptyList() or setOf(el)/listOf(el) respectively
            +irWhen(
                resultType,
                listOf(
                    irBranch(irEquals(irGet(size), irInt(0)), irImplicitCast(irCall(emptyFunction), resultType)),
                    irBranch(
                        isSingleton,
                        irImplicitCast(irCall(singletonFunction).apply { arguments[0] = firstElement }, resultType),
                    ),
                    irElseBranch(irImplicitCast(irGet(destination), resultType)),
                ),
            )
        }
    }
}
