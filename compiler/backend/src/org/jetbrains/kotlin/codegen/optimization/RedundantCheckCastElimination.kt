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

import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.InstructionLivenessAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

class RedundantCheckCastEliminationMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val insns = methodNode.instructions.toArray()
        if (!insns.any { it.opcode == Opcodes.CHECKCAST }) return
        if (insns.any { ReifiedTypeInliner.isOperationReifiedMarker(it) }) return

        val typeAdjustmentForALoads = getTypeAdjustmentForALoadInstructions(insns, methodNode)
        val interpreter = object : OptimizationBasicInterpreter() {
            override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue {
                val adjustedType = typeAdjustmentForALoads[insn]
                return if (adjustedType != null)
                    newValue(adjustedType)
                        ?: throw AssertionError("Local variable type can't be VOID: $adjustedType")
                else
                    super.copyOperation(insn, value)
            }
        }

        val redundantCheckCasts = ArrayList<TypeInsnNode>()

        val frames = FastMethodAnalyzer(internalClassName, methodNode, interpreter, pruneExceptionEdges = true).analyze()
        for (i in insns.indices) {
            val insn = insns[i]
            if (insn.opcode == Opcodes.CHECKCAST) {
                val value = frames[i]?.top() ?: continue
                val typeInsn = insn as TypeInsnNode
                val insnType = Type.getObjectType(typeInsn.desc)
                if (value !== StrictBasicValue.NULL_VALUE && !isTrivialSubtype(insnType, value.type)) continue

                //Keep casts to multiarray types cause dex doesn't recognize ANEWARRAY [Ljava/lang/Object; as Object [][], but Object [] type
                //It's not clear is it bug in dex or not and maybe best to distinguish such types from MULTINEWARRRAY ones in method analyzer
                if (isMultiArrayType(insnType)) continue

                redundantCheckCasts.add(typeInsn)
            }
        }

        redundantCheckCasts.forEach {
            methodNode.instructions.remove(it)
        }
    }

    private fun getTypeAdjustmentForALoadInstructions(
        insns: Array<AbstractInsnNode>,
        methodNode: MethodNode
    ): Map<AbstractInsnNode, Type> {
        val isNonHandler = InstructionLivenessAnalyzer(methodNode, visitExceptionHandlers = false).analyze()

        val result = HashMap<AbstractInsnNode, Type>()
        for (lv in methodNode.localVariables) {
            val startIndex = methodNode.instructions.indexOf(lv.start)
            val endIndex = methodNode.instructions.indexOf(lv.end)
            for (i in startIndex until endIndex) {
                val insn = insns[i]
                // If we are in exception handler (or in dead code, but it really doesn't matter here, since dead code should not be seen
                // by data flow analyzer), treat ALOAD instructions as producing a value of declared local variable type.
                // Otherwise, resulting bytecode might fail verification on JDK 1.8+ because of inexact frames (see KT-47851).
                if (insn.opcode == Opcodes.ALOAD && (insn as VarInsnNode).`var` == lv.index && !isNonHandler[i]) {
                    result[insn] = Type.getType(lv.desc)
                }
            }
        }
        return result
    }

    private fun isTrivialSubtype(superType: Type, subType: Type) =
        superType == subType

    private fun isMultiArrayType(type: Type) = type.sort == Type.ARRAY && type.dimensions != 1
}