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

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.BranchedValue
import org.jetbrains.kotlin.codegen.Invert
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.range.BoundedValue
import org.jetbrains.kotlin.codegen.range.comparison.ComparisonGenerator
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class InPrimitiveContinuousRangeExpressionGenerator(
        operatorReference: KtSimpleNameExpression,
        private val boundedValue: BoundedValue,
        private val comparisonGenerator: ComparisonGenerator
) : InExpressionGenerator {
    private val isNotIn = operatorReference.getReferencedNameElementType() == KtTokens.NOT_IN

    override fun generate(argument: StackValue): BranchedValue =
            gen(argument).let { if (isNotIn) Invert(it) else it }

    private fun gen(argument: StackValue): BranchedValue =
            object : BranchedValue(argument, null, comparisonGenerator.comparedType, Opcodes.IFEQ) {
                override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                    if (jumpIfFalse) {
                        genJumpIfFalse(v, jumpLabel)
                    }
                    else {
                        genJumpIfTrue(v, jumpLabel)
                    }
                }

                private fun genJumpIfTrue(v: InstructionAdapter, jumpLabel: Label) {
                    // if (arg is in range) goto jumpLabel

                    val exitLabel1 = Label()
                    val exitLabel2 = Label()

                    boundedValue.putHighLow(v, operandType)
                    arg1.put(operandType, v)
                    AsmUtil.dupx(v, operandType)

                    // On stack: high arg low arg
                    // if (low bound is NOT satisfied) goto exitLabel1
                    if (boundedValue.isLowInclusive) {
                        // low > arg
                        comparisonGenerator.jumpIfGreater(v, exitLabel1)
                    }
                    else {
                        // low >= arg
                        comparisonGenerator.jumpIfGreaterOrEqual(v, exitLabel1)
                    }

                    // On stack: high arg
                    // if (high bound is satisfied) goto jumpLabel
                    if (boundedValue.isHighInclusive) {
                        // high >= arg
                        comparisonGenerator.jumpIfGreaterOrEqual(v, jumpLabel)
                    }
                    else {
                        // high > arg
                        comparisonGenerator.jumpIfGreater(v, jumpLabel)
                    }
                    v.goTo(exitLabel2)

                    v.mark(exitLabel1)
                    AsmUtil.pop2(v, operandType)

                    v.mark(exitLabel2)
                }

                private fun genJumpIfFalse(v: InstructionAdapter, jumpLabel: Label) {
                    // if (arg is NOT in range) goto jumpLabel

                    val cmpHighLabel = Label()

                    boundedValue.putHighLow(v, operandType)
                    arg1.put(operandType, v)
                    AsmUtil.dupx(v, operandType)

                    // On stack: high arg low arg
                    // if ([low bound is satisfied]) goto cmpHighLabel
                    if (boundedValue.isLowInclusive) {
                        // low <= arg
                        comparisonGenerator.jumpIfLessOrEqual(v, cmpHighLabel)
                    }
                    else {
                        // low < arg
                        comparisonGenerator.jumpIfLess(v, cmpHighLabel)
                    }

                    // Low bound is NOT satisfied, clear stack and goto jumpLabel
                    AsmUtil.pop2(v, operandType)
                    v.goTo(jumpLabel)

                    v.mark(cmpHighLabel)
                    // On stack: high arg
                    // if ([high bound is NOT satisfied]) goto jumpLabel
                    if (boundedValue.isHighInclusive) {
                        // high < arg
                        comparisonGenerator.jumpIfLess(v, jumpLabel)
                    }
                    else {
                        // high <= arg
                        comparisonGenerator.jumpIfLessOrEqual(v, jumpLabel)
                    }
                }
            }
}