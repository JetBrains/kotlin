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

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class AbstractInRangeWithKnownBoundsExpressionGenerator(
        val codegen: ExpressionCodegen,
        operatorReference: KtSimpleNameExpression,
        protected val isInclusiveHighBound: Boolean,
        protected val asmElementType: Type
) : InExpressionGenerator {
    private val isNotIn = operatorReference.getReferencedNameElementType() == KtTokens.NOT_IN

    protected abstract fun genLowBound(): StackValue
    protected abstract fun genHighBound(): StackValue

    protected interface ComparisonGenerator {
        fun jumpIfGreaterOrEqual(v: InstructionAdapter, label: Label)
        fun jumpIfLessOrEqual(v: InstructionAdapter, label: Label)
        fun jumpIfGreater(v: InstructionAdapter, label: Label)
        fun jumpIfLess(v: InstructionAdapter, label: Label)
    }

    protected abstract val comparisonGenerator: ComparisonGenerator

    override fun generate(argument: StackValue): BranchedValue =
            gen(argument).let { if (isNotIn) Invert(it) else it }

    private fun gen(argument: StackValue): BranchedValue =
            object : BranchedValue(argument, null, asmElementType, Opcodes.IFEQ) {
                override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                    if (jumpIfFalse) {
                        // arg1 in low .. high
                        // arg1 >= low && arg1 <[=] high
                        val cmpHighLabel = Label()

                        arg1.put(operandType, v)
                        AsmUtil.dup(v, operandType)

                        genLowBound().put(operandType, v)
                        comparisonGenerator.jumpIfGreaterOrEqual(v, cmpHighLabel)

                        AsmUtil.pop(v, operandType)
                        v.goTo(jumpLabel)

                        v.mark(cmpHighLabel)
                        genHighBound().put(operandType, v)
                        if (isInclusiveHighBound) {
                            comparisonGenerator.jumpIfGreater(v, jumpLabel)
                        }
                        else {
                            comparisonGenerator.jumpIfGreaterOrEqual(v, jumpLabel)
                        }
                    }
                    else {
                        // arg1 !in low .. high
                        // arg1 < low || arg1 >[=] high
                        val trueLabel1 = Label()
                        val trueLabel2 = Label()

                        arg1.put(operandType, v)
                        AsmUtil.dup(v, operandType)

                        genLowBound().put(operandType, v)
                        comparisonGenerator.jumpIfLess(v, trueLabel1)

                        genHighBound().put(operandType, v)
                        if (isInclusiveHighBound) {
                            comparisonGenerator.jumpIfLessOrEqual(v, jumpLabel)
                        }
                        else {
                            comparisonGenerator.jumpIfLess(v, jumpLabel)
                        }
                        v.goTo(trueLabel2)

                        v.mark(trueLabel1)
                        AsmUtil.pop(v, operandType)

                        v.mark(trueLabel2)
                    }
                }
            }
}