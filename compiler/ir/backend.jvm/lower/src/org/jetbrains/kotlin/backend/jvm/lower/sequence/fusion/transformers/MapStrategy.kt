/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.increment
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration

internal class MapStrategy(val map: SequenceTransformer.Map, builderWithParent: IrBuilderWithParent) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement {
        val builder = builderWithParent.first
        val mapIndexedVariable = builder.scope.createTemporaryVariable(
            builder.irInt(-1),
            isMutable = true,
            nameHint = "mapIndexedVariable",
        )
        val mainBodyBuilder = { sequenceVariable: IrValueDeclaration ->
            with(builder) {
                val mappedFunctionCall = when (map.predicateCall) {
                    is MapPredicateCall.Indexed -> map.predicateCall.predicate(builderWithParent)(mapIndexedVariable, sequenceVariable)
                    is MapPredicateCall.NonIndexed -> map.predicateCall.predicate(builderWithParent)(sequenceVariable)
                }
                irBlock {
                    if (map.isIndexed) {
                        +builder.increment(mapIndexedVariable)
                    }
                    if (map.isNotNull) {
                        val mapResultVariable = scope.createTemporaryVariable(mappedFunctionCall, nameHint = "mapResult")
                        +mapResultVariable
                        val condition = irEquals(irGet(mapResultVariable), irNull())
                        // if (mapResult == null) return true, otherwise consume
                        +irIfThenElse(
                            context.irBuiltIns.booleanType,
                            condition,
                            irTrue(),
                            sequenceReplacement.mainBodyBuilder(mapResultVariable),
                        )
                    } else {
                        // TODO: this declaration could be removed possibly
                        val mapResultVariable = scope.createTemporaryVariable(mappedFunctionCall, nameHint = "mapResult")
                        +mapResultVariable
                        +sequenceReplacement.mainBodyBuilder(mapResultVariable)
                    }
                }
            }
        }
        val initialDeclarations =
            if (map.isIndexed) sequenceReplacement.initialDeclarations + mapIndexedVariable
            else sequenceReplacement.initialDeclarations
        val finalExpression = sequenceReplacement.finalExpression
        return SequenceReplacement(initialDeclarations, mainBodyBuilder, finalExpression)
    }
}
