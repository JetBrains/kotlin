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
import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
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

class RedundantBoxingMethodTransformer(private val generationState: GenerationState) : MethodTransformer() {

    override fun transform(internalClassName: String, node: MethodNode) {
        if (node.instructions.none { it.isBoxing(generationState) || it.isMethodInsnWith(Opcodes.INVOKEINTERFACE) { name == "next" } })
            return

        val interpreter = RedundantBoxingInterpreter(node, generationState)
        val analyzer = FastMethodAnalyzer<BasicValue>(
            internalClassName, node, interpreter, pruneExceptionEdges = false
        ) { nLocals, nStack -> BoxingFrame(nLocals, nStack, interpreter) }
        val frames = analyzer.analyze()

        interpretPopInstructionsForBoxedValues(interpreter, node, frames)

        val valuesToOptimize = interpreter.candidatesBoxedValues

        if (!valuesToOptimize.isEmpty) {
            // has side effect on valuesToOptimize
            removeValuesFromTaintedProgressionIterators(valuesToOptimize)

            // has side effect on valuesToOptimize and frames, containing BoxedBasicValues that are unsafe to remove
            removeValuesClashingWithVariables(valuesToOptimize, node, frames)

            // cannot replace them inplace because replaced variables indexes are known after remapping
            val variablesForReplacement = adaptLocalSingleVariableTableForBoxedValuesAndPrepareMultiVariables(node, frames)

            node.remapLocalVariables(buildVariablesRemapping(valuesToOptimize, node))

            replaceVariables(node, variablesForReplacement)

            sortAdaptableInstructionsForBoxedValues(node, valuesToOptimize)

            adaptInstructionsForBoxedValues(node, valuesToOptimize)
        }
    }

    private fun sortAdaptableInstructionsForBoxedValues(node: MethodNode, valuesToOptimize: RedundantBoxedValuesCollection) {
        val indexes = node.instructions.withIndex().associate { (index, insn) -> insn to index }
        for (value in valuesToOptimize) {
            value.sortAssociatedInsns(indexes)
            value.sortUnboxingWithCastInsns(indexes)
        }
    }

    private fun replaceVariables(node: MethodNode, variablesForReplacement: Map<LocalVariableNode, List<LocalVariableNode>>) {
        if (variablesForReplacement.isEmpty()) return
        node.localVariables = node.localVariables.flatMap { oldVar ->
            variablesForReplacement[oldVar]?.also { newVars -> for (newVar in newVars) newVar.index += oldVar.index } ?: listOf(oldVar)
        }.toMutableList()
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
        frames: Array<Frame<BasicValue>?>
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
            if (isUnsafeToRemoveBoxingForConnectedValues(variableValues, firstBoxed.unboxedTypes)) {
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

    private fun removeValuesFromTaintedProgressionIterators(valuesToOptimize: RedundantBoxedValuesCollection) {
        for (descriptor in valuesToOptimize.toList()) {
            val progressionIterator = descriptor?.progressionIterator ?: continue
            if (progressionIterator.tainted) {
                valuesToOptimize.remove(descriptor)
            }
        }
    }

    private fun isUnsafeToRemoveBoxingForConnectedValues(usedValues: List<BasicValue>, unboxedTypes: List<Type>): Boolean =
        usedValues.any { input ->
            if (input === StrictBasicValue.UNINITIALIZED_VALUE) return@any false
            if (input !is CleanBoxedValue) return@any true

            val descriptor = input.descriptor
            !descriptor.isSafeToRemove || descriptor.unboxedTypes != unboxedTypes
        }

    private fun adaptLocalSingleVariableTableForBoxedValuesAndPrepareMultiVariables(
        node: MethodNode, frames: Array<Frame<BasicValue>?>
    ): Map<LocalVariableNode, List<LocalVariableNode>> {
        val localVariablesReplacement = mutableMapOf<LocalVariableNode, List<LocalVariableNode>>()
        for (localVariableNode in node.localVariables) {
            if (Type.getType(localVariableNode.desc).sort != Type.OBJECT) {
                continue
            }

            for (value in getValuesStoredOrLoadedToVariable(localVariableNode, node, frames)) {
                if (value !is BoxedBasicValue) continue

                val descriptor = value.descriptor
                if (!descriptor.isSafeToRemove) continue
                val unboxedType = descriptor.unboxedTypes.singleOrNull()
                if (unboxedType == null) {
                    var offset = 0
                    localVariablesReplacement[localVariableNode] =
                        descriptor.multiFieldValueClassUnboxInfo!!.unboxedTypesAndMethodNamesAndFieldNames.map { (type, _, fieldName) ->
                            val newVarName = "${localVariableNode.name}-$fieldName"
                            val newStart = localVariableNode.start
                            val newEnd = localVariableNode.end
                            val newOffset = offset
                            offset += type.size
                            LocalVariableNode(newVarName, type.descriptor, null, newStart, newEnd, newOffset)
                        }
                } else {
                    localVariableNode.desc = unboxedType.descriptor
                }
            }
        }
        return localVariablesReplacement
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
                (insn as VarInsnNode).`var` == localVariableNode.index
            ) {
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
        val wideVars2SizeMinusOne = HashMap<Int, Int>()
        for (valueDescriptor in values) {
            val size = valueDescriptor.getTotalUnboxSize()
            if (size < 2) continue
            for (index in valueDescriptor.getVariablesIndexes()) {
                wideVars2SizeMinusOne.merge(index, size - 1, ::maxOf)
            }
        }

        node.maxLocals += wideVars2SizeMinusOne.values.sum()
        val remapping = IntArray(node.maxLocals)
        for (i in remapping.indices) {
            remapping[i] = i
        }

        for ((varIndex, shift) in wideVars2SizeMinusOne) {
            for (i in varIndex + 1..remapping.lastIndex) {
                remapping[i] += shift
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

        var extraSlotsUsed = 0
        for (insn in value.getAssociatedInsns()) {
            extraSlotsUsed = maxOf(extraSlotsUsed, adaptInstruction(node, insn, value))
        }
        node.maxLocals += extraSlotsUsed
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
        val castInsnsListener = MethodNode(Opcodes.API_VERSION)
        InstructionAdapter(castInsnsListener)
            .cast(value.getUnboxTypeOrOtherwiseMethodReturnType(castInsn as? MethodInsnNode), castWithType.getSecond())


        for (insn in castInsnsListener.instructions.toArray()) {
            node.instructions.insertBefore(castInsn, insn)
        }

        node.instructions.remove(castInsn)
    }

    private fun adaptInstruction(
        node: MethodNode, insn: AbstractInsnNode, value: BoxedValueDescriptor
    ): Int {
        var usedExtraSlots = 0

        when (insn.opcode) {
            Opcodes.POP -> {
                val newPops = makePops(value.unboxedTypes)
                node.instructions.insert(insn, newPops)
                node.instructions.remove(insn)
            }

            Opcodes.DUP -> when (value.getTotalUnboxSize()) {
                1 -> Unit
                2 -> node.instructions.set(insn, InsnNode(Opcodes.DUP2))
                else -> {
                    usedExtraSlots = value.getTotalUnboxSize()
                    var currentSlot = node.maxLocals
                    val slotIndices = value.unboxedTypes.map { type -> currentSlot.also { currentSlot += type.size } }
                    for ((type, index) in (value.unboxedTypes zip slotIndices).asReversed()) {
                        node.instructions.insertBefore(insn, VarInsnNode(type.getOpcode(Opcodes.ISTORE), index))
                    }
                    repeat(2) {
                        for ((type, index) in (value.unboxedTypes zip slotIndices)) {
                            node.instructions.insertBefore(insn, VarInsnNode(type.getOpcode(Opcodes.ILOAD), index))
                        }
                    }
                    node.instructions.remove(insn)
                }
            }

            Opcodes.ASTORE, Opcodes.ALOAD -> {
                val isStore = insn.opcode == Opcodes.ASTORE
                val singleUnboxedType = value.unboxedTypes.singleOrNull()
                if (singleUnboxedType == null) {
                    val newInstructions = mutableListOf<VarInsnNode>()
                    var offset = 0
                    for (unboxedType in value.unboxedTypes) {
                        val opcode = unboxedType.getOpcode(if (isStore) Opcodes.ISTORE else Opcodes.ILOAD)
                        val newIndex = (insn as VarInsnNode).`var` + offset
                        newInstructions.add(VarInsnNode(opcode, newIndex))
                        offset += unboxedType.size
                    }
                    if (isStore) {
                        val previousInstructions = generateSequence(insn.previous) { it.previous }
                            .take(value.unboxedTypes.size).toList().asReversed()
                        if (value.unboxedTypes.map { it.getOpcode(Opcodes.ILOAD) } == previousInstructions.map { it.opcode }) {
                            // help optimizer and put each xSTORE after the corresponding xLOAD
                            for ((load, store) in previousInstructions zip newInstructions) {
                                newInstructions.remove(store)
                                node.instructions.insert(load, store)
                            }
                        } else {
                            for (newInstruction in newInstructions.asReversed()) {
                                node.instructions.insertBefore(insn, newInstruction)
                            }
                        }
                    } else {
                        for (newInstruction in newInstructions) {
                            node.instructions.insertBefore(insn, newInstruction)
                        }
                    }
                    node.instructions.remove(insn)
                } else {
                    val storeOpcode = singleUnboxedType.getOpcode(if (isStore) Opcodes.ISTORE else Opcodes.ILOAD)
                    node.instructions.set(insn, VarInsnNode(storeOpcode, (insn as VarInsnNode).`var`))
                }
            }

            Opcodes.INSTANCEOF -> {
                node.instructions.insertBefore(insn, makePops(value.unboxedTypes))
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

            Opcodes.CHECKCAST -> node.instructions.remove(insn)
            Opcodes.INVOKEVIRTUAL -> {
                if (value.unboxedTypes.size != 1) {
                    val unboxMethodCall = insn as MethodInsnNode
                    val unboxMethodIndex = value.multiFieldValueClassUnboxInfo!!.unboxedMethodNames.indexOf(unboxMethodCall.name)
                    val unboxedType = value.unboxedTypes[unboxMethodIndex]

                    var canRemoveInsns = true
                    var savedToVariable = false
                    for ((i, type) in value.unboxedTypes.withIndex().toList().asReversed()) {
                        fun canRemoveInsn(includeDup: Boolean): Boolean {
                            if (!canRemoveInsns) return false
                            val insnToCheck = if (i < unboxMethodIndex) unboxMethodCall.previous.previous else unboxMethodCall.previous
                            val result = when (insnToCheck.opcode) {
                                type.getOpcode(Opcodes.ILOAD) -> true
                                Opcodes.DUP2 -> includeDup && type.size == 2
                                Opcodes.DUP -> includeDup && type.size == 1
                                else -> false
                            }

                            canRemoveInsns = result
                            return result
                        }

                        fun insertPopInstruction() =
                            node.instructions.insertBefore(unboxMethodCall, InsnNode(if (type.size == 2) Opcodes.POP2 else Opcodes.POP))

                        fun saveToVariableIfNecessary() {
                            if (savedToVariable) return
                            if (i > unboxMethodIndex) return
                            savedToVariable = true
                            usedExtraSlots = unboxedType.size
                            node.instructions.insertBefore(insn, VarInsnNode(unboxedType.getOpcode(Opcodes.ISTORE), node.maxLocals))
                        }

                        if (i == unboxMethodIndex) {
                            if (unboxMethodIndex > 0 && !canRemoveInsn(includeDup = false)) {
                                saveToVariableIfNecessary()
                            }
                        } else if (canRemoveInsn(includeDup = i > unboxMethodIndex)) {
                            node.instructions.remove(if (i < unboxMethodIndex) unboxMethodCall.previous.previous else unboxMethodCall.previous)
                        } else {
                            saveToVariableIfNecessary()
                            insertPopInstruction()
                        }
                    }
                    if (savedToVariable) {
                        node.instructions.insertBefore(insn, VarInsnNode(unboxedType.getOpcode(Opcodes.ILOAD), node.maxLocals))
                    }
                }
                node.instructions.remove(insn)
            }

            else ->
                throwCannotAdaptInstruction(insn)
        }
        return usedExtraSlots
    }

    private fun throwCannotAdaptInstruction(insn: AbstractInsnNode): Nothing =
        throw AssertionError("Cannot adapt instruction: ${insn.insnText}")

    private fun adaptAreEqualIntrinsic(
        node: MethodNode,
        insn: AbstractInsnNode,
        value: BoxedValueDescriptor
    ) {
        val unboxedType = value.unboxedTypes.singleOrNull()

        when (unboxedType?.sort) {
            Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.INT, Type.CHAR ->
                adaptAreEqualIntrinsicForInt(node, insn)
            Type.LONG ->
                adaptAreEqualIntrinsicForLong(node, insn)
            Type.OBJECT, null -> {
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
        val unboxedType = value.unboxedTypes.single()

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
