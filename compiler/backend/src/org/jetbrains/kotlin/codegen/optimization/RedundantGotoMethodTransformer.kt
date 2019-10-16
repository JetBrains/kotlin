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

import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class RedundantGotoMethodTransformer : MethodTransformer() {
    /**
     * Removes redundant GOTO's in the following cases:
     *  (1) subsequent labels
     *      ...
     *      goto Label (can be removed)
     *      nop        (any number of them, or maybe none; will be removed by RedundantNopsCleanupMethodTransformer)
     *      Label:
     *      ...
     *  (2) indirect goto
     *      ...
     *      <branch instruction> Label (can be rewrote to <branch instruction> Label2)
     *      ...
     *      Label:
     *      goto Label2 (must not be removed due to the previous instruction that can fallthrough on this goto)
     *      ...
     */
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val insns = methodNode.instructions.toArray().apply { reverse() }
        val insnsToRemove = arrayListOf<AbstractInsnNode>()
        val currentLabels = hashSetOf<LabelNode>()
        val labelsToReplace = hashMapOf<LabelNode, JumpInsnNode>()
        var pendingGoto: JumpInsnNode? = null

        for (insn in insns) {
            // We can remove jumps over labels, NOPs, GOTOs with the same target, and fake
            // instructions used to describe the current frame state. We have to keep jumps
            // over line numbers, though, as otherwise something like an `if` with an empty
            // `else` will trigger a breakpoint inside the `else` even when the condition is true.
            when {
                insn is LabelNode -> {
                    currentLabels.add(insn)
                    pendingGoto?.let { labelsToReplace[insn] = it }
                }
                insn.opcode == Opcodes.GOTO -> {
                    pendingGoto = insn as JumpInsnNode
                    if (insn.label in currentLabels) {
                        insnsToRemove.add(insn)
                    } else {
                        currentLabels.clear()
                    }
                }
                insn is LineNumberNode || (insn.isMeaningful && insn.opcode != Opcodes.NOP) -> {
                    currentLabels.clear()
                    pendingGoto = null
                }
            }
        }

        // Rewrite branch instructions.
        if (!labelsToReplace.isEmpty()) {
            insns.filterIsInstance<JumpInsnNode>().forEach { rewriteLabelIfNeeded(it, labelsToReplace) }
        }

        for (insnToRemove in insnsToRemove) {
            methodNode.instructions.remove(insnToRemove)
        }
    }

    private fun rewriteLabelIfNeeded(
        jumpInsn: JumpInsnNode,
        labelsToReplace: Map<LabelNode, JumpInsnNode>
    ) {
        val lastJumpInsn = getLastTargetJumpInsn(jumpInsn, labelsToReplace, mutableListOf())
        if (lastJumpInsn != null && lastJumpInsn != jumpInsn) {
            // Do not remove the old label because it can be used to define a local variable range.
            jumpInsn.label = lastJumpInsn.label
        }
    }

    private fun getLastTargetJumpInsn(
        jumpInsn: JumpInsnNode,
        labelsToReplace: Map<LabelNode, JumpInsnNode>,
        alreadyVisited: MutableList<JumpInsnNode>
    ): JumpInsnNode? {
        labelsToReplace[jumpInsn.label]?.let {
            if (alreadyVisited.contains(it)) {
                // Cycle detected, do no apply goto optimization
                return null
            }
            alreadyVisited.add(it)
            return getLastTargetJumpInsn(it, labelsToReplace, alreadyVisited)
        }
        return jumpInsn
    }
}
