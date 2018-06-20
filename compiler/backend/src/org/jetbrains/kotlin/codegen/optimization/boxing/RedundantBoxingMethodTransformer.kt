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

package org.jetbrains.kotlin.codegen.optimization.boxing

import com.intellij.openapi.util.Pair
import org.jetbrains.kotlin.codegen.inline.insnOpcodeText
import org.jetbrains.kotlin.codegen.inline.insnText
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.codegen.optimization.common.remapLocalVariables
import org.jetbrains.kotlin.codegen.optimization.fixStack.peek
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import java.util.*

class RedundantBoxingMethodTransformer(private val generationState: GenerationState) : MethodTransformer() {

    override fun transform(internalClassName: String, node: MethodNode) {
        val interpreter = RedundantBoxingInterpreter(node.instructions, generationState)
        val frames = MethodTransformer.analyze(internalClassName, node, interpreter)

        interpretPopInstructionsForBoxedValues(interpreter, node, frames)

        val valuesToOptimize = interpreter.candidatesBoxedValues

        if (!valuesToOptimize.isEmpty) {
            // has side effect on valuesToOptimize and frames, containing BoxedBasicValues that are unsafe to remove
            removeValuesClashingWithVariables(valuesToOptimize, node, frames)

            adaptLocalVariableTableForBoxedValues(node, frames)

            node.remapLocalVariables(buildVariablesRemapping(valuesToOptimize, node))

            adaptInstructionsForBoxedValues(node, valuesToOptimize)
        }
    }

    private fun interpretPopInstructionsForBoxedValues(
        interpreter: RedundantBoxingInterpreter,
        node: MethodNode,
        frames: Array<out Frame<BasicValue>?>
    ) {
        for (i in frames.indices) {
            val insn = node.instructions[i]
            if (insn.opcode != Opcodes.POP && insn.opcode != Opcodes.POP2) {
                continue
            }

            val frame = frames[i] ?: continue

            val top = frame.top()!!
            interpreter.processPopInstruction(insn, top)

            if (top.size == 1 && insn.opcode == Opcodes.POP2) {
                interpreter.processPopInstruction(insn, frame.peek(1)!!)
            }
        }
    }

    private fun removeValuesClashingWithVariables(
        values: RedundantBoxedValuesCollection,
        node: MethodNode,
        frames: Array<Frame<BasicValue>>
    ) {
        while (removeValuesClashingWithVariablesPass(values, node, frames)) {
            // do nothing
        }
    }

    private fun removeValuesClashingWithVariablesPass(
        values: RedundantBoxedValuesCollection,
        node: MethodNode,
        frames: Array<out Frame<BasicValue>?>
    ): Boolean {
        var needToRepeat = false

        for (localVariableNode in node.localVariables) {
            if (Type.getType(localVariableNode.desc).sort != Type.OBJECT) {
                continue
            }

            val variableValues = getValuesStoredOrLoadedToVariable(localVariableNode, node, frames)

            val boxed = variableValues.filterIsInstance<BoxedBasicValue>()

            if (boxed.isEmpty()) continue

            val firstBoxed = boxed.first().descriptor
            if (isUnsafeToRemoveBoxingForConnectedValues(variableValues, firstBoxed.unboxedType)) {
                for (value in boxed) {
                    val descriptor = value.descriptor
                    if (descriptor.isSafeToRemove) {
                        values.remove(descriptor)
                        needToRepeat = true
                    }
                }
            }
        }

        return needToRepeat
    }

    private fun isUnsafeToRemoveBoxingForConnectedValues(usedValues: List<BasicValue>, unboxedType: Type): Boolean =
        usedValues.any { input ->
            if (input === StrictBasicValue.UNINITIALIZED_VALUE) return@any false
            if (input !is BoxedBasicValue) return@any true

            val descriptor = input.descriptor
            !descriptor.isSafeToRemove || descriptor.unboxedType != unboxedType
        }

    private fun adaptLocalVariableTableForBoxedValues(node: MethodNode, frames: Array<Frame<BasicValue>>) {
        for (localVariableNode in node.localVariables) {
            if (Type.getType(localVariableNode.desc).sort != Type.OBJECT) {
                continue
            }

            for (value in getValuesStoredOrLoadedToVariable(localVariableNode, node, frames)) {
                if (value !is BoxedBasicValue) continue

                val descriptor = value.descriptor
                if (!descriptor.isSafeToRemove) continue
                localVariableNode.desc = descriptor.unboxedType.descriptor
            }
        }
    }

    private fun getValuesStoredOrLoadedToVariable(
        localVariableNode: LocalVariableNode,
        node: MethodNode,
        frames: Array<out Frame<BasicValue>?>
    ): List<BasicValue> {
        val values = ArrayList<BasicValue>()
        val insnList = node.instructions
        val localVariableStart = insnList.indexOf(localVariableNode.start)
        val localVariableEnd = insnList.indexOf(localVariableNode.end)

        frames[localVariableStart]?.let { frameForStartInsn ->
            frameForStartInsn.getLocal(localVariableNode.index)?.let { localVarValue ->
                values.add(localVarValue)
            }
        }

        for (i in localVariableStart until localVariableEnd) {
            if (i < 0 || i >= insnList.size()) continue
            val frame = frames[i] ?: continue
            val insn = insnList[i]
            if ((insn.opcode == Opcodes.ASTORE || insn.opcode == Opcodes.ALOAD) &&
                (insn as VarInsnNode).`var` == localVariableNode.index) {
                if (insn.getOpcode() == Opcodes.ASTORE) {
                    values.add(frame.top()!!)
                } else {
                    values.add(frame.getLocal(insn.`var`))
                }
            }
        }

        return values
    }

    private fun buildVariablesRemapping(values: RedundantBoxedValuesCollection, node: MethodNode): IntArray {
        val doubleSizedVars = HashSet<Int>()
        for (valueDescriptor in values) {
            if (valueDescriptor.isDoubleSize()) {
                doubleSizedVars.addAll(valueDescriptor.getVariablesIndexes())
            }
        }

        node.maxLocals += doubleSizedVars.size
        val remapping = IntArray(node.maxLocals)
        for (i in remapping.indices) {
            remapping[i] = i
        }

        for (varIndex in doubleSizedVars) {
            for (i in varIndex + 1..remapping.lastIndex) {
                remapping[i]++
            }
        }

        return remapping
    }

    private fun adaptInstructionsForBoxedValues(
        node: MethodNode,
        values: RedundantBoxedValuesCollection
    ) {
        for (value in values) {
            adaptInstructionsForBoxedValue(node, value)
        }
    }

    private fun adaptInstructionsForBoxedValue(node: MethodNode, value: BoxedValueDescriptor) {
        adaptBoxingInstruction(node, value)

        for (cast in value.getUnboxingWithCastInsns()) {
            adaptCastInstruction(node, value, cast)
        }

        for (insn in value.getAssociatedInsns()) {
            adaptInstruction(node, insn, value)
        }
    }

    private fun adaptBoxingInstruction(node: MethodNode, value: BoxedValueDescriptor) {
        if (!value.isFromProgressionIterator()) {
            node.instructions.remove(value.boxingInsn)
        } else {
            val iterator = value.progressionIterator ?: error("iterator should not be null because isFromProgressionIterator returns true")

            //add checkcast to kotlin/<T>Iterator before next() call
            node.instructions.insertBefore(value.boxingInsn, TypeInsnNode(Opcodes.CHECKCAST, iterator.type.internalName))

            //invoke concrete method (kotlin/<T>iterator.next<T>())
            node.instructions.set(
                value.boxingInsn,
                MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    iterator.type.internalName, iterator.nextMethodName, iterator.nextMethodDesc,
                    false
                )
            )
        }
    }

    private fun adaptCastInstruction(
        node: MethodNode,
        value: BoxedValueDescriptor,
        castWithType: Pair<AbstractInsnNode, Type>
    ) {
        val castInsn = castWithType.getFirst()
        val castInsnsListener = MethodNode(Opcodes.ASM5)
        InstructionAdapter(castInsnsListener).cast(value.unboxedType, castWithType.getSecond())

        for (insn in castInsnsListener.instructions.toArray()) {
            node.instructions.insertBefore(castInsn, insn)
        }

        node.instructions.remove(castInsn)
    }

    private fun adaptInstruction(
        node: MethodNode, insn: AbstractInsnNode, value: BoxedValueDescriptor
    ) {
        val isDoubleSize = value.isDoubleSize()

        when (insn.opcode) {
            Opcodes.POP ->
                if (isDoubleSize) {
                    node.instructions.set(insn, InsnNode(Opcodes.POP2))
                }

            Opcodes.DUP ->
                if (isDoubleSize) {
                    node.instructions.set(insn, InsnNode(Opcodes.DUP2))
                }

            Opcodes.ASTORE, Opcodes.ALOAD -> {
                val storeOpcode = value.unboxedType.getOpcode(if (insn.opcode == Opcodes.ASTORE) Opcodes.ISTORE else Opcodes.ILOAD)
                node.instructions.set(insn, VarInsnNode(storeOpcode, (insn as VarInsnNode).`var`))
            }

            Opcodes.INSTANCEOF -> {
                node.instructions.insertBefore(
                    insn,
                    InsnNode(if (isDoubleSize) Opcodes.POP2 else Opcodes.POP)
                )
                node.instructions.set(insn, InsnNode(Opcodes.ICONST_1))
            }

            Opcodes.INVOKESTATIC -> {
                when {
                    insn.isAreEqualIntrinsic() ->
                        adaptAreEqualIntrinsic(node, insn, value)
                    insn.isJavaLangComparableCompareTo() ->
                        adaptJavaLangComparableCompareTo(node, insn, value)
                    insn.isJavaLangClassBoxing() ||
                            insn.isJavaLangClassUnboxing() ->
                        node.instructions.remove(insn)
                    else ->
                        throwCannotAdaptInstruction(insn)
                }
            }

            Opcodes.INVOKEINTERFACE -> {
                if (insn.isJavaLangComparableCompareTo()) {
                    adaptJavaLangComparableCompareTo(node, insn, value)
                } else {
                    throwCannotAdaptInstruction(insn)
                }
            }

            Opcodes.CHECKCAST,
            Opcodes.INVOKEVIRTUAL ->
                node.instructions.remove(insn)

            else ->
                throwCannotAdaptInstruction(insn)
        }
    }

    private fun throwCannotAdaptInstruction(insn: AbstractInsnNode): Nothing =
        throw AssertionError("Cannot adapt instruction: ${insn.insnText}")

    private fun adaptAreEqualIntrinsic(
        node: MethodNode,
        insn: AbstractInsnNode,
        value: BoxedValueDescriptor
    ) {
        val unboxedType = value.unboxedType

        when (unboxedType.sort) {
            Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.INT, Type.CHAR ->
                adaptAreEqualIntrinsicForInt(node, insn)
            Type.LONG ->
                adaptAreEqualIntrinsicForLong(node, insn)
            Type.OBJECT -> {
            }
            else ->
                throw AssertionError("Unexpected unboxed type kind: $unboxedType")
        }
    }

    private fun adaptAreEqualIntrinsicForInt(node: MethodNode, insn: AbstractInsnNode) {
        node.instructions.run {
            val next = insn.next
            if (next != null && (next.opcode == Opcodes.IFEQ || next.opcode == Opcodes.IFNE)) {
                fuseAreEqualWithBranch(node, insn, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPEQ)
                remove(insn)
                remove(next)
            } else {
                ifEqual1Else0(node, insn, Opcodes.IF_ICMPNE)
                remove(insn)
            }
        }
    }

    private fun adaptAreEqualIntrinsicForLong(node: MethodNode, insn: AbstractInsnNode) {
        node.instructions.run {
            insertBefore(insn, InsnNode(Opcodes.LCMP))
            val next = insn.next
            if (next != null && (next.opcode == Opcodes.IFEQ || next.opcode == Opcodes.IFNE)) {
                fuseAreEqualWithBranch(node, insn, Opcodes.IFNE, Opcodes.IFEQ)
                remove(insn)
                remove(next)
            } else {
                ifEqual1Else0(node, insn, Opcodes.IFNE)
                remove(insn)
            }
        }
    }

    private fun fuseAreEqualWithBranch(
        node: MethodNode,
        insn: AbstractInsnNode,
        ifEqualOpcode: Int,
        ifNotEqualOpcode: Int
    ) {
        node.instructions.run {
            val next = insn.next
            assert(next is JumpInsnNode) { "JumpInsnNode expected: $next" }
            val nextLabel = (next as JumpInsnNode).label
            when {
                next.getOpcode() == Opcodes.IFEQ ->
                    insertBefore(insn, JumpInsnNode(ifEqualOpcode, nextLabel))
                next.getOpcode() == Opcodes.IFNE ->
                    insertBefore(insn, JumpInsnNode(ifNotEqualOpcode, nextLabel))
                else ->
                    throw AssertionError("IFEQ or IFNE expected: ${next.insnOpcodeText}")
            }
        }
    }

    private fun ifEqual1Else0(node: MethodNode, insn: AbstractInsnNode, ifneOpcode: Int) {
        node.instructions.run {
            val lNotEqual = LabelNode(Label())
            val lDone = LabelNode(Label())
            insertBefore(insn, JumpInsnNode(ifneOpcode, lNotEqual))
            insertBefore(insn, InsnNode(Opcodes.ICONST_1))
            insertBefore(insn, JumpInsnNode(Opcodes.GOTO, lDone))
            insertBefore(insn, lNotEqual)
            insertBefore(insn, InsnNode(Opcodes.ICONST_0))
            insertBefore(insn, lDone)
        }
    }

    private fun adaptJavaLangComparableCompareTo(
        node: MethodNode,
        insn: AbstractInsnNode,
        value: BoxedValueDescriptor
    ) {
        val unboxedType = value.unboxedType

        when (unboxedType.sort) {
            Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.INT, Type.CHAR ->
                adaptJavaLangComparableCompareToForInt(node, insn)
            Type.LONG ->
                adaptJavaLangComparableCompareToForLong(node, insn)
            Type.FLOAT ->
                adaptJavaLangComparableCompareToForFloat(node, insn)
            Type.DOUBLE ->
                adaptJavaLangComparableCompareToForDouble(node, insn)
            else ->
                throw AssertionError("Unexpected unboxed type kind: $unboxedType")
        }
    }

    private fun adaptJavaLangComparableCompareToForInt(node: MethodNode, insn: AbstractInsnNode) {
        node.instructions.run {
            val next = insn.next
            val next2 = next?.next
            when {
                next != null && next2 != null &&
                        next.opcode == Opcodes.ICONST_0 &&
                        next2.opcode >= Opcodes.IF_ICMPEQ && next2.opcode <= Opcodes.IF_ICMPLE -> {
                    // Fuse: compareTo + ICONST_0 + IF_ICMPxx -> IF_ICMPxx
                    remove(insn)
                    remove(next)
                }

                next != null &&
                        next.opcode >= Opcodes.IFEQ && next.opcode <= Opcodes.IFLE -> {
                    // Fuse: compareTo + IFxx -> IF_ICMPxx
                    val nextLabel = (next as JumpInsnNode).label
                    val ifCmpOpcode = next.opcode - Opcodes.IFEQ + Opcodes.IF_ICMPEQ
                    insertBefore(insn, JumpInsnNode(ifCmpOpcode, nextLabel))
                    remove(insn)
                    remove(next)
                }

                else -> {
                    // Can't fuse with branching instruction. Use Intrinsics#compare(int, int).
                    set(insn, MethodInsnNode(Opcodes.INVOKESTATIC, IntrinsicMethods.INTRINSICS_CLASS_NAME, "compare", "(II)I", false))
                }
            }
        }
    }

    private fun adaptJavaLangComparableCompareToForLong(node: MethodNode, insn: AbstractInsnNode) {
        node.instructions.set(insn, InsnNode(Opcodes.LCMP))
    }

    private fun adaptJavaLangComparableCompareToForFloat(node: MethodNode, insn: AbstractInsnNode) {
        node.instructions.set(insn, MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "compare", "(FF)I", false))
    }

    private fun adaptJavaLangComparableCompareToForDouble(node: MethodNode, insn: AbstractInsnNode) {
        node.instructions.set(insn, MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "compare", "(DD)I", false))
    }
}
