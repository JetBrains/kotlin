/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.evaluateFunctionArgumentOnce
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration

/**
 * Fuses `takeWhile` and `dropWhile`: remembers whether the predicate has ever returned `false`,
 * which stops the iteration for `takeWhile` and starts passing the elements downstream for `dropWhile`.
 */
internal class TakeWhileStrategy(val takeWhile: SequenceTransformer.TakeWhile, builderWithParent: IrBuilderWithParent) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(sequenceReplacement: SequenceReplacement): SequenceReplacement {
        val [builder, parent] = builderWithParent
        val [functionVariable, function] = builderWithParent.evaluateFunctionArgumentOnce(takeWhile.function)
        val didTakeOrDropEverFail = builder.scope.createTemporaryVariable(builder.irFalse(), "didTakeOrDropEverFail", isMutable = true)
        val mainBodyBuilder = { sequenceVariable: IrValueDeclaration ->
            with(builder) {
                irBlock {
                    val predicateCall = callPredicate(function, parent, irGet(sequenceVariable))
                    // `dropWhile` does not call the predicate after it returned false once
                    val filterResult = when (takeWhile.takeOrDrop) {
                        TakeOrDrop.Take -> predicateCall
                        TakeOrDrop.Drop ->
                            irIfThenElse(context.irBuiltIns.booleanType, irGet(didTakeOrDropEverFail), irFalse(), predicateCall)
                    }
                    val filterResultVariable = irTemporary(filterResult)
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
        val initialDeclarations = listOfNotNull(functionVariable) + sequenceReplacement.initialDeclarations + didTakeOrDropEverFail
        val finalExpression = sequenceReplacement.finalExpression
        // `takeWhile` stops the iteration through the result of its body, so it can never skip the iteration up front
        return SequenceReplacement(initialDeclarations, sequenceReplacement.skipIteration, mainBodyBuilder, finalExpression)
    }
}
