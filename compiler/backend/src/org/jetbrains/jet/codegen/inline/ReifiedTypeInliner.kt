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
import org.jetbrains.org.objectweb.asm.signature.SignatureReader
import org.jetbrains.org.objectweb.asm.signature.SignatureWriter
import org.jetbrains.org.objectweb.asm.MethodVisitor
import com.google.common.collect.ImmutableSet

public class ReifiedTypeInliner(private val parametersMapping: ReifiedTypeParameterMappings?) {

    class object {
        public val NEW_ARRAY_MARKER_METHOD_NAME: String = "reifyNewArray"
        public val CHECKCAST_MARKER_METHOD_NAME: String = "reifyCheckcast"
        public val INSTANCEOF_MARKER_METHOD_NAME: String = "reifyInstanceof"
        public val JAVA_CLASS_MARKER_METHOD_NAME: String = "reifyJavaClass"
        public val NEED_CLASS_REIFICATION_MARKER_METHOD_NAME: String = "needClassReification"

        private val PARAMETRISED_MARKERS = ImmutableSet.of(
                NEW_ARRAY_MARKER_METHOD_NAME, CHECKCAST_MARKER_METHOD_NAME,
                INSTANCEOF_MARKER_METHOD_NAME, JAVA_CLASS_MARKER_METHOD_NAME
        )

        private fun isParametrisedReifiedMarker(insn: AbstractInsnNode) =
                isReifiedMarker(insn) { PARAMETRISED_MARKERS.contains(it) }

        private fun isReifiedMarker(insn: AbstractInsnNode, namePredicate: (String) -> Boolean): Boolean {
            if (insn.getOpcode() != Opcodes.INVOKESTATIC || insn !is MethodInsnNode) return false
            return insn.owner == IntrinsicMethods.INTRINSICS_CLASS_NAME && namePredicate(insn.name)
        }

        platformStatic public fun isNeedClassReificationMarker(insn: AbstractInsnNode): Boolean =
                isReifiedMarker(insn) { s -> s == NEED_CLASS_REIFICATION_MARKER_METHOD_NAME }

        platformStatic public fun putNeedClassReificationMarker(v: MethodVisitor) {
            v.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    IntrinsicMethods.INTRINSICS_CLASS_NAME, NEED_CLASS_REIFICATION_MARKER_METHOD_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE), false
            );
        }
    }

    /**
     * @return true if there is something need to be reified further
     * e.g. when we're generating inline function containing reified T
     * and another function containing reifiable parts is inlined into that function
     */
    public fun reifyInstructions(instructions: InsnList): Boolean {
        if (parametersMapping == null) return false
        var needFurtherReification = false
        for (insn in instructions.toArray()) {
            if (isParametrisedReifiedMarker(insn)) {
                if (processReifyMarker(insn as MethodInsnNode, instructions)) {
                    needFurtherReification = true
                }
            }
        }

        return needFurtherReification
    }

    public fun reifySignature(oldSignature: String): SignatureReificationResult {
        if (parametersMapping == null) return SignatureReificationResult(oldSignature, false)

        val signatureRemapper = object : SignatureWriter() {
            var needFurtherReification = false
            override fun visitTypeVariable(name: String?) {
                val mapping = getMappingByName(name) ?:
                              return super.visitTypeArgument()
                if (mapping.newName != null) {
                    needFurtherReification = true
                    return super.visitTypeVariable(mapping.newName)
                }

                // else TypeVariable is replaced by concrete type
                visitClassType(mapping.asmType!!.getInternalName())
                visitEnd()
            }

            override fun visitFormalTypeParameter(name: String?) {
                val mapping = getMappingByName(name) ?:
                              return super.visitFormalTypeParameter(name)
                if (mapping.newName != null) {
                    needFurtherReification = true
                    super.visitFormalTypeParameter(mapping.newName)
                }
            }

            private fun getMappingByName(name: String?) = parametersMapping[name!!]
        }

        SignatureReader(oldSignature).accept(signatureRemapper)

        return SignatureReificationResult(signatureRemapper.toString(), signatureRemapper.needFurtherReification)
    }

    data class SignatureReificationResult(val newSignature: String, val needFurtherReification: Boolean)

    /**
     * @return true if this marker should be reified further
     */
    private fun processReifyMarker(insn: MethodInsnNode, instructions: InsnList): Boolean {
        val mapping = getTypeParameterMapping(insn) ?: return false

        val asmType = mapping.asmType
        if (asmType != null) {
            if (!when (insn.name) {
                NEW_ARRAY_MARKER_METHOD_NAME -> processNewArray(insn, asmType)
                CHECKCAST_MARKER_METHOD_NAME -> processCheckcast(insn, asmType)
                INSTANCEOF_MARKER_METHOD_NAME -> processInstanceof(insn, asmType)
                JAVA_CLASS_MARKER_METHOD_NAME -> processJavaClass(insn, asmType)
                else -> false
            }) {
                return false
            }
            instructions.remove(insn.getPrevious()!!)
            instructions.remove(insn)

            return false
        } else {
            instructions.set(insn.getPrevious()!!, iconstInsn(mapping.newIndex!!))
            return true
        }
    }

    private fun processNewArray(insn: MethodInsnNode, parameter: Type) =
        processNextTypeInsn(insn, parameter, Opcodes.ANEWARRAY)

    private fun processCheckcast(insn: MethodInsnNode, parameter: Type) =
        processNextTypeInsn(insn, parameter, Opcodes.CHECKCAST)

    private fun processInstanceof(insn: MethodInsnNode, parameter: Type) =
        processNextTypeInsn(insn, parameter, Opcodes.INSTANCEOF)

    private fun processNextTypeInsn(insn: MethodInsnNode, parameter: Type, expectedNextOpcode: Int): Boolean {
        if (insn.getNext()?.getOpcode() != expectedNextOpcode) return false
        (insn.getNext() as TypeInsnNode).desc = parameter.getInternalName()
        return true
    }

    private fun processJavaClass(insn: MethodInsnNode, parameter: Type): Boolean {
        val next = insn.getNext()
        if (next !is LdcInsnNode) return false
        next.cst = parameter
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
        mappingsByIndex[index] = ReifiedTypeParameterMapping(name, asmType, null, null)
        indexByParameterName[name] = index
    }

    public fun addParameterMappingToNewParameter(index: Int, name: String, newIndex: Int, newName: String) {
        mappingsByIndex[index] = ReifiedTypeParameterMapping(name, null, newIndex, newName)
        indexByParameterName[name] = index
    }

    fun get(index: Int) = mappingsByIndex[index]
    fun get(name: String): ReifiedTypeParameterMapping? {
        return this[indexByParameterName[name] ?: return null]
    }
}

public class ReifiedTypeParameterMapping(val name: String, val asmType: Type?, val newIndex: Int?, val newName: String?)

private fun iconstInsn(n: Int): AbstractInsnNode {
    val node = MethodNode()
    InstructionAdapter(node).iconst(n)
    return node.instructions.getFirst()!!
}
