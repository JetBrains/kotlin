/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.increment
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor

internal class WithIndexStrategy(val withIndex: SequenceTransformer.WithIndex, builderWithParent: IrBuilderWithParent) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(sequenceReplacement: SequenceReplacement): SequenceReplacement {
        val builder = builderWithParent.first
        val sequenceType = withIndex.call.type as? IrSimpleType ?: error("WithIndex call has non-simple type")
        val indexedValueType =
            sequenceType.arguments.singleOrNull()?.typeOrNull as? IrSimpleType ?: error("WithIndex call has no indexed value type")

        val indexedValueClassifier =
            indexedValueType.classifierOrNull as? IrClassSymbol ?: error("IndexedValue has no class")

        val indexedValueClass = indexedValueClassifier.owner

        val constructor =
            indexedValueClass.primaryConstructor
                ?: error("IndexedValue has no primary constructor")

        val valueType =
            indexedValueType.arguments.singleOrNull()?.typeOrNull
                ?: error("Expected IndexedValue<T>")

        val indexVariable = builder.scope.createTemporaryVariable(builder.irInt(0), nameHint = "withIndexIndex", isMutable = true)

        val mainBodyBuilder = { sequenceVariable: IrValueDeclaration ->
            with(builder) {
                val indexedValueCall = builder.irCallConstructor(
                    constructor.symbol,
                    listOf(valueType)
                ).apply {
                    arguments[0] = irGet(indexVariable)
                    arguments[1] = irGet(sequenceVariable)
                }
                builder.irBlock {
                    val resultVariable = irTemporary(indexedValueCall, nameHint = "withIndexResult")
                    +increment(indexVariable)
                    +sequenceReplacement.mainBodyBuilder(resultVariable)
                    +irTrue()
                }
            }
        }
        val initialDeclarations = sequenceReplacement.initialDeclarations + listOf(indexVariable)
        val finalExpression = sequenceReplacement.finalExpression
        return SequenceReplacement(initialDeclarations, mainBodyBuilder, finalExpression)
    }
}
