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

package org.jetbrains.kotlin.codegen.optimization.nullCheck

import org.jetbrains.kotlin.codegen.optimization.DeadCodeEliminationMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.boxing.ProgressionIteratorBasicValue
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

class RedundantNullCheckMethodTransformer : MethodTransformer() {
    private val deadCodeElimination = DeadCodeEliminationMethodTransformer()

    override fun transform(internalClassName: String, methodNode: MethodNode) {
        while (runSingleNullCheckEliminationPass(internalClassName, methodNode)) {
            deadCodeElimination.transform(internalClassName, methodNode)
        }
    }

    private fun isAlwaysFalse(opcode: Int, nullability: Nullability) =
            (opcode == Opcodes.IFNULL && nullability == Nullability.NOT_NULL) ||
            (opcode == Opcodes.IFNONNULL && nullability == Nullability.NULL)

    private fun isAlwaysTrue(opcode: Int, nullability: Nullability) =
            (opcode == Opcodes.IFNULL && nullability == Nullability.NULL) ||
            (opcode == Opcodes.IFNONNULL && nullability == Nullability.NOT_NULL)


    private fun runSingleNullCheckEliminationPass(internalClassName: String, methodNode: MethodNode): Boolean {
        val insnList = methodNode.instructions
        val instructions = insnList.toArray()

        val nullCheckIfs = instructions.mapNotNullTo(SmartList<JumpInsnNode>()) {
            it.safeAs<JumpInsnNode>()?.takeIf {
                it.opcode == Opcodes.IFNULL ||
                it.opcode == Opcodes.IFNONNULL
            }
        }
        if (nullCheckIfs.isEmpty()) return false

        val frames = analyze(internalClassName, methodNode, NullabilityInterpreter())

        val redundantNullCheckIfs = nullCheckIfs.mapNotNull { insn ->
            frames[instructions.indexOf(insn)]?.top()?.let { top ->
                val nullability = top.getNullability()
                if (nullability == Nullability.NULLABLE)
                    null
                else
                    Pair(insn, nullability)
            }
        }
        if (redundantNullCheckIfs.isEmpty()) return false

        for ((insn, nullability) in redundantNullCheckIfs) {
            val previous = insn.previous
            when (previous?.opcode) {
                Opcodes.ALOAD, Opcodes.DUP ->
                    insnList.remove(previous)
                else ->
                    insnList.insert(previous, InsnNode(Opcodes.POP))
            }

            when {
                isAlwaysTrue(insn.opcode, nullability) ->
                    insnList.set(insn, JumpInsnNode(Opcodes.GOTO, insn.label))
                isAlwaysFalse(insn.opcode, nullability) ->
                    insnList.remove(insn)
            }
        }

        return true
    }
}




