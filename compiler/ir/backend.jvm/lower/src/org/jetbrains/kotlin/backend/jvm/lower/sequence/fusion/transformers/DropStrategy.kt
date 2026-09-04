/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.increment
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration

/**
 * Fuses `drop(n)`: the first `n` elements are counted and not passed to the downstream body.
 */
internal class DropStrategy(val drop: SequenceTransformer.Drop, builderWithParent: IrBuilderWithParent, val context: JvmBackendContext) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement {
        val builder = builderWithParent.first
        val counter = builderWithParent.createElementCounter(drop.argument)
        val mainBodyBuilder = { sequenceVariable: IrValueDeclaration ->
            builder.irBlock {
                +irIfThenElse(
                    context.irBuiltIns.booleanType,
                    irIntLess(irGet(counter.count), irGet(counter.argument)),
                    irBlock {
                        +increment(counter.count, this@DropStrategy.context)
                        +irTrue()
                    },
                    sequenceReplacement.mainBodyBuilder(sequenceVariable),
                )
            }
        }
        return SequenceReplacement(
            listOf(counter.count, counter.argument, counter.negativeArgumentCheck) + sequenceReplacement.initialDeclarations,
            sequenceReplacement.skipIteration,
            mainBodyBuilder,
            sequenceReplacement.finalExpression,
            listOf(counter.argument) + sequenceReplacement.leadingDropCounts,
        )
    }
}
