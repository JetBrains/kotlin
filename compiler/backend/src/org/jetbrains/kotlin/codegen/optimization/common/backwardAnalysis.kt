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

import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

interface VarFrame<F : VarFrame<F>> {
    fun mergeFrom(other: F)
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface BackwardAnalysisInterpreter<F : VarFrame<F>> {
    fun newFrame(maxLocals: Int): F
    fun def(frame: F, insn: AbstractInsnNode)
    fun use(frame: F, insn: AbstractInsnNode)
}

fun <F : VarFrame<F>> analyze(node: MethodNode, interpreter: BackwardAnalysisInterpreter<F>): List<F> {
    val graph = ControlFlowGraph.build(node)
    val insnList = node.instructions

    val frames = (1..insnList.size()).map { interpreter.newFrame(node.maxLocals) }.toMutableList()
    val insnArray = insnList.toArray()

    // see Figure 9.16 from Dragon book
    var wereChanges: Boolean

    do {
        wereChanges = false
        for (insn in insnArray) {
            val index = insnList.indexOf(insn)
            val newFrame = interpreter.newFrame(node.maxLocals)
            for (successorIndex in graph.getSuccessorsIndices(insn)) {
                newFrame.mergeFrom(frames[successorIndex])
            }

            interpreter.def(newFrame, insn)
            interpreter.use(newFrame, insn)

            if (frames[index] != newFrame) {
                frames[index] = newFrame
                wereChanges = true
            }
        }

    } while (wereChanges)

    return frames
}
