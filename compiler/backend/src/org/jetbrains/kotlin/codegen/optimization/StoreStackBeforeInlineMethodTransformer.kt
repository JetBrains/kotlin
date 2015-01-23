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

package org.jetbrains.kotlin.codegen.optimization

import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.Type
import com.intellij.util.containers.Stack

class StoreStackBeforeInlineMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val frames = MethodTransformer.analyze(internalClassName, methodNode, OptimizationBasicInterpreter())
        if (needToProcess(methodNode, frames)) {
            process(methodNode, frames)
        }
        else {
            removeInlineMarkers(methodNode)
        }
    }
}

private fun needToProcess(node: MethodNode, frames: Array<Frame<BasicValue>?>): Boolean {
    val insns = node.instructions.toArray()
    var balance = 0
    var isThereAnyInlineMarker = false

    for ((insn, frame) in insns.zip(frames)) {
        if (isInlineMarker(insn)) {
            isThereAnyInlineMarker = true

            // inline marker is not available
            if (frame == null) return false
        }

        if (isBeforeInlineMarker(insn)) {
            balance++
        }
        else if(isAfterInlineMarker(insn)) {
            balance--
        }

        if (balance < 0) return false
    }

    return balance == 0 && isThereAnyInlineMarker
}

private fun isBeforeInlineMarker(insn: AbstractInsnNode) = isInlineMarker(insn, InlineCodegenUtil.INLINE_MARKER_BEFORE_METHOD_NAME)

private fun isAfterInlineMarker(insn: AbstractInsnNode) = isInlineMarker(insn, InlineCodegenUtil.INLINE_MARKER_AFTER_METHOD_NAME)

private fun isInlineMarker(insn: AbstractInsnNode, markerName: String? = null): Boolean {
    return insn.getOpcode() == Opcodes.INVOKESTATIC &&
           insn is MethodInsnNode &&
           insn.owner == InlineCodegenUtil.INLINE_MARKER_CLASS_NAME &&
           if (markerName != null) markerName == insn.name else (
               insn.name == InlineCodegenUtil.INLINE_MARKER_BEFORE_METHOD_NAME ||
               insn.name == InlineCodegenUtil.INLINE_MARKER_AFTER_METHOD_NAME
           )
}

private fun process(methodNode: MethodNode, frames: Array<Frame<BasicValue>?>) {
    val insns = methodNode.instructions.toArray()

    val storedValuesDescriptorsStack = Stack<StoredStackValuesDescriptor>()
    var firstAvailableVarIndex = methodNode.maxLocals
    var currentStoredValuesCount = 0

    for ((insn, frame) in insns.zip(frames)) {
        if (isBeforeInlineMarker(insn)) {
            frame ?: throw AssertionError("process method shouldn't be called if frame is null before inline marker")

            val desc = storeStackValues(methodNode, frame, insn, firstAvailableVarIndex, currentStoredValuesCount)

            firstAvailableVarIndex += desc.storedStackSize
            currentStoredValuesCount += desc.storedValuesCount
            storedValuesDescriptorsStack.push(desc)
        }
        else if (isAfterInlineMarker(insn)) {
            frame ?: throw AssertionError("process method shouldn't be called if frame is null before inline marker")

            val desc = storedValuesDescriptorsStack.pop() ?:
                       throw AssertionError("should be non null becase markers are balanced")

            loadStackValues(methodNode, frame, insn, desc)
            firstAvailableVarIndex -= desc.storedStackSize
            currentStoredValuesCount -= desc.storedValuesCount
        }

        if (isInlineMarker(insn)) {
            methodNode.instructions.remove(insn)
        }
    }
}

private class StoredStackValuesDescriptor(
        val values: List<BasicValue>,
        val firstVariableIndex: Int,
        val storedStackSize: Int,
        alreadyStoredValuesCount: Int
) {
    val nextFreeVarIndex : Int get() = firstVariableIndex + storedStackSize
    val storedValuesCount: Int get() = values.size()
    val isStored: Boolean get() = storedValuesCount > 0
    val totalValuesCountOnStackBeforeInline = alreadyStoredValuesCount + storedValuesCount
}

private fun removeInlineMarkers(node: MethodNode) {
    for (insn in node.instructions.toArray()) {
        if (isInlineMarker(insn)) {
            node.instructions.remove(insn)
        }
    }
}

private fun storeStackValues(
        node: MethodNode,
        frame: Frame<BasicValue>,
        beforeInlineMarker: AbstractInsnNode,
        firstAvailableVarIndex: Int,
        alreadyStoredValuesCount: Int
) : StoredStackValuesDescriptor {
    var stackSize = 0

    val values = frame.getStackValuesStartingFrom(alreadyStoredValuesCount)

    for (value in values.reverse()) {
        node.instructions.insertBefore(
                beforeInlineMarker,
                VarInsnNode(
                        value.getType()!!.getOpcode(Opcodes.ISTORE),
                        firstAvailableVarIndex + stackSize
                )
        )
        stackSize += value.getSize()
    }

    node.updateMaxLocals(firstAvailableVarIndex + stackSize)

    return StoredStackValuesDescriptor(values, firstAvailableVarIndex, stackSize, alreadyStoredValuesCount)
}

private fun loadStackValues(
        node: MethodNode,
        frame: Frame<BasicValue>,
        afterInlineMarker: AbstractInsnNode,
        desc: StoredStackValuesDescriptor
) {
    if (!desc.isStored) return

    val insns = node.instructions
    var returnValueVarIndex = -1
    var returnType : Type? = null

    if (frame.getStackSize() != desc.totalValuesCountOnStackBeforeInline) {
        // only returned value
        assert(
                (frame.getStackSize() - desc.totalValuesCountOnStackBeforeInline) == 1,
                "Stack sizes should not differ by more than 1 (returned value)"
        )

        returnValueVarIndex = desc.nextFreeVarIndex
        returnType = frame.getStack(frame.getStackSize() - 1)!!.getType()
        node.updateMaxLocals(returnValueVarIndex + returnType!!.getSize())

        insns.insertBefore(
                afterInlineMarker,
                VarInsnNode(returnType!!.getOpcode(Opcodes.ISTORE), returnValueVarIndex)
        )
    }

    var currentVarIndex = desc.firstVariableIndex + desc.storedStackSize

    for (value in desc.values) {
        currentVarIndex -= value.getSize()
        insns.insertBefore(
                afterInlineMarker,
                VarInsnNode(
                        value.getType()!!.getOpcode(Opcodes.ILOAD),
                        currentVarIndex
                )
        )
    }

    if (returnValueVarIndex != -1) {
        insns.insertBefore(
                afterInlineMarker,
                VarInsnNode(returnType!!.getOpcode(Opcodes.ILOAD), returnValueVarIndex)
        )
    }
}

private fun <V : BasicValue> Frame<V>.getStackValuesStartingFrom(from: Int): List<V> =
        IntRange(from, getStackSize() - 1).map { getStack(it) }.requireNoNulls()

private fun MethodNode.updateMaxLocals(newMaxLocals: Int) {
    maxLocals = Math.max(maxLocals, newMaxLocals)
}
