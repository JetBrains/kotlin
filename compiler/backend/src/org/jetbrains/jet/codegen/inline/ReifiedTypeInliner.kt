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

package org.jetbrains.jet.codegen.inline

import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.IntInsnNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods
import kotlin.platform.platformStatic
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import java.util.ArrayList

public class ReifiedTypeInliner(private val parametersMapping: ReifiedTypeParameterMappings?) {

    class object {
        public val NEW_ARRAY_MARKER_METHOD_NAME: String = "reifyNewArray"
    }

    public fun reifyInstructions(instructions: InsnList) {
        if (parametersMapping == null) return
        for (insn in instructions.toArray()) {
            if (isReifiedMarker(insn)) {
                processReifyMarker(insn as MethodInsnNode, instructions)
            }
        }
    }

    private fun isReifiedMarker(insn: AbstractInsnNode): Boolean {
        if (insn.getOpcode() != Opcodes.INVOKESTATIC || insn !is MethodInsnNode) return false
        return insn.owner == IntrinsicMethods.INTRINSICS_CLASS_NAME && insn.name.startsWith("reify")
    }

    private fun processReifyMarker(insn: MethodInsnNode, instructions: InsnList) {
        val mapping = getTypeParameterMapping(insn) ?: return

        if (mapping.asmType != null) {
            if (!when (insn.name) {
                NEW_ARRAY_MARKER_METHOD_NAME -> processNewArray(insn, mapping.asmType)
                else -> false
            }) {
                return
            }
            instructions.remove(insn.getPrevious()!!)
            instructions.remove(insn)
        } else {
            instructions.set(insn.getPrevious()!!, iconstInsn(mapping.newIndex!!))
        }
    }

    private fun processNewArray(insn: MethodInsnNode, parameter: Type): Boolean {
        if (insn.getNext()?.getOpcode() != Opcodes.ANEWARRAY) return false
        val next = insn.getNext() as TypeInsnNode
        next.desc = parameter.getInternalName()
        return true
    }

    private fun getParameterIndex(insn: MethodInsnNode): Int? {
        val prev = insn.getPrevious()!!

        return when (prev.getOpcode()) {
            Opcodes.ICONST_0 -> 0
            Opcodes.ICONST_1 -> 1
            Opcodes.ICONST_2 -> 2
            Opcodes.ICONST_3 -> 3
            Opcodes.ICONST_4 -> 4
            Opcodes.ICONST_5 -> 5
            Opcodes.BIPUSH, Opcodes.SIPUSH -> (prev as IntInsnNode).operand
            Opcodes.LDC -> (prev as LdcInsnNode).cst as Int
            else -> throw AssertionError("Unexpected opcode ${prev.getOpcode()}")
        }
    }

    private fun getTypeParameterMapping(insn: MethodInsnNode): ReifiedTypeParameterMapping? {
        return parametersMapping?.get(getParameterIndex(insn) ?: return null)
    }
}

public class ReifiedTypeParameterMappings(private val size: Int) {
    private val mappingsByIndex = arrayOfNulls<ReifiedTypeParameterMapping>(size)
    private val indexByParameterName = hashMapOf<String, Int>()

    public fun addParameterMappingToType(index: Int, name: String, asmType: Type) {
        mappingsByIndex[index] = ReifiedTypeParameterMapping(name, asmType, null)
        indexByParameterName[name] = index
    }

    public fun addParameterMappingToNewParameter(index: Int, name: String, newIndex: Int) {
        mappingsByIndex[index] = ReifiedTypeParameterMapping(name, null, newIndex)
        indexByParameterName[name] = index
    }

    fun get(index: Int) = mappingsByIndex[index]
    fun get(name: String): ReifiedTypeParameterMapping? {
        return this[indexByParameterName[name] ?: return null]
    }
}

public class ReifiedTypeParameterMapping(val name: String, val asmType: Type?, val newIndex: Int?)

private fun iconstInsn(n: Int): AbstractInsnNode {
    val node = MethodNode()
    InstructionAdapter(node).iconst(n)
    return node.instructions.getFirst()!!
}
