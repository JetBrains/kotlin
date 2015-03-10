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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import kotlin.platform.platformStatic
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import org.jetbrains.org.objectweb.asm.signature.SignatureReader
import org.jetbrains.org.objectweb.asm.signature.SignatureWriter
import org.jetbrains.org.objectweb.asm.MethodVisitor
import com.google.common.collect.ImmutableSet
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.resolve.DescriptorUtils

public class ReifiedTypeInliner(private val parametersMapping: ReifiedTypeParameterMappings?) {

    default object {
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
     * @return set of type parameters' identifiers contained in markers that should be reified further
     * e.g. when we're generating inline function containing reified T
     * and another function containing reifiable parts is inlined into that function
     */
    public fun reifyInstructions(instructions: InsnList): ReifiedTypeParametersUsages {
        if (parametersMapping == null) return ReifiedTypeParametersUsages()
        var result = ReifiedTypeParametersUsages()
        for (insn in instructions.toArray()) {
            if (isParametrisedReifiedMarker(insn)) {
                val newName: String? = processReifyMarker(insn as MethodInsnNode, instructions)
                if (newName != null) {
                    result.addUsedReifiedParameter(newName)
                }
            }
        }

        return result
    }

    public fun reifySignature(oldSignature: String): SignatureReificationResult {
        if (parametersMapping == null) return SignatureReificationResult(oldSignature, ReifiedTypeParametersUsages())

        val signatureRemapper = object : SignatureWriter() {
            var typeParamsToReify = ReifiedTypeParametersUsages()
            override fun visitTypeVariable(name: String?) {
                val mapping = getMappingByName(name) ?:
                              return super.visitTypeVariable(name)
                if (mapping.newName != null) {
                    typeParamsToReify.addUsedReifiedParameter(mapping.newName)
                    return super.visitTypeVariable(mapping.newName)
                }

                // else TypeVariable is replaced by concrete type
                SignatureReader(mapping.signature).accept(this)
            }

            override fun visitFormalTypeParameter(name: String?) {
                val mapping = getMappingByName(name) ?:
                              return super.visitFormalTypeParameter(name)
                if (mapping.newName != null) {
                    typeParamsToReify.addUsedReifiedParameter(mapping.newName)
                    super.visitFormalTypeParameter(mapping.newName)
                }
            }

            private fun getMappingByName(name: String?) = parametersMapping[name!!]
        }

        SignatureReader(oldSignature).accept(signatureRemapper)

        return SignatureReificationResult(signatureRemapper.toString(), signatureRemapper.typeParamsToReify)
    }

    data class SignatureReificationResult(val newSignature: String, val typeParametersUsages: ReifiedTypeParametersUsages)

    /**
     * @return new type parameter identifier if this marker should be reified further
     * or null if it shouldn't
     */
    private fun processReifyMarker(insn: MethodInsnNode, instructions: InsnList): String? {
        val mapping = getTypeParameterMapping(insn) ?: return null

        val asmType = mapping.asmType
        if (asmType != null) {
            // process* methods return false if marker should be reified further
            // or it's invalid (may be emitted explicitly in code)
            // they return true if instruction is reified and marker can be deleted
            if (when (insn.name) {
                NEW_ARRAY_MARKER_METHOD_NAME -> processNewArray(insn, asmType)
                CHECKCAST_MARKER_METHOD_NAME -> processCheckcast(insn, asmType)
                INSTANCEOF_MARKER_METHOD_NAME -> processInstanceof(insn, asmType)
                JAVA_CLASS_MARKER_METHOD_NAME -> processJavaClass(insn, asmType)
                else -> false
            }) {
                instructions.remove(insn.getPrevious()!!)
                instructions.remove(insn)
            }

            return null
        } else {
            instructions.set(insn.getPrevious()!!, LdcInsnNode(mapping.newName))
            return mapping.newName
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

    private fun getParameterName(insn: MethodInsnNode): String? {
        val prev = insn.getPrevious()!!

        return when (prev.getOpcode()) {
            Opcodes.LDC -> (prev as LdcInsnNode).cst as String
            else -> null
        }
    }

    private fun getTypeParameterMapping(insn: MethodInsnNode): ReifiedTypeParameterMapping? {
        return parametersMapping?.get(getParameterName(insn) ?: return null)
    }
}

public class ReifiedTypeParameterMappings() {
    private val mappingsByName = hashMapOf<String, ReifiedTypeParameterMapping>()

    public fun addParameterMappingToType(name: String, asmType: Type, signature: String) {
        mappingsByName[name] =  ReifiedTypeParameterMapping(name, asmType, newName = null, signature = signature)
    }

    public fun addParameterMappingToNewParameter(name: String, newName: String) {
        mappingsByName[name] = ReifiedTypeParameterMapping(name, asmType = null, newName = newName, signature = null)
    }

    fun get(name: String): ReifiedTypeParameterMapping? {
        return mappingsByName[name]
    }
}

public class ReifiedTypeParameterMapping(val name: String, val asmType: Type?, val newName: String?, val signature: String?)

public class ReifiedTypeParametersUsages {
    val usedTypeParameters: MutableSet<String> = hashSetOf()

    public fun wereUsedReifiedParameters(): Boolean = usedTypeParameters.isNotEmpty()

    public fun addUsedReifiedParameter(name: String) {
        usedTypeParameters.add(name)
    }

    public fun propagateChildUsagesWithinContext(child: ReifiedTypeParametersUsages, context: MethodContext) {
        if (!child.wereUsedReifiedParameters()) return
        // used for propagating reified TP usages from children member codegen to parent's
        // mark enclosing object-literal/lambda as needed reification iff
        // 1. at least one of it's method contains operations to reify
        // 2. reified type parameter of these operations is not from current method signature
        // i.e. from outer scope
        child.usedTypeParameters.filterNot {
            DescriptorUtils.containsReifiedTypeParameterWithName(context.getContextDescriptor(), it)
        }.forEach { usedTypeParameters.add(it) }
    }

    public fun mergeAll(other: ReifiedTypeParametersUsages) {
        if (!other.wereUsedReifiedParameters()) return
        usedTypeParameters.addAll(other.usedTypeParameters)
    }
}
