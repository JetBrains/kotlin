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

import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful

public class RedundantGotoMethodTransformer : MethodTransformer() {
    /**
     * Removes redundant GOTO's, i.e. to subsequent labels
     */
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val insns = methodNode.instructions.toArray()
        val insnsToRemove = arrayListOf<AbstractInsnNode>()

        val currentLabels = hashSetOf<LabelNode>()
        for (insn in insns.reverse()) {
            if (insn.isMeaningful) {
                if (insn.getOpcode() == Opcodes.GOTO && (insn as JumpInsnNode).label in currentLabels) {
                    insnsToRemove.add(insn)
                }
                else {
                    currentLabels.clear()
                }
                continue
            }

            if (insn is LabelNode) {
                currentLabels.add(insn)
            }
        }

        for (insnToRemove in insnsToRemove) {
            methodNode.instructions.remove(insnToRemove)
        }
    }
}
