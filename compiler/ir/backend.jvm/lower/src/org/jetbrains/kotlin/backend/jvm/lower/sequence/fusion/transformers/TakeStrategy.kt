/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceTransformer
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.TakeType
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
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
        val builder = builderWithParent.first
        val takeVariable = builder.scope.createTemporaryVariable(
            builder.irInt(0),
            isMutable = true,
            nameHint = "takeVar"
        )
        val takeArgumentVariable = builder.scope.createTemporaryVariable(
            take.argument.deepCopyWithSymbols(builderWithParent.second),
            nameHint = "takeArgument",
            startOffset = take.startOffset,
            endOffset = take.endOffset,
        )
        val classifier = takeVariable.type.classifierOrNull
        val lessThanSymbol = builder.context.irBuiltIns.lessFunByOperandType[classifier]
            ?: error("No lessThan function found for type ${takeVariable.type}")
        val lessOrEqualSymbol = builder.context.irBuiltIns.lessOrEqualFunByOperandType[classifier]
            ?: error("No lessOrEqual function found for type ${takeVariable.type}")
        val exceptionClass = builder.context.irBuiltIns.illegalArgumentExceptionSymbol.owner

        val throwExpression = builder.irThrow(
            builder.irCall(exceptionClass).apply {
                arguments[0] = builder.irString("Requested element count is less than zero.")
            }
        )
        val checkIfNegative = builder.irIfThen(
            type = builder.context.irBuiltIns.unitType,
            builder.irCall(lessThanSymbol).apply {
                arguments[0] = builder.irGet(takeArgumentVariable)
                arguments[1] = builder.irInt(0)
            },
            throwExpression
        )
        val mainBodyBuilder =
            with(builder) {
                { sequenceVariable: IrValueDeclaration ->
                    irBlock {
                        // takeVariable++
                        +irSet(takeVariable, irCall(context.irBuiltIns.intPlusSymbol).apply {
                            dispatchReceiver = irGet(takeVariable)
                            arguments[1] = irInt(1)
                        })
                        when (take.takeType) {
                            TakeType.Take -> {
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
                            TakeType.Drop -> {
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
            }
        val initialDeclarations = sequenceReplacement.initialDeclarations + takeVariable + takeArgumentVariable + checkIfNegative
        val finalExpression = sequenceReplacement.finalExpression
        return SequenceReplacement(initialDeclarations, mainBodyBuilder, finalExpression)
    }
}
