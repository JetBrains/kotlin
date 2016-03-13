/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.optimization.boxing;

import com.google.common.collect.ImmutableSet
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

internal class RedundantBoxingInterpreter(insnList: InsnList) : BoxingInterpreter(insnList) {

    val candidatesBoxedValues = RedundantBoxedValuesCollection()

    @Throws(AnalyzerException::class)
    override fun binaryOperation(insn: AbstractInsnNode, value1: BasicValue, value2: BasicValue): BasicValue? {
        processOperationWithBoxedValue(value1, insn)
        processOperationWithBoxedValue(value2, insn)

        return super.binaryOperation(insn, value1, value2)
    }

    @Throws(AnalyzerException::class)
    override fun ternaryOperation(insn: AbstractInsnNode, value1: BasicValue, value2: BasicValue, value3: BasicValue): BasicValue? {
        // in a valid code only aastore could happen with boxed value
        processOperationWithBoxedValue(value3, insn)

        return super.ternaryOperation(insn, value1, value2, value3)
    }

    @Throws(AnalyzerException::class)
    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? {
        if ((insn.opcode == Opcodes.CHECKCAST || insn.opcode == Opcodes.INSTANCEOF) && value is BoxedBasicValue) {
            val typeInsn = insn as TypeInsnNode

            if (!isSafeCast(value, typeInsn.desc)) {
                markValueAsDirty(value)
            }
        }

        processOperationWithBoxedValue(value, insn)

        return super.unaryOperation(insn, value)
    }

    @Throws(AnalyzerException::class)
    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue {
        if (value is BoxedBasicValue && insn.opcode === Opcodes.ASTORE) {
            value.addVariableIndex((insn as VarInsnNode).`var`)
        }

        processOperationWithBoxedValue(value, insn)

        return super.copyOperation(insn, value)
    }

    fun processPopInstruction(insnNode: AbstractInsnNode, value: BasicValue) {
        processOperationWithBoxedValue(value, insnNode)
    }

    override fun onNewBoxedValue(value: BoxedBasicValue) {
        candidatesBoxedValues.add(value)
    }

    override fun onUnboxing(insn: AbstractInsnNode, value: BoxedBasicValue, resultType: Type) {
        if (value.primitiveType == resultType) {
            addAssociatedInsn(value, insn)
        }
        else {
            value.addUnboxingWithCastTo(insn, resultType)
        }
    }

    override fun onMethodCallWithBoxedValue(value: BoxedBasicValue) {
        markValueAsDirty(value)
    }

    override fun onMergeFail(value: BoxedBasicValue) {
        markValueAsDirty(value)
    }

    override fun onMergeSuccess(v: BoxedBasicValue, w: BoxedBasicValue) {
        candidatesBoxedValues.merge(v, w)
    }

    private fun processOperationWithBoxedValue(value: BasicValue?, insnNode: AbstractInsnNode) {
        if (value is BoxedBasicValue) {
            if (!PERMITTED_OPERATIONS_OPCODES.contains(insnNode.opcode)) {
                markValueAsDirty(value)
            }
            else {
                addAssociatedInsn(value, insnNode)
            }
        }
    }

    private fun markValueAsDirty(value: BoxedBasicValue) {
        candidatesBoxedValues.remove(value)
    }

    companion object {
        private val PERMITTED_OPERATIONS_OPCODES =
                ImmutableSet.of(Opcodes.ASTORE, Opcodes.ALOAD, Opcodes.POP, Opcodes.DUP, Opcodes.CHECKCAST, Opcodes.INSTANCEOF)

        private val PRIMITIVE_TYPES_SORTS_WITH_WRAPPER_EXTENDS_NUMBER =
                ImmutableSet.of(Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE)

        private fun isSafeCast(value: BoxedBasicValue, targetInternalName: String) =
                when (targetInternalName) {
                    Type.getInternalName(Any::class.java) ->
                        true
                    Type.getInternalName(Number::class.java) -> {
                        PRIMITIVE_TYPES_SORTS_WITH_WRAPPER_EXTENDS_NUMBER.contains(
                                value.primitiveType.sort)
                    }
                    else ->
                        value.type.internalName.equals(targetInternalName)
                }

        private fun addAssociatedInsn(value: BoxedBasicValue, insn: AbstractInsnNode) {
            if (value.isSafeToRemove) {
                value.addInsn(insn)
            }
        }
    }
}