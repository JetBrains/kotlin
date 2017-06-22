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

package org.jetbrains.kotlin.codegen.optimization.boxing

import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class FastPopBackwardPropagationTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        while (transformOnce(methodNode)) {
        }
    }

    private fun transformOnce(methodNode: MethodNode): Boolean {
        val toRemove = ArrayList<AbstractInsnNode>()
        val toReplaceWithNop = ArrayList<AbstractInsnNode>()

        val insns = methodNode.instructions.toArray()

        for (i in 1 until insns.size) {
            val insn = insns[i]
            val prev = insns[i - 1]

            when (insn.opcode) {
                Opcodes.POP -> {
                    if (prev.isEliminatedByPop()) {
                        toReplaceWithNop.add(insn)
                        toRemove.add(prev)
                    }
                }

                Opcodes.POP2 -> {
                    if (prev.isEliminatedByPop2()) {
                        toReplaceWithNop.add(insn)
                        toRemove.add(prev)
                    }
                    else if (i > 1) {
                        val prev2 = insns[i - 2]
                        if (prev.isEliminatedByPop() && prev2.isEliminatedByPop()) {
                            toReplaceWithNop.add(insn)
                            toRemove.add(prev)
                            toRemove.add(prev2)
                        }
                    }
                }
            }
        }

        toReplaceWithNop.forEach { methodNode.instructions.set(it, InsnNode(Opcodes.NOP)) }
        toRemove.forEach { methodNode.instructions.remove(it) }

        return toReplaceWithNop.isNotEmpty() &&
               toRemove.isNotEmpty()
    }

    private fun AbstractInsnNode.isEliminatedByPop() =
            opcode in Opcodes.ACONST_NULL..Opcodes.FCONST_2 ||
            opcode in Opcodes.BIPUSH..Opcodes.ILOAD ||
            opcode == Opcodes.FLOAD ||
            opcode == Opcodes.ALOAD ||
            isUnitInstance() ||
            opcode == Opcodes.DUP

    private fun AbstractInsnNode.isEliminatedByPop2() =
            opcode == Opcodes.LCONST_0 || opcode == Opcodes.LCONST_1 ||
            opcode == Opcodes.DCONST_0 || opcode == Opcodes.DCONST_1 ||
            opcode == Opcodes.LLOAD ||
            opcode == Opcodes.DLOAD ||
            opcode == Opcodes.DUP2
}

