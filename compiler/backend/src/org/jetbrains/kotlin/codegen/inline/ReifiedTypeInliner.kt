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

import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.generateAsCast
import org.jetbrains.kotlin.codegen.generateIsCheck
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.common.intConstant
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*

class ReificationArgument(
        val parameterName: String, val nullable: Boolean, private val arrayDepth: Int
) {
    fun asString() = "[".repeat(arrayDepth) + parameterName + (if (nullable) "?" else "")
    fun combine(replacement: ReificationArgument) =
            ReificationArgument(
                    replacement.parameterName,
                    this.nullable || (replacement.nullable && this.arrayDepth == 0),
                    this.arrayDepth + replacement.arrayDepth
            )

    fun reify(replacementAsmType: Type, kotlinType: KotlinType) =
            Pair(Type.getType("[".repeat(arrayDepth) + replacementAsmType), TypeUtils.makeNullableIfNeeded(kotlinType.arrayOf(arrayDepth), nullable))

    private fun KotlinType.arrayOf(arrayDepth: Int): KotlinType {
        val builtins = this.builtIns
        var currentType = this

        repeat(arrayDepth) {
            currentType = builtins.getArrayType(Variance.INVARIANT, currentType)
        }

        return currentType
    }
}

class ReifiedTypeInliner(private val parametersMapping: TypeParameterMappings?, private val isReleaseCoroutines: Boolean) {
    enum class OperationKind {
        NEW_ARRAY, AS, SAFE_AS, IS, JAVA_CLASS, ENUM_REIFIED;

        val id: Int get() = ordinal
    }

    companion object {
        const val REIFIED_OPERATION_MARKER_METHOD_NAME = "reifiedOperationMarker"
        const val NEED_CLASS_REIFICATION_MARKER_METHOD_NAME = "needClassReification"

        fun isOperationReifiedMarker(insn: AbstractInsnNode) =
                isReifiedMarker(insn) { it == REIFIED_OPERATION_MARKER_METHOD_NAME }

        private fun isReifiedMarker(insn: AbstractInsnNode, namePredicate: (String) -> Boolean): Boolean {
            if (insn.opcode != Opcodes.INVOKESTATIC || insn !is MethodInsnNode) return false
            return insn.owner == IntrinsicMethods.INTRINSICS_CLASS_NAME && namePredicate(insn.name)
        }

        @JvmStatic
        fun isNeedClassReificationMarker(insn: AbstractInsnNode): Boolean =
                isReifiedMarker(insn) { s -> s == NEED_CLASS_REIFICATION_MARKER_METHOD_NAME }

        @JvmStatic
        fun putNeedClassReificationMarker(v: MethodVisitor) {
            v.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    IntrinsicMethods.INTRINSICS_CLASS_NAME, NEED_CLASS_REIFICATION_MARKER_METHOD_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE), false
            )
        }
    }

    private var maxStackSize = 0

    private val hasReifiedParameters = parametersMapping?.hasReifiedParameters() ?: false

    /**
     * @return set of type parameters' identifiers contained in markers that should be reified further
     * e.g. when we're generating inline function containing reified T
     * and another function containing reifiable parts is inlined into that function
     */
    fun reifyInstructions(node: MethodNode): ReifiedTypeParametersUsages {
        if (!hasReifiedParameters) return ReifiedTypeParametersUsages()

        val instructions = node.instructions
        maxStackSize = 0
        val result = ReifiedTypeParametersUsages()
        for (insn in instructions.toArray()) {
            if (isOperationReifiedMarker(insn)) {
                val newName: String? = processReifyMarker(insn as MethodInsnNode, instructions)
                if (newName != null) {
                    result.addUsedReifiedParameter(newName)
                }
            }
        }

        node.maxStack = node.maxStack + maxStackSize
        return result
    }

    /**
     * @return new type parameter identifier if this marker should be reified further
     * or null if it shouldn't
     */
    private fun processReifyMarker(insn: MethodInsnNode, instructions: InsnList): String? {
        val operationKind = insn.operationKind ?: return null
        val reificationArgument = insn.reificationArgument ?: return null
        val mapping = parametersMapping?.get(reificationArgument.parameterName) ?: return null

        if (mapping.asmType != null) {
            // process* methods return false if marker should be reified further
            // or it's invalid (may be emitted explicitly in code)
            // they return true if instruction is reified and marker can be deleted
            val (asmType, kotlinType) = reificationArgument.reify(mapping.asmType, mapping.type)

            if (when (operationKind) {
                OperationKind.NEW_ARRAY -> processNewArray(insn, asmType)
                OperationKind.AS -> processAs(insn, instructions, kotlinType, asmType, safe = false)
                OperationKind.SAFE_AS -> processAs(insn, instructions, kotlinType, asmType, safe = true)
                OperationKind.IS -> processIs(insn, instructions, kotlinType, asmType)
                OperationKind.JAVA_CLASS -> processJavaClass(insn, asmType)
                OperationKind.ENUM_REIFIED -> processSpecialEnumFunction(insn, instructions, asmType)
            }) {
                instructions.remove(insn.previous.previous!!) // PUSH operation ID
                instructions.remove(insn.previous!!) // PUSH type parameter
                instructions.remove(insn) // INVOKESTATIC marker method
            }

            return null
        }
        else {
            val newReificationArgument = reificationArgument.combine(mapping.reificationArgument!!)
            instructions.set(insn.previous!!, LdcInsnNode(newReificationArgument.asString()))
            return mapping.reificationArgument.parameterName
        }
    }

    private fun processNewArray(insn: MethodInsnNode, parameter: Type) =
            processNextTypeInsn(insn, parameter, Opcodes.ANEWARRAY)

    private fun processAs(
            insn: MethodInsnNode,
            instructions: InsnList,
            kotlinType: KotlinType,
            asmType: Type,
            safe: Boolean
    ) = rewriteNextTypeInsn(insn, Opcodes.CHECKCAST) { stubCheckcast: AbstractInsnNode ->
        if (stubCheckcast !is TypeInsnNode) return false

        val newMethodNode = MethodNode(API)
        generateAsCast(InstructionAdapter(newMethodNode), kotlinType, asmType, safe, isReleaseCoroutines)

        instructions.insert(insn, newMethodNode.instructions)
        instructions.remove(stubCheckcast)

        // TODO: refine max stack calculation (it's not always as big as +4)
        maxStackSize = Math.max(maxStackSize, 4)

        return true
    }

    private fun processIs(
            insn: MethodInsnNode,
            instructions: InsnList,
            kotlinType: KotlinType,
            asmType: Type
    ) = rewriteNextTypeInsn(insn, Opcodes.INSTANCEOF) { stubInstanceOf: AbstractInsnNode ->
        if (stubInstanceOf !is TypeInsnNode) return false

        val newMethodNode = MethodNode(API)
        generateIsCheck(InstructionAdapter(newMethodNode), kotlinType, asmType, isReleaseCoroutines)

        instructions.insert(insn, newMethodNode.instructions)
        instructions.remove(stubInstanceOf)

        // TODO: refine max stack calculation (it's not always as big as +2)
        maxStackSize = Math.max(maxStackSize, 2)
        return true
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
        if (insn.next?.opcode != expectedNextOpcode) return false
        (insn.next as TypeInsnNode).desc = parameter.internalName
        return true
    }

    private fun processJavaClass(insn: MethodInsnNode, parameter: Type): Boolean {
        val next = insn.next
        if (next !is LdcInsnNode) return false
        next.cst = parameter
        return true
    }

    private fun processSpecialEnumFunction(insn: MethodInsnNode, instructions: InsnList, parameter: Type): Boolean {
        val next1 = insn.next ?: return false
        val next2 = next1.next ?: return false
        if (next1.opcode == Opcodes.ACONST_NULL && next2.opcode == Opcodes.ALOAD) {
            val next3 = next2.next ?: return false
            if (next3 is MethodInsnNode && next3.name == "valueOf") {
                instructions.remove(next1)
                next3.owner = parameter.internalName
                next3.desc = getSpecialEnumFunDescriptor(parameter, true)
                return true
            }
        }
        else if (next1.opcode == Opcodes.ICONST_0 && next2.opcode == Opcodes.ANEWARRAY) {
            instructions.remove(next1)
            instructions.remove(next2)
            val desc = getSpecialEnumFunDescriptor(parameter, false)
            instructions.insert(insn, MethodInsnNode(Opcodes.INVOKESTATIC, parameter.internalName, "values", desc, false))
            return true
        }

        return false
    }
}

val MethodInsnNode.reificationArgument: ReificationArgument?
    get() {
        val prev = previous!!

        val reificationArgumentRaw = when (prev.opcode) {
            Opcodes.LDC -> (prev as LdcInsnNode).cst as String
            else -> return null
        }

        val arrayDepth = reificationArgumentRaw.indexOfFirst { it != '[' }
        val parameterName = reificationArgumentRaw.substring(arrayDepth).removeSuffix("?")
        val nullable = reificationArgumentRaw.endsWith('?')

        return ReificationArgument(parameterName, nullable, arrayDepth)
    }

val MethodInsnNode.operationKind: ReifiedTypeInliner.OperationKind? get() =
    previous?.previous?.intConstant?.let {
        ReifiedTypeInliner.OperationKind.values().getOrNull(it)
    }

class TypeParameterMappings() {
    private val mappingsByName = hashMapOf<String, TypeParameterMapping>()

    fun addParameterMappingToType(name: String, type: KotlinType, asmType: Type, signature: String, isReified: Boolean) {
        mappingsByName[name] = TypeParameterMapping(
                name, type, asmType, reificationArgument = null, signature = signature, isReified = isReified
        )
    }

    fun addParameterMappingForFurtherReification(name: String, type: KotlinType, reificationArgument: ReificationArgument, isReified: Boolean) {
        mappingsByName[name] = TypeParameterMapping(
                name, type, asmType = null, reificationArgument = reificationArgument, signature = null, isReified = isReified
        )
    }

    operator fun get(name: String): TypeParameterMapping? = mappingsByName[name]

    fun hasReifiedParameters() = mappingsByName.values.any { it.isReified }

    internal inline fun forEach(l: (TypeParameterMapping) -> Unit)  {
        mappingsByName.values.forEach(l)
    }
}

class TypeParameterMapping(
        val name: String,
        val type: KotlinType,
        val asmType: Type?,
        val reificationArgument: ReificationArgument?,
        val signature: String?,
        val isReified: Boolean
)

class ReifiedTypeParametersUsages {
    private val usedTypeParameters: MutableSet<String> = hashSetOf()

    fun wereUsedReifiedParameters(): Boolean = usedTypeParameters.isNotEmpty()

    fun addUsedReifiedParameter(name: String) {
        usedTypeParameters.add(name)
    }

    fun propagateChildUsagesWithinContext(child: ReifiedTypeParametersUsages, context: MethodContext) {
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

    fun mergeAll(other: ReifiedTypeParametersUsages) {
        if (!other.wereUsedReifiedParameters()) return
        usedTypeParameters.addAll(other.usedTypeParameters)
    }
}
