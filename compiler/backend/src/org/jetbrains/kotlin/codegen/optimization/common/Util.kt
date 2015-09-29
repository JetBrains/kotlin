/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

val AbstractInsnNode.TEMP_isMeaningful: Boolean get() =
    when (this.getType()) {
        AbstractInsnNode.LABEL, AbstractInsnNode.LINE, AbstractInsnNode.FRAME -> false
        else -> true
    }

public class InsnSequence(val from: AbstractInsnNode, val to: AbstractInsnNode?) : Sequence<AbstractInsnNode> {
    public constructor(insnList: InsnList) : this(insnList.getFirst(), null)

    override fun iterator(): Iterator<AbstractInsnNode> {
        return object : Iterator<AbstractInsnNode> {
            var current: AbstractInsnNode? = from
            override fun next(): AbstractInsnNode {
                val result = current
                current = current!!.getNext()
                return result!!
            }
            override fun hasNext() = current != to
        }
    }
}

fun MethodNode.prepareForEmitting() {
    tryCatchBlocks = tryCatchBlocks.filter { tcb ->
        InsnSequence(tcb.start, tcb.end).any { insn ->
            insn.TEMP_isMeaningful
        }
    }

    // local variables with live ranges starting after last meaningful instruction lead to VerifyError
    localVariables = localVariables.filter { lv ->
        InsnSequence(lv.start, lv.end).any { insn ->
            insn.TEMP_isMeaningful
        }
    }

    // We should remove linenumbers after last meaningful instruction
    // because they point to index of non-existing instruction and it leads to VerifyError
    var current = instructions.getLast()
    while (!current.TEMP_isMeaningful) {
        val prev = current.getPrevious()

        if (current.getType() == AbstractInsnNode.LINE) {
            instructions.remove(current)
        }

        current = prev
    }
}

abstract class BasicValueWrapper(val wrappedValue: BasicValue?) : BasicValue(wrappedValue?.getType()) {
    val basicValue: BasicValue? get() = (wrappedValue as? BasicValueWrapper)?.basicValue ?: wrappedValue

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && this.javaClass == other?.javaClass
    }
}

inline fun AbstractInsnNode.findNextOrNull(predicate: (AbstractInsnNode) -> Boolean): AbstractInsnNode? {
    var finger = this.getNext()
    while (finger != null && !predicate(finger)) {
        finger = finger.getNext()
    }
    return finger
}

inline fun AbstractInsnNode.findPreviousOrNull(predicate: (AbstractInsnNode) -> Boolean): AbstractInsnNode? {
    var finger = this.getPrevious()
    while (finger != null && !predicate(finger)) {
        finger = finger.getPrevious()
    }
    return finger
}

fun AbstractInsnNode.hasOpcode(): Boolean =
        getOpcode() >= 0
