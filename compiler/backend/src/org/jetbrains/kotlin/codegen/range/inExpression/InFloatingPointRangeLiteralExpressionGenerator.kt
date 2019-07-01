/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.range.inExpression

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.range.BoundedValue
import org.jetbrains.kotlin.codegen.range.comparison.ComparisonGenerator
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class InFloatingPointRangeLiteralExpressionGenerator(
    operatorReference: KtSimpleNameExpression,
    private val rangeLiteral: BoundedValue,
    private val comparisonGenerator: ComparisonGenerator,
    private val frameMap: FrameMap
) : InExpressionGenerator {
    init {
        assert(rangeLiteral.isLowInclusive && rangeLiteral.isHighInclusive) { "Floating point range literal bounds should be inclusive" }
    }

    private val isNotIn = operatorReference.getReferencedNameElementType() == KtTokens.NOT_IN

    override fun generate(argument: StackValue): BranchedValue =
        gen(argument).let { if (isNotIn) Invert(it) else it }

    private fun gen(argument: StackValue): BranchedValue =
        object : BranchedValue(argument, null, comparisonGenerator.comparedType, Opcodes.IFEQ) {
            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                if (jumpIfFalse) {
                    genJumpIfFalse(v, jumpLabel)
                } else {
                    genJumpIfTrue(v, jumpLabel)
                }
            }

            private fun genJumpIfTrue(v: InstructionAdapter, jumpLabel: Label) {
                //  if (arg is in range) goto jumpLabel
                // =>
                //      if (arg is NOT in range) goto exitLabel
                //      goto jumpLabel
                //  exitLabel:

                frameMap.useTmpVar(operandType) { _ ->
                    val exitLabel = Label()
                    genJumpIfFalse(v, exitLabel)
                    v.goTo(jumpLabel)
                    v.mark(exitLabel)
                }
            }

            private fun genJumpIfFalse(v: InstructionAdapter, jumpLabel: Label) {
                // if (arg is NOT in range) goto jumpLabel

                frameMap.useTmpVar(operandType) { argVar ->
                    // Evaluate low and high bounds once (unless they have no side effects)
                    val (lowValue, lowTmpType) = introduceTemporaryIfRequired(v, rangeLiteral.lowBound, operandType)
                    val (highValue, highTmpType) = introduceTemporaryIfRequired(v, rangeLiteral.highBound, operandType)

                    val argValue = StackValue.local(argVar, operandType)
                    argValue.store(arg1, v)

                    // if (low bound is NOT satisfied) goto jumpLabel
                    // arg < low
                    argValue.put(operandType, v)
                    lowValue.put(operandType, v)
                    comparisonGenerator.jumpIfLess(v, jumpLabel)

                    // if (high bound is NOT satisfied) goto jumpLabel
                    // arg > high
                    argValue.put(operandType, v)
                    highValue.put(operandType, v)
                    comparisonGenerator.jumpIfGreater(v, jumpLabel)

                    highTmpType?.let { frameMap.leaveTemp(it) }
                    lowTmpType?.let { frameMap.leaveTemp(it) }
                }

            }

            // TODO evaluateOnce
            private fun introduceTemporaryIfRequired(v: InstructionAdapter, value: StackValue, type: Type): Pair<StackValue, Type?> {
                val resultValue: StackValue
                val resultType: Type?

                if (value.canHaveSideEffects()) {
                    val index = frameMap.enterTemp(type)
                    resultValue = StackValue.local(index, type)
                    resultType = type
                    resultValue.store(value, v)
                } else {
                    resultValue = value
                    resultType = null
                }

                return Pair(resultValue, resultType)
            }
        }
}