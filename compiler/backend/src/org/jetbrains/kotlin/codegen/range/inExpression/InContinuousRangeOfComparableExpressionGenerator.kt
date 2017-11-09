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
import org.jetbrains.kotlin.codegen.range.comparison.ObjectComparisonGenerator
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class InContinuousRangeOfComparableExpressionGenerator(
        operatorReference: KtSimpleNameExpression,
        private val boundedValue: BoundedValue
) : InExpressionGenerator {
    private val isNotIn = operatorReference.getReferencedNameElementType() == KtTokens.NOT_IN
    private val comparisonGenerator = ObjectComparisonGenerator

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
                        // arg < low
                        v.swap()
                        comparisonGenerator.jumpIfLess(v, exitLabel1)
                    }
                    else {
                        // arg <= low
                        v.swap()
                        comparisonGenerator.jumpIfLessOrEqual(v, exitLabel1)
                    }

                    // On stack: high arg
                    // if (high bound is satisfied) goto jumpLabel
                    if (boundedValue.isHighInclusive) {
                        // arg <= high
                        v.swap()
                        comparisonGenerator.jumpIfLessOrEqual(v, jumpLabel)
                    }
                    else {
                        // arg < high
                        v.swap()
                        comparisonGenerator.jumpIfLess(v, jumpLabel)
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
                        // arg >= low
                        v.swap()
                        comparisonGenerator.jumpIfGreaterOrEqual(v, cmpHighLabel)
                    }
                    else {
                        // arg > low
                        v.swap()
                        comparisonGenerator.jumpIfGreater(v, cmpHighLabel)
                    }

                    // Low bound is NOT satisfied, clear stack and goto jumpLabel
                    AsmUtil.pop2(v, operandType)
                    v.goTo(jumpLabel)

                    v.mark(cmpHighLabel)
                    // On stack: high arg
                    // if ([high bound is NOT satisfied]) goto jumpLabel
                    if (boundedValue.isHighInclusive) {
                        // arg > high
                        v.swap()
                        comparisonGenerator.jumpIfGreater(v, jumpLabel)
                    }
                    else {
                        // arg >= high
                        v.swap()
                        comparisonGenerator.jumpIfGreaterOrEqual(v, jumpLabel)
                    }
                }
            }
}