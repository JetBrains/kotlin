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

import org.jetbrains.kotlin.codegen.optimization.boxing.isMethodInsnWith
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*

class ApiVersionCallsPreprocessingMethodTransformer(private val targetApiVersion: ApiVersion) : MethodTransformer() {
    private val constantConditionElimination = ConstantConditionEliminationMethodTransformer()

    override fun transform(internalClassName: String, methodNode: MethodNode) {
        var hasFoldedCalls = false

        for (insn in methodNode.instructions.toArray()) {
            if (!insn.isApiVersionIsAtLeastCall()) continue

            val prev3 = insn.previous ?: continue
            val minor = prev3.getIntConstValue() ?: continue

            val prev2 = prev3.previous ?: continue
            val major = prev2.getIntConstValue() ?: continue

            val prev1 = prev2.previous ?: continue
            val epic = prev1.getIntConstValue() ?: continue

            hasFoldedCalls = true

            val atLeastVersion = MavenComparableVersion("$epic.$major.$minor")

            val replacementInsn =
                if (targetApiVersion.version >= atLeastVersion)
                    InsnNode(Opcodes.ICONST_1)
                else
                    InsnNode(Opcodes.ICONST_0)

            methodNode.instructions.run {
                remove(prev1)
                remove(prev2)
                remove(prev3)
                set(insn, replacementInsn)
            }
        }

        if (hasFoldedCalls) {
            constantConditionElimination.transform(internalClassName, methodNode)
        }
    }

    private fun AbstractInsnNode.isApiVersionIsAtLeastCall(): Boolean =
        isMethodInsnWith(Opcodes.INVOKESTATIC) {
            owner.startsWith("kotlin/internal") &&
                    name == "apiVersionIsAtLeast" &&
                    desc == "(III)Z"
        }

    private fun AbstractInsnNode.getIntConstValue(): Int? =
        when (this) {
            is InsnNode ->
                if (opcode in Opcodes.ICONST_M1..Opcodes.ICONST_5)
                    opcode - Opcodes.ICONST_0
                else
                    null

            is IntInsnNode ->
                when (opcode) {
                    Opcodes.BIPUSH -> operand
                    Opcodes.SIPUSH -> operand
                    else -> null
                }

            is LdcInsnNode -> cst as? Int

            else -> null
        }
}