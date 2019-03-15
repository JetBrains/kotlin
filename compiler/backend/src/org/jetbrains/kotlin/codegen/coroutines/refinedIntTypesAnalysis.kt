/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.optimization.common.*
import org.jetbrains.kotlin.codegen.optimization.fixStack.peek
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue
import java.util.*

// BasicValue interpreter from ASM does not distinct 'int' types from other int-like types like 'byte' or 'boolean',
// neither do HotSpot and JVM spec.
// But it seems like Dalvik does not follow it, and spilling boolean value into an 'int' field fails with VerifyError on Android 4,
// so this function calculates refined frames' markup.
// Note that type of some values is only possible to determine by their usages (e.g. ICONST_1, BALOAD both may push boolean or byte on stack)
internal fun performRefinedTypeAnalysis(methodNode: MethodNode, thisName: String): Array<out Frame<out BasicValue>?> {
    val insnList = methodNode.instructions
    val basicFrames = MethodTransformer.analyze(thisName, methodNode, OptimizationBasicInterpreter())
    val sourceValueFrames = MethodTransformer.analyze(thisName, methodNode, MySourceInterpreter())

    val expectedTypeAndSourcesByInsnIndex: Array<Pair<Type, List<SourceValue>>?> = arrayOfNulls(insnList.size())

    fun AbstractInsnNode.index() = insnList.indexOf(this)

    fun saveExpectedType(value: SourceValue?, expectedType: Type) {
        if (value == null) return
        if (expectedType.sort !in REFINED_INT_SORTS) return

        value.insns.forEach {
            val index = insnList.indexOf(it)

            checkUpdatedExpectedType(expectedTypeAndSourcesByInsnIndex[index]?.first, expectedType)

            expectedTypeAndSourcesByInsnIndex[index] =
                    Pair(expectedType,
                         expectedTypeAndSourcesByInsnIndex[index]?.second.orEmpty() + value)
        }
    }

    fun saveExpectedTypeForArrayStore(insn: AbstractInsnNode, sourceValueFrame: Frame<SourceValue>) {
        val arrayStoreType =
                when (insn.opcode) {
                    Opcodes.BASTORE -> Type.BYTE_TYPE
                    Opcodes.CASTORE -> Type.CHAR_TYPE
                    Opcodes.SASTORE -> Type.SHORT_TYPE
                    else -> return
                }

        val insnIndex = insnList.indexOf(insn)

        val arrayArg = basicFrames[insnIndex].peek(2)
        // may be different from 'arrayStoreType' in case of boolean arrays (BASTORE opcode is also used for them)
        val expectedType =
                if (arrayArg?.type?.sort == Type.ARRAY)
                    arrayArg.type.elementType
                else
                    arrayStoreType

        saveExpectedType(sourceValueFrame.top(), expectedType)
    }

    fun saveExpectedTypeForFieldOrMethod(insn: AbstractInsnNode, sourceValueFrame: Frame<SourceValue>) {
        when (insn.opcode) {
            Opcodes.PUTFIELD, Opcodes.PUTSTATIC ->
                saveExpectedType(sourceValueFrame.top(), Type.getType((insn as FieldInsnNode).desc))

            Opcodes.INVOKESTATIC, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE, Opcodes.INVOKESPECIAL -> {
                val argumentTypes = Type.getArgumentTypes((insn as MethodInsnNode).desc)
                argumentTypes.withIndex().forEach {
                    val (argIndex, type) = it
                    saveExpectedType(sourceValueFrame.peek(argumentTypes.size - argIndex - 1), type)
                }
            }
        }
    }

    fun saveExpectedTypeForVarStore(insn: AbstractInsnNode, sourceValueFrame: Frame<SourceValue>) {
        if (insn.isIntStore()) {
            val varIndex = (insn as VarInsnNode).`var`
            // Considering next insn is important because variable initializer is emitted just before
            // the beginning of variable
            val nextInsn = InsnSequence(insn.next, insnList.last).firstOrNull(AbstractInsnNode::isMeaningful)

            val variableNode =
                    methodNode.findContainingVariableFromTable(insn, varIndex)
                    ?: methodNode.findContainingVariableFromTable(nextInsn ?: return, varIndex)
                    ?: return

            saveExpectedType(sourceValueFrame.top(), Type.getType(variableNode.desc))
        }
    }

    for ((insnIndex, insn) in insnList.toArray().withIndex()) {
         assert(insn.opcode != Opcodes.IRETURN) {
            "Coroutine body must not contain IRETURN instructions because 'doResume' is always void"
        }

        val sourceValueFrame = sourceValueFrames[insnIndex] ?: continue

        saveExpectedTypeForArrayStore(insn, sourceValueFrame)
        saveExpectedTypeForFieldOrMethod(insn, sourceValueFrame)
        saveExpectedTypeForVarStore(insn, sourceValueFrame)
    }

    val refinedVarFrames = analyze(methodNode, object : BackwardAnalysisInterpreter<VarExpectedTypeFrame> {
        override fun newFrame(maxLocals: Int): VarExpectedTypeFrame = VarExpectedTypeFrame(maxLocals)

        override fun def(frame: VarExpectedTypeFrame, insn: AbstractInsnNode) {
            if (insn.isIntStore()) {
                frame.expectedTypeByVarIndex[(insn as VarInsnNode).`var`] = null
            }
        }

        override fun use(frame: VarExpectedTypeFrame, insn: AbstractInsnNode) {
            val (expectedType, sources) = expectedTypeAndSourcesByInsnIndex[insn.index()] ?: return

            sources.flatMap(SourceValue::insns).forEach {
                insn ->
                if (insn.isIntLoad()) {
                    frame.updateExpectedType((insn as VarInsnNode).`var`, expectedType)
                }
            }
        }
    })

    return Array(basicFrames.size) {
        insnIndex ->
        val current = Frame(basicFrames[insnIndex] ?: return@Array null)

        refinedVarFrames[insnIndex].expectedTypeByVarIndex.withIndex().filter { it.value != null }.forEach {
            assert(current.getLocal(it.index)?.type?.sort in ALL_INT_SORTS) {
                "int type expected, but ${current.getLocal(it.index)?.type} was found in basic frames"
            }

            current.setLocal(it.index, StrictBasicValue(it.value))
        }

        current
    }
}

private fun AbstractInsnNode.isIntLoad() = opcode == Opcodes.ILOAD
private fun AbstractInsnNode.isIntStore() = opcode == Opcodes.ISTORE

private fun checkUpdatedExpectedType(was: Type?, new: Type) {
    assert(was == null || was == new) {
        "Conflicting expected types: $was/$new"
    }
}

private class MySourceInterpreter : SourceInterpreter(Opcodes.API_VERSION) {
    override fun copyOperation(insn: AbstractInsnNode, value: SourceValue) =
            when {
                insn.isStoreOperation() || insn.isLoadOperation() -> SourceValue(value.size, insn)
                // For DUP* instructions return the same value (effectively ignore DUP's)
                else -> value
            }
}

private val REFINED_INT_SORTS = setOf(Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT)
private val ALL_INT_SORTS = REFINED_INT_SORTS + Type.INT

private fun MethodNode.findContainingVariableFromTable(insn: AbstractInsnNode, varIndex: Int): LocalVariableNode? {
    val insnIndex = instructions.indexOf(insn)
    return localVariables.firstOrNull {
        it.index == varIndex && it.rangeContainsInsn(insnIndex, instructions)
    }
}

private fun LocalVariableNode.rangeContainsInsn(insnIndex: Int, insnList: InsnList) =
        insnList.indexOf(start) < insnIndex && insnIndex < insnList.indexOf(end)

private class VarExpectedTypeFrame(maxLocals: Int) : VarFrame<VarExpectedTypeFrame> {
    val expectedTypeByVarIndex = arrayOfNulls<Type>(maxLocals)

    override fun mergeFrom(other: VarExpectedTypeFrame) {
        assert(expectedTypeByVarIndex.size == other.expectedTypeByVarIndex.size) {
            "Other VarExpectedTypeFrame has different size: ${expectedTypeByVarIndex.size} / ${other.expectedTypeByVarIndex.size}"
        }

        for ((varIndex, type) in other.expectedTypeByVarIndex.withIndex()) {
            updateExpectedType(varIndex, type ?: continue)
        }
    }

    fun updateExpectedType(varIndex: Int, new: Type) {
        val was = expectedTypeByVarIndex[varIndex]
        // Widening to int is always allowed
        if (new == Type.INT_TYPE) return

        checkUpdatedExpectedType(was, new)

        expectedTypeByVarIndex[varIndex] = new
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false

        other as VarExpectedTypeFrame

        if (!Arrays.equals(expectedTypeByVarIndex, other.expectedTypeByVarIndex)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(expectedTypeByVarIndex)
    }
}
