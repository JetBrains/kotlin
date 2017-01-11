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

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

fun <V : Value> Frame<V>.top(): V? =
        peek(0)

fun <V : Value> Frame<V>.peek(offset: Int): V? =
        if (stackSize >= offset) getStack(stackSize - offset - 1) else null

class SavedStackDescriptor(
        val savedValues: List<BasicValue>,
        val firstLocalVarIndex: Int
) {
    val savedValuesSize = savedValues.fold(0, { size, value -> size + value.size })
    val firstUnusedLocalVarIndex = firstLocalVarIndex + savedValuesSize

    override fun toString(): String =
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
        insertBefore(nodeToReplace, VarInsnNode(returnValue.type.getOpcode(Opcodes.ISTORE), returnValueLocalVarIndex))
        generateLoadInstructions(methodNode, nodeToReplace, savedStackDescriptor)
        insertBefore(nodeToReplace, VarInsnNode(returnValue.type.getOpcode(Opcodes.ILOAD), returnValueLocalVarIndex))
        remove(nodeToReplace)
    }
}

fun generateLoadInstructions(methodNode: MethodNode, location: AbstractInsnNode, savedStackDescriptor: SavedStackDescriptor) {
    var localVarIndex = savedStackDescriptor.firstLocalVarIndex
    for (value in savedStackDescriptor.savedValues) {
        methodNode.instructions.insertBefore(location,
                                             VarInsnNode(value.type.getOpcode(Opcodes.ILOAD), localVarIndex))
        localVarIndex += value.size
    }
}

fun generateStoreInstructions(methodNode: MethodNode, location: AbstractInsnNode, savedStackDescriptor: SavedStackDescriptor) {
    var localVarIndex = savedStackDescriptor.firstUnusedLocalVarIndex
    for (value in savedStackDescriptor.savedValues.asReversed()) {
        localVarIndex -= value.size
        methodNode.instructions.insertBefore(location,
                                             VarInsnNode(value.type.getOpcode(Opcodes.ISTORE), localVarIndex))
    }
}

fun getPopInstruction(top: BasicValue) =
        InsnNode(when (top.size) {
                     1 -> Opcodes.POP
                     2 -> Opcodes.POP2
                     else -> throw AssertionError("Unexpected value type size")
                 })

fun removeAlwaysFalseIfeq(methodNode: MethodNode, node: AbstractInsnNode) {
    with (methodNode.instructions) {
        remove(node.next)
        remove(node)
    }
}

fun replaceAlwaysTrueIfeqWithGoto(methodNode: MethodNode, node: AbstractInsnNode) {
    with (methodNode.instructions) {
        val next = node.next as JumpInsnNode
        insertBefore(node, JumpInsnNode(Opcodes.GOTO, next.label))
        remove(node)
        remove(next)
    }
}

fun replaceMarkerWithPops(methodNode: MethodNode, node: AbstractInsnNode, expectedStackSize: Int, stackContent: List<BasicValue>) {
    with (methodNode.instructions) {
        for (stackValue in stackContent.subList(expectedStackSize, stackContent.size)) {
            insert(node, getPopInstruction(stackValue))
        }
        remove(node)
    }
}
