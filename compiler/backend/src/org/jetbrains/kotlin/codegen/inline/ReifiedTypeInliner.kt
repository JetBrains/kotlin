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

import com.google.common.collect.ImmutableSet
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.generateIsCheck
import org.jetbrains.kotlin.codegen.generateNullCheckForNonSafeAs
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.signature.SignatureReader
import org.jetbrains.org.objectweb.asm.signature.SignatureWriter
import org.jetbrains.org.objectweb.asm.tree.*

private class ParameterNameAndNullability(val name: String, val nullable: Boolean)

public class ReifiedTypeInliner(private val parametersMapping: ReifiedTypeParameterMappings?) {

    companion object {
        public val NEW_ARRAY_MARKER_METHOD_NAME: String = "reifyNewArray"
        public val CHECKCAST_MARKER_METHOD_NAME: String = "reifyCheckcast"
        public val SAFE_CHECKCAST_MARKER_METHOD_NAME: String = "reifySafeCheckcast"
        public val INSTANCEOF_MARKER_METHOD_NAME: String = "reifyInstanceof"
        public val JAVA_CLASS_MARKER_METHOD_NAME: String = "reifyJavaClass"
        public val NEED_CLASS_REIFICATION_MARKER_METHOD_NAME: String = "needClassReification"

        private val PARAMETRISED_MARKERS = ImmutableSet.of(
                NEW_ARRAY_MARKER_METHOD_NAME,
                CHECKCAST_MARKER_METHOD_NAME, SAFE_CHECKCAST_MARKER_METHOD_NAME,
                INSTANCEOF_MARKER_METHOD_NAME, JAVA_CLASS_MARKER_METHOD_NAME
        )

        private fun isParametrisedReifiedMarker(insn: AbstractInsnNode) =
                isReifiedMarker(insn) { PARAMETRISED_MARKERS.contains(it) }

        private fun isReifiedMarker(insn: AbstractInsnNode, namePredicate: (String) -> Boolean): Boolean {
            if (insn.getOpcode() != Opcodes.INVOKESTATIC || insn !is MethodInsnNode) return false
            return insn.owner == IntrinsicMethods.INTRINSICS_CLASS_NAME && namePredicate(insn.name)
        }

        @JvmStatic
        public fun isNeedClassReificationMarker(insn: AbstractInsnNode): Boolean =
                isReifiedMarker(insn) { s -> s == NEED_CLASS_REIFICATION_MARKER_METHOD_NAME }

        @JvmStatic
        public fun putNeedClassReificationMarker(v: MethodVisitor) {
            v.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    IntrinsicMethods.INTRINSICS_CLASS_NAME, NEED_CLASS_REIFICATION_MARKER_METHOD_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE), false
            );
        }

        @JvmStatic
        public fun isNullableMarkerInstruction(marker: String) = INSTANCEOF_MARKER_METHOD_NAME == marker ||
                                                                 CHECKCAST_MARKER_METHOD_NAME == marker
    }

    private var maxStackSize = 0

    /**
     * @return set of type parameters' identifiers contained in markers that should be reified further
     * e.g. when we're generating inline function containing reified T
     * and another function containing reifiable parts is inlined into that function
     */
    public fun reifyInstructions(node: MethodNode): ReifiedTypeParametersUsages {
        if (parametersMapping == null) return ReifiedTypeParametersUsages()
        val instructions = node.instructions
        maxStackSize = 0
        var result = ReifiedTypeParametersUsages()
        for (insn in instructions.toArray()) {
            if (isParametrisedReifiedMarker(insn)) {
                val newName: String? = processReifyMarker(insn as MethodInsnNode, instructions)
                if (newName != null) {
                    result.addUsedReifiedParameter(newName)
                }
            }
        }

        node.maxStack = node.maxStack + maxStackSize
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
        val parameter = getParameter(insn) ?: return null
        val mapping = parametersMapping?.get(parameter.name) ?: return null
        val kotlinType =
                if (isNullableMarkerInstruction(insn.name) && parameter.nullable)
                    TypeUtils.makeNullable(mapping.type)
                else
                    mapping.type


        val asmType = mapping.asmType
        if (asmType != null) {
            // process* methods return false if marker should be reified further
            // or it's invalid (may be emitted explicitly in code)
            // they return true if instruction is reified and marker can be deleted
            if (when (insn.name) {
                NEW_ARRAY_MARKER_METHOD_NAME -> processNewArray(insn, asmType)
                CHECKCAST_MARKER_METHOD_NAME -> processCheckcast(insn, instructions, kotlinType, asmType, safe = false)
                SAFE_CHECKCAST_MARKER_METHOD_NAME -> processCheckcast(insn, instructions, kotlinType, asmType, safe = true)
                INSTANCEOF_MARKER_METHOD_NAME -> processInstanceof(insn, instructions, kotlinType, asmType)
                JAVA_CLASS_MARKER_METHOD_NAME -> processJavaClass(insn, asmType)
                else -> false
            }) {
                instructions.remove(insn.getPrevious()!!)
                instructions.remove(insn)
            }

            return null
        } else {
            val nullableSuffix = if (isNullableMarkerInstruction(insn.name) && kotlinType.isMarkedNullable) "?" else ""
            instructions.set(insn.previous!!, LdcInsnNode(mapping.newName + nullableSuffix))
            return mapping.newName
        }
    }

    private fun processNewArray(insn: MethodInsnNode, parameter: Type) =
            processNextTypeInsn(insn, parameter, Opcodes.ANEWARRAY)

    private fun processCheckcast(insn: MethodInsnNode,
                                 instructions: InsnList,
                                 jetType: KotlinType,
                                 asmType: Type,
                                 safe: Boolean) =
            rewriteNextTypeInsn(insn, Opcodes.CHECKCAST) { instanceofInsn: AbstractInsnNode ->
                if (instanceofInsn !is TypeInsnNode) return false

                addNullCheckForAsIfNeeded(insn.previous!!, instructions, jetType, safe)
                TypeIntrinsics.checkcast(instanceofInsn, instructions, jetType, asmType, safe)
                return true
            }

    private fun addNullCheckForAsIfNeeded(insn: AbstractInsnNode, instructions: InsnList, jetType: KotlinType, safe: Boolean) {
        if (!safe && !TypeUtils.isNullableType(jetType)) {
            val methodNode = MethodNode(InlineCodegenUtil.API)
            generateNullCheckForNonSafeAs(InstructionAdapter(methodNode), jetType)

            InlineCodegenUtil.insertNodeBefore(methodNode, instructions, insn)
            maxStackSize = Math.max(maxStackSize, 4)
        }
    }

    private fun processInstanceof(insn: MethodInsnNode, instructions: InsnList, jetType: KotlinType, asmType: Type) =
            rewriteNextTypeInsn(insn, Opcodes.INSTANCEOF) { instanceofInsn: AbstractInsnNode ->
                if (instanceofInsn !is TypeInsnNode) return false

                addNullCheckForIsIfNeeded(insn, instructions, jetType)
                TypeIntrinsics.instanceOf(instanceofInsn, instructions, jetType, asmType)
                return true
            }

    private fun addNullCheckForIsIfNeeded(insn: AbstractInsnNode, instructions: InsnList, type: KotlinType) {
        if (TypeUtils.isNullableType(type)) {
            val instanceOf = insn.next
            insertNullCheckAround(instructions, insn.previous!!, instanceOf)
            maxStackSize = Math.max(maxStackSize, 2)
        }
    }

    private fun insertNullCheckAround(instructions: InsnList, start: AbstractInsnNode, end: AbstractInsnNode) {
        val methodNode = MethodNode(InlineCodegenUtil.API)
        var splitIndex: Int = -1
        generateIsCheck(InstructionAdapter(methodNode), true) {
            splitIndex = methodNode.instructions.size()
        }
        assert(splitIndex >= 0) {
            "Split index should be non-negative, but $splitIndex"
        }

        val nullCheckInsns = methodNode.instructions.toArray()
        nullCheckInsns.take(splitIndex).forEach {
            instructions.insertBefore(start, it)
        }

        nullCheckInsns.drop(splitIndex).reversed().forEach {
            instructions.insert(end, it)
        }
    }

    inline private fun rewriteNextTypeInsn(
            marker: MethodInsnNode,
            expectedNextOpcode: Int,
            rewrite: (AbstractInsnNode) -> Boolean
    ): Boolean {
        val next = marker.next ?: return false
        if (next.opcode != expectedNextOpcode) return false
        return rewrite(next)
    }

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

    private fun getParameter(insn: MethodInsnNode): ParameterNameAndNullability? {
        val prev = insn.getPrevious()!!

        val parameterNameWithFlag = when (prev.getOpcode()) {
            Opcodes.LDC -> (prev as LdcInsnNode).cst as String
            else -> return null
        }

        val parameterName = if (parameterNameWithFlag.endsWith("?")) parameterNameWithFlag.dropLast(1) else parameterNameWithFlag
        return ParameterNameAndNullability(parameterName, parameterName !== parameterNameWithFlag)
    }
}

public class ReifiedTypeParameterMappings() {
    private val mappingsByName = hashMapOf<String, ReifiedTypeParameterMapping>()

    public fun addParameterMappingToType(name: String, type: KotlinType, asmType: Type, signature: String) {
        mappingsByName[name] =  ReifiedTypeParameterMapping(name, type, asmType, newName = null, signature = signature)
    }

    public fun addParameterMappingToNewParameter(name: String, type: KotlinType, newName: String) {
        mappingsByName[name] = ReifiedTypeParameterMapping(name, type = type, asmType = null, newName = newName, signature = null)
    }

    operator fun get(name: String): ReifiedTypeParameterMapping? {
        return mappingsByName[name]
    }
}

public class ReifiedTypeParameterMapping(
        val name: String, val type: KotlinType, val asmType: Type?, val newName: String?, val signature: String?
)

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
        child.usedTypeParameters.filterNot { name ->
            context.contextDescriptor.typeParameters.any { typeParameter ->
                typeParameter.isReified && typeParameter.name.asString() == name
            }
        }.forEach { usedTypeParameters.add(it) }
    }

    public fun mergeAll(other: ReifiedTypeParametersUsages) {
        if (!other.wereUsedReifiedParameters()) return
        usedTypeParameters.addAll(other.usedTypeParameters)
    }
}
