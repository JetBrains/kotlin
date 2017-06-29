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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class AbstractInPrimitiveNumberRangeExpressionGenerator(
        val codegen: ExpressionCodegen,
        operatorReference: KtSimpleNameExpression,
        rangeCall: ResolvedCall<*>,
        private val isInclusiveHighBound: Boolean
) : InExpressionGenerator {
    protected val asmElementType =
            AsmTypes.valueTypeForPrimitive(
                    getPrimitiveRangeElementType(rangeCall.resultingDescriptor.returnType!!)
            )

    private val isInverted =
            operatorReference.getReferencedNameElementType() == KtTokens.NOT_IN

    protected abstract fun genLowBound(): StackValue
    protected abstract fun genHighBound(): StackValue

    override fun generate(argument: StackValue): BranchedValue =
            gen(argument).let { if (isInverted) Invert(it) else it }

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
                        v.jumpIfGe(cmpHighLabel)

                        AsmUtil.pop(v, operandType)
                        v.goTo(jumpLabel)

                        v.mark(cmpHighLabel)
                        genHighBound().put(operandType, v)
                        if (isInclusiveHighBound) {
                            v.jumpIfGt(jumpLabel)
                        }
                        else {
                            v.jumpIfGe(jumpLabel)
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
                        v.jumpIfLt(trueLabel1)

                        genLowBound().put(operandType, v)
                        if (isInclusiveHighBound) {
                            v.jumpIfLe(jumpLabel)
                        }
                        else {
                            v.jumpIfLt(jumpLabel)
                        }
                        v.goTo(trueLabel2)

                        v.mark(trueLabel1)
                        AsmUtil.pop(v, operandType)

                        v.mark(trueLabel2)
                    }
                }

                private fun InstructionAdapter.jumpIfGe(label: Label) {
                    when {
                        AsmUtil.isIntPrimitive(operandType) -> ificmpge(label)

                        operandType === Type.LONG_TYPE -> {
                            lcmp()
                            ifge(label)
                        }

                        operandType === Type.FLOAT_TYPE || operandType === Type.DOUBLE_TYPE -> {
                            cmpg(operandType)
                            ifge(label)
                        }

                        else -> throw UnsupportedOperationException("Unexpected type: " + operandType)
                    }
                }

                private fun InstructionAdapter.jumpIfLe(label: Label) {
                    when {
                        AsmUtil.isIntPrimitive(operandType) -> ificmple(label)

                        operandType === Type.LONG_TYPE -> {
                            lcmp()
                            ifle(label)
                        }

                        operandType === Type.FLOAT_TYPE || operandType === Type.DOUBLE_TYPE -> {
                            cmpg(operandType)
                            ifle(label)
                        }

                        else -> throw UnsupportedOperationException("Unexpected type: " + operandType)
                    }
                }

                private fun InstructionAdapter.jumpIfGt(label: Label) {
                    when {
                        AsmUtil.isIntPrimitive(operandType) -> ificmpgt(label)

                        operandType === Type.LONG_TYPE -> {
                            lcmp()
                            ifgt(label)
                        }

                        operandType === Type.FLOAT_TYPE || operandType === Type.DOUBLE_TYPE -> {
                            cmpg(operandType)
                            ifgt(label)
                        }

                        else -> throw UnsupportedOperationException("Unexpected type: " + operandType)
                    }
                }

                private fun InstructionAdapter.jumpIfLt(label: Label) {
                    when {
                        AsmUtil.isIntPrimitive(operandType) -> ificmplt(label)

                        operandType === Type.LONG_TYPE -> {
                            lcmp()
                            iflt(label)
                        }

                        operandType === Type.FLOAT_TYPE || operandType === Type.DOUBLE_TYPE -> {
                            cmpg(operandType)
                            iflt(label)
                        }

                        else -> throw UnsupportedOperationException("Unexpected type: " + operandType)
                    }
                }
            }
}
