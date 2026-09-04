/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration

internal class TakeWhileStrategy(val takeWhile: SequenceTransformer.TakeWhile, builderWithParent: IrBuilderWithParent) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(sequenceReplacement: SequenceReplacement): SequenceReplacement {
        val builder = builderWithParent.first
        val didTakeOrDropEverFail = builder.scope.createTemporaryVariable(builder.irFalse(), "didTakeOrDropEverFail", isMutable = true)
        val mainBodyBuilder = { sequenceVariable: IrValueDeclaration ->
            with(builder) {
                irBlock {
                    val filterResultVariable = irTemporary(takeWhile.predicateCall(builderWithParent)(sequenceVariable))
                    +irIfThen(
                        irNot(irGet(filterResultVariable)),
                        irSet(didTakeOrDropEverFail, builder.irTrue())
                    )
                    when (takeWhile.takeOrDrop) {
                        TakeOrDrop.Take -> +irIfThenElse(
                            context.irBuiltIns.booleanType,
                            irGet(didTakeOrDropEverFail),
                            irFalse(),
                            sequenceReplacement.mainBodyBuilder(sequenceVariable),
                        )
                        TakeOrDrop.Drop -> +irIfThenElse(
                            context.irBuiltIns.booleanType,
                            irGet(didTakeOrDropEverFail),
                            sequenceReplacement.mainBodyBuilder(sequenceVariable),
                            irTrue(),
                        )
                    }
                }
            }
        }
        val initialDeclarations = sequenceReplacement.initialDeclarations + didTakeOrDropEverFail
        val finalExpression = sequenceReplacement.finalExpression
        val checkBeforeNextElement = when (takeWhile.takeOrDrop) {
            TakeOrDrop.Drop -> {
                sequenceReplacement.checkBeforeNextElement
            }
            TakeOrDrop.Take -> {
                sequenceReplacement.checkBeforeNextElement?.let {
                    builder.irCall(builder.context.irBuiltIns.ororSymbol).apply {
                        arguments[0] = it
                        arguments[1] = builder.irGet(didTakeOrDropEverFail)
                    }
                } ?: builder.irGet(didTakeOrDropEverFail)
            }
        }
        return SequenceReplacement(initialDeclarations, checkBeforeNextElement, mainBodyBuilder, finalExpression)
    }
}
