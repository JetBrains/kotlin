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


class ControlFlowGraph private constructor(private val insns: InsnList) {
    private val edges: Array<MutableList<Int>> = Array(insns.size()) { arrayListOf<Int>() }

    fun getSuccessorsIndices(insn: AbstractInsnNode): List<Int> = getSuccessorsIndices(insns.indexOf(insn))
    fun getSuccessorsIndices(index: Int): List<Int> = edges[index]

    companion object {
        @JvmStatic
        fun build(node: MethodNode): ControlFlowGraph {
            val graph = ControlFlowGraph(node.instructions)

            fun addEdge(from: Int, to: Int) {
                graph.edges[from].add(to)
            }

            object : MethodAnalyzer<BasicValue>("fake", node, OptimizationBasicInterpreter()) {
                override fun visitControlFlowEdge(insn: Int, successor: Int): Boolean {
                    addEdge(insn, successor)
                    return true
                }

                override fun visitControlFlowExceptionEdge(insn: Int, successor: Int): Boolean {
                    addEdge(insn, successor)
                    return true
                }
            }.analyze()

            return graph
        }
    }
}
