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

import org.jetbrains.kotlin.codegen.JvmBackendClassResolver
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode

class RedundantCheckCastEliminationMethodTransformer(
        private val jvmBackendClassResolver: JvmBackendClassResolver
) : MethodTransformer() {

    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val insns = methodNode.instructions.toArray()
        if (!insns.any { it.opcode == Opcodes.CHECKCAST }) return

        val redundantCheckCasts = ArrayList<TypeInsnNode>()

        val frames = analyze(internalClassName, methodNode, OptimizationBasicInterpreter())
        for (i in insns.indices) {
            val valueType = frames[i]?.top()?.type ?: continue
            val insn = insns[i]
            if (ReifiedTypeInliner.isOperationReifiedMarker(insn.previous)) continue

            if (insn is TypeInsnNode && insn.opcode == Opcodes.CHECKCAST) {
                val insnType = Type.getObjectType(insn.desc)
                if (insnType.sort != Type.OBJECT) continue
                if (valueType == insnType || canSkipCheckcast(valueType, insnType)) {
                    redundantCheckCasts.add(insn)
                }
            }
        }

        redundantCheckCasts.forEach {
            methodNode.instructions.remove(it)
        }
    }

    private fun canSkipCheckcast(subType: Type, superType: Type): Boolean {
        val superClasses = jvmBackendClassResolver.resolveToClassDescriptors(superType)
        val subClasses = jvmBackendClassResolver.resolveToClassDescriptors(subType)

        return subClasses.any { subClass ->
            superClasses.any { superClass ->
                DescriptorUtils.isSubclass(subClass, superClass)
            }
        }
    }
}