/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.increment
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import kotlin.collections.get

internal class TakeStrategy(val take: SequenceTransformer.Take, builderWithParent: IrBuilderWithParent) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement {
        val [builder, parent] = builderWithParent
        with(builder) {
            val takeVariable = scope.createTemporaryVariable(
                irInt(0),
                isMutable = true,
                nameHint = "takeVar"
            )
            val takeArgumentVariable = scope.createTemporaryVariable(
                take.argument.deepCopyWithSymbols(parent),
                nameHint = "takeArgument",
            )
            val classifier = takeVariable.type.classifierOrNull
            val lessThanSymbol = context.irBuiltIns.lessFunByOperandType[classifier]
                ?: error("No lessThan function found for type ${takeVariable.type}")
            val lessOrEqualSymbol = context.irBuiltIns.lessOrEqualFunByOperandType[classifier]
                ?: error("No lessOrEqual function found for type ${takeVariable.type}")
            val exceptionClass = context.irBuiltIns.illegalArgumentExceptionSymbol.owner

            val throwExpression = irThrow(
                irCall(exceptionClass).apply {
                    arguments[0] = irString("Requested element count is less than zero.")
                }
            )
            val checkIfNegative = irIfThen(
                type = context.irBuiltIns.unitType,
                irCall(lessThanSymbol).apply {
                    arguments[0] = irGet(takeArgumentVariable)
                    arguments[1] = irInt(0)
                },
                throwExpression
            )
            val mainBodyBuilder =
                { sequenceVariable: IrValueDeclaration ->
                    irBlock {
                        +builder.increment(takeVariable)
                        when (take.takeOrDrop) {
                            TakeOrDrop.Take -> {
                                val condition = irCall(lessOrEqualSymbol).apply {
                                    arguments[0] = irGet(takeVariable)
                                    arguments[1] = irGet(takeArgumentVariable)
                                }
                                +irIfThenElse(
                                    context.irBuiltIns.booleanType,
                                    condition,
                                    sequenceReplacement.mainBodyBuilder(sequenceVariable),
                                    irFalse()
                                )
                            }
                            TakeOrDrop.Drop -> {
                                val condition = irCall(lessOrEqualSymbol).apply {
                                    arguments[0] = irGet(takeVariable)
                                    arguments[1] = irGet(takeArgumentVariable)
                                }
                                +irIfThenElse(
                                    context.irBuiltIns.booleanType,
                                    condition,
                                    irTrue(),
                                    sequenceReplacement.mainBodyBuilder(sequenceVariable),
                                )
                            }
                        }
                    }
                }
            val initialDeclarations = sequenceReplacement.initialDeclarations + takeVariable + takeArgumentVariable + checkIfNegative
            val finalExpression = sequenceReplacement.finalExpression
            return SequenceReplacement(initialDeclarations, mainBodyBuilder, finalExpression)
        }
    }
}
