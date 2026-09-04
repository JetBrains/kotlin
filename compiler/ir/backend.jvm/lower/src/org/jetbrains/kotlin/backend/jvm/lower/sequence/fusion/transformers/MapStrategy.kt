/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.evaluateFunctionArgumentOnce
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.increment
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.types.makeNotNull

internal class MapStrategy(val map: SequenceTransformer.Map, builderWithParent: IrBuilderWithParent, val context: JvmBackendContext) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement {
        val [builder, parent] = builderWithParent
        val [functionVariable, function] = builderWithParent.evaluateFunctionArgumentOnce(map.function)
        val mapIndexedVariable = builder.scope.createTemporaryVariable(
            builder.irInt(-1),
            isMutable = true,
            nameHint = "mapIndexedVariable",
        )
        val mainBodyBuilder = { sequenceVariable: IrValueDeclaration ->
            with(builder) {
                val mappedFunctionCall =
                    if (map.isIndexed) callPredicate(function, parent, irGet(mapIndexedVariable), irGet(sequenceVariable))
                    else callPredicate(function, parent, irGet(sequenceVariable))
                irBlock {
                    if (map.isIndexed) {
                        +increment(mapIndexedVariable, this@MapStrategy.context)
                    }
                    if (map.isNotNull) {
                        val mapResultVariable = scope.createTemporaryVariable(mappedFunctionCall, nameHint = "mapResult")
                        +mapResultVariable
                        val condition = irEquals(irGet(mapResultVariable), irNull())
                        +irIfThenElse(
                            context.irBuiltIns.booleanType,
                            condition,
                            irTrue(),
                            irBlock {
                                val notNullResultVariable = irTemporary(
                                    irImplicitCast(irGet(mapResultVariable), mapResultVariable.type.makeNotNull()),
                                    nameHint = "notNullMapResult",
                                )
                                +sequenceReplacement.mainBodyBuilder(notNullResultVariable)
                            },
                        )
                    } else {
                        val mapResultVariable = scope.createTemporaryVariable(mappedFunctionCall, nameHint = "mapResult")
                        +mapResultVariable
                        +sequenceReplacement.mainBodyBuilder(mapResultVariable)
                    }
                }
            }
        }
        // the function call is evaluated before all transformers and the consumer after it
        val initialDeclarations = listOfNotNull(functionVariable) + sequenceReplacement.initialDeclarations +
                listOfNotNull(mapIndexedVariable.takeIf { map.isIndexed })
        val finalExpression = sequenceReplacement.finalExpression
        val skipIteration = sequenceReplacement.skipIteration
        return SequenceReplacement(initialDeclarations, skipIteration, mainBodyBuilder, finalExpression)
    }
}
