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

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.IincInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import java.util.*


class VariableLivenessFrame(val maxLocals: Int) {
    private val bitSet = BitSet(maxLocals)

    fun mergeFrom(other: VariableLivenessFrame) {
        bitSet.or(other.bitSet)
    }

    fun markAlive(varIndex: Int) {
        bitSet.set(varIndex, true)
    }

    fun markDead(varIndex: Int) {
        bitSet.set(varIndex, false)
    }

    fun isAlive(varIndex: Int): Boolean = bitSet.get(varIndex)

    override fun equals(other: Any?): Boolean {
        if (other !is VariableLivenessFrame) return false
        return bitSet == other.bitSet
    }

    override fun hashCode() = bitSet.hashCode()
}

fun analyzeLiveness(node: MethodNode): Array<VariableLivenessFrame> {
    val typeAnnotatedFrames = MethodTransformer.analyze("fake", node, OptimizationBasicInterpreter())

    val graph = ControlFlowGraph.build(node)
    val insnList = node.instructions
    val frames = Array(insnList.size()) { VariableLivenessFrame(node.maxLocals) }
    val insnArray = insnList.toArray()

    // see Figure 9.16 from Dragon book
    var wereChanges: Boolean

    do {
        wereChanges = false
        for (insn in insnArray) {
            val index = insnList.indexOf(insn)
            val newFrame = VariableLivenessFrame(node.maxLocals)
            for (successorIndex in graph.getSuccessorsIndices(insn)) {
                newFrame.mergeFrom(frames[successorIndex])
            }

            def(newFrame, insn)
            use(newFrame, insn, node, typeAnnotatedFrames[index])

            if (frames[index] != newFrame) {
                frames[index] = newFrame
                wereChanges = true
            }
        }

    } while (wereChanges)

    return frames
}

private fun def(frame: VariableLivenessFrame, insn: AbstractInsnNode) {
    if (insn is VarInsnNode && insn.isStoreOperation()) {
        frame.markDead(insn.`var`)
    }
}

private fun use(
        frame: VariableLivenessFrame,
        insn: AbstractInsnNode,
        node: MethodNode,
        // May be null in case of dead code
        typeAnnotatedFrame: Frame<BasicValue>?
) {
    val index = node.instructions.indexOf(insn)
    node.localVariables.filter {
        node.instructions.indexOf(it.start) < index && index < node.instructions.indexOf(it.end) &&
            Type.getType(it.desc).sort == typeAnnotatedFrame?.getLocal(it.index)?.type?.sort
    }.forEach {
        frame.markAlive(it.index)
    }

    if (insn is VarInsnNode && insn.isLoadOperation()) {
        frame.markAlive(insn.`var`)
    }
    else if (insn is IincInsnNode) {
        frame.markAlive(insn.`var`)
    }
}
