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

package org.jetbrains.kotlin.codegen.optimization.fixStack

import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.kotlin.codegen.pseudoInsns.parsePseudoInsnOrNull
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

public inline fun InsnList.forEachPseudoInsn(block: (PseudoInsn, AbstractInsnNode) -> Unit) {
    InsnSequence(this).forEach { insn ->
        parsePseudoInsnOrNull(insn)?.let { block(it, insn) }
    }
}

public inline fun InsnList.forEachInlineMarker(block: (String, MethodInsnNode) -> Unit) {
    InsnSequence(this).forEach { insn ->
        if (InlineCodegenUtil.isInlineMarker(insn)) {
            val methodInsnNode = insn as MethodInsnNode
            block(methodInsnNode.name, methodInsnNode)
        }
    }
}

public fun <V : Value> Frame<V>.top(): V? {
    val stackSize = getStackSize()
    if (stackSize == 0)
        return null
    else
        return getStack(stackSize - 1)
}

public fun MethodNode.updateMaxLocals(newMaxLocals: Int) {
    maxLocals = Math.max(maxLocals, newMaxLocals)
}

class SavedStackDescriptor(
        val savedValues: List<BasicValue>,
        val firstLocalVarIndex: Int
) {
    val savedValuesSize = savedValues.fold(0, { size, value -> size + value.getSize() })
    val firstUnusedLocalVarIndex = firstLocalVarIndex + savedValuesSize

    public override fun toString(): String =
            "@$firstLocalVarIndex: [$savedValues]"

    fun isNotEmpty(): Boolean = savedValues.isNotEmpty()
}


fun saveStack(methodNode: MethodNode, nodeToReplace: AbstractInsnNode, savedStackDescriptor: SavedStackDescriptor,
              restoreImmediately: Boolean) {
    with(methodNode.instructions) {
        generateStoreInstructions(methodNode, nodeToReplace, savedStackDescriptor)
        if (restoreImmediately) {
            generateLoadInstructions(methodNode, nodeToReplace, savedStackDescriptor)
        }
        remove(nodeToReplace)
    }
}

fun restoreStack(methodNode: MethodNode, location: AbstractInsnNode, savedStackDescriptor: SavedStackDescriptor) {
    with(methodNode.instructions) {
        generateLoadInstructions(methodNode, location, savedStackDescriptor)
        remove(location)
    }
}

fun restoreStackWithReturnValue(
        methodNode: MethodNode,
        nodeToReplace: AbstractInsnNode,
        savedStackDescriptor: SavedStackDescriptor,
        returnValue: BasicValue,
        returnValueLocalVarIndex: Int
) {
    with(methodNode.instructions) {
        insertBefore(nodeToReplace, VarInsnNode(returnValue.getType().getOpcode(Opcodes.ISTORE), returnValueLocalVarIndex))
        generateLoadInstructions(methodNode, nodeToReplace, savedStackDescriptor)
        insertBefore(nodeToReplace, VarInsnNode(returnValue.getType().getOpcode(Opcodes.ILOAD), returnValueLocalVarIndex))
        remove(nodeToReplace)
    }
}

fun generateLoadInstructions(methodNode: MethodNode, location: AbstractInsnNode, savedStackDescriptor: SavedStackDescriptor) {
    var localVarIndex = savedStackDescriptor.firstLocalVarIndex
    for (value in savedStackDescriptor.savedValues) {
        methodNode.instructions.insertBefore(location,
                                             VarInsnNode(value.getType().getOpcode(Opcodes.ILOAD), localVarIndex))
        localVarIndex += value.getSize()
    }
}

fun generateStoreInstructions(methodNode: MethodNode, location: AbstractInsnNode, savedStackDescriptor: SavedStackDescriptor) {
    var localVarIndex = savedStackDescriptor.firstUnusedLocalVarIndex
    for (value in savedStackDescriptor.savedValues.reverse()) {
        localVarIndex -= value.getSize()
        methodNode.instructions.insertBefore(location,
                                             VarInsnNode(value.getType().getOpcode(Opcodes.ISTORE), localVarIndex))
    }
}

fun getPopInstruction(top: BasicValue) =
        InsnNode(when (top.getSize()) {
                     1 -> Opcodes.POP
                     2 -> Opcodes.POP2
                     else -> throw AssertionError("Unexpected value type size")
                 })

fun removeAlwaysFalseIfeq(methodNode: MethodNode, node: AbstractInsnNode) {
    with (methodNode.instructions) {
        remove(node.getNext())
        remove(node)
    }
}

fun replaceAlwaysTrueIfeqWithGoto(methodNode: MethodNode, node: AbstractInsnNode) {
    with (methodNode.instructions) {
        val next = node.getNext() as JumpInsnNode
        insertBefore(node, JumpInsnNode(Opcodes.GOTO, next.label))
        remove(node)
        remove(next)
    }
}

fun replaceMarkerWithPops(methodNode: MethodNode, node: AbstractInsnNode, expectedStackSize: Int, frame: Frame<BasicValue>) {
    with (methodNode.instructions) {
        while (frame.getStackSize() > expectedStackSize) {
            val top = frame.pop()
            insertBefore(node, getPopInstruction(top))
        }
        remove(node)
    }
}

