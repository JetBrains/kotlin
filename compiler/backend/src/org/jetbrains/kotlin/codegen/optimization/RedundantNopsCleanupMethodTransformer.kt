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

package org.jetbrains.kotlin.codegen.optimization

import org.jetbrains.kotlin.codegen.optimization.common.findNextOrNull
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class RedundantNopsCleanupMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        // NOP instruction is required, iff one of the following conditions is true:
        // (a) it is a sole bytecode instruction in a try-catch block (TCB)
        // (b) it is a sole bytecode instruction is a source code line

        val requiredNops = HashSet<AbstractInsnNode>()

        recordNopsRequiredForSourceCodeLines(methodNode.instructions.first, requiredNops)
        recordNopsRequiredForTryCatchBlocks(methodNode, requiredNops)

        var current: AbstractInsnNode? = methodNode.instructions.first
        while (current != null) {
            if (current.opcode == Opcodes.NOP && !requiredNops.contains(current)) {
                val toRemove = current
                current = current.next
                methodNode.instructions.remove(toRemove)
            }
            else {
                current = current.next
            }
        }
    }

    private fun recordNopsRequiredForSourceCodeLines(first: AbstractInsnNode, requiredNops: MutableSet<AbstractInsnNode>) {
        var current: AbstractInsnNode? = first
        while (current != null) {
            if (current is LineNumberNode) {
                val nextLineNumberNode = current.getNextLineNumberNode()
                requiredNops.addIfNotNull(getRequiredNopInRange(current, nextLineNumberNode))
                current = nextLineNumberNode
            }
            else {
                current = current.next
            }
        }
    }

    private fun recordNopsRequiredForTryCatchBlocks(methodNode: MethodNode, requiredNops: MutableSet<AbstractInsnNode>) {
        for (tcb in methodNode.tryCatchBlocks) {
            val nop = tcb.start.findNextOrNull { it.isMeaningful }
            if (nop?.opcode == Opcodes.NOP) {
                requiredNops.add(nop)
            }
        }
    }
}


internal fun LineNumberNode.getNextLineNumberNode(): LineNumberNode? {
    var current: AbstractInsnNode? = this
    while (current != null) {
        if (current is LineNumberNode && current.line != this.line) {
            return current
        }
        current = current.next
    }
    return null
}

internal fun getRequiredNopInRange(firstInclusive: AbstractInsnNode, lastExclusive: AbstractInsnNode?): AbstractInsnNode? {
    var lastNop: AbstractInsnNode? = null
    var current: AbstractInsnNode? = firstInclusive
    while (current != null && current != lastExclusive) {
        if (current.isMeaningful && current.opcode != Opcodes.NOP) {
            return null
        }
        else if (current.opcode == Opcodes.NOP) {
            lastNop = current
        }
        current = current.next
    }

    return lastNop
}
