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

import org.jetbrains.kotlin.codegen.inline.remove
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.removeEmptyCatchBlocks
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class DeadCodeEliminationMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        transformWithResult(internalClassName, methodNode)
    }

    fun transformWithResult(internalClassName: String, methodNode: MethodNode): Result {
        val frames = analyze(internalClassName, methodNode, OptimizationBasicInterpreter())
        return removeDeadCodeByFrames(methodNode, frames)
    }

    fun removeDeadCodeByFrames(methodNode: MethodNode, frames: Array<out Any?>): Result {
        val insnsToRemove = ArrayList<AbstractInsnNode>()

        val insns = methodNode.instructions.toArray()
        for (i in insns.indices) {
            val insn = insns[i]
            if (shouldRemove(insn, i, frames)) {
                insnsToRemove.add(insn)
            }
        }

        methodNode.remove(insnsToRemove)

        // Remove empty try-catch blocks to make sure we don't break data flow analysis invariants by dead code elimination.
        methodNode.removeEmptyCatchBlocks()

        return Result(insnsToRemove.toSet())
    }

    private fun shouldRemove(insn: AbstractInsnNode, index: Int, frames: Array<out Any?>): Boolean =
            when (insn) {
                is LabelNode ->
                    // Do not remove label nodes because they can be referred by try/catch blocks or local variables table
                    false
                is LineNumberNode ->
                    isDeadLineNumber(insn, index, frames)
                else ->
                    frames[index] == null
            }

    private fun isDeadLineNumber(insn: LineNumberNode, index: Int, frames: Array<out Any?>): Boolean {
        // Line number node is "dead" if the corresponding line number interval
        // contains at least one "dead" meaningful instruction and no "live" meaningful instructions.
        var finger: AbstractInsnNode = insn
        var fingerIndex = index
        var hasDeadInsn = false
        loop@ while (true) {
            finger = finger.next ?: break
            fingerIndex++
            when (finger) {
                is LabelNode ->
                    continue@loop
                is LineNumberNode ->
                    if (finger.line != insn.line) return hasDeadInsn
                else -> {
                    if (frames[fingerIndex] != null) return false
                    hasDeadInsn = true
                }
            }
        }
        return true
    }

    class Result(val removedNodes: Set<AbstractInsnNode>) {
        fun hasRemovedAnything() = removedNodes.isNotEmpty()
        fun isRemoved(node: AbstractInsnNode) = removedNodes.contains(node)
        fun isAlive(node: AbstractInsnNode) = !isRemoved(node)
    }
}
