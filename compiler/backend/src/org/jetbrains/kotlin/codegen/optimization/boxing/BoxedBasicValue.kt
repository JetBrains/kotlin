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

package org.jetbrains.kotlin.codegen.optimization.boxing

import com.intellij.openapi.util.Pair
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.GenerationState.MultiFieldValueClassUnboxInfo
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isMultiFieldValueClass
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode

abstract class BoxedBasicValue(type: Type) : StrictBasicValue(type) {
    abstract val descriptor: BoxedValueDescriptor
    abstract fun taint(): BoxedBasicValue

    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}


class CleanBoxedValue(
    boxedType: Type,
    boxingInsn: AbstractInsnNode,
    progressionIterator: ProgressionIteratorBasicValue?,
    val generationState: GenerationState
) : BoxedBasicValue(boxedType) {
    override val descriptor = BoxedValueDescriptor(boxedType, boxingInsn, progressionIterator, generationState)

    private var tainted: TaintedBoxedValue? = null
    override fun taint(): BoxedBasicValue = tainted ?: TaintedBoxedValue(this).also { tainted = it }
}


class TaintedBoxedValue(private val boxedBasicValue: CleanBoxedValue) : BoxedBasicValue(boxedBasicValue.type) {
    override val descriptor get() = boxedBasicValue.descriptor

    override fun taint(): BoxedBasicValue = this
}


class BoxedValueDescriptor(
    val boxedType: Type,
    val boxingInsn: AbstractInsnNode,
    val progressionIterator: ProgressionIteratorBasicValue?,
    val generationState: GenerationState
) {
    private val associatedInsns = LinkedHashSet<AbstractInsnNode>()
    private val unboxingWithCastInsns = LinkedHashSet<Pair<AbstractInsnNode, Type>>()
    private val associatedVariables = HashSet<Int>()
    private val mergedWith = HashSet<BoxedValueDescriptor>()

    var isSafeToRemove = true; private set
    val multiFieldValueClassUnboxInfo = getMultiFieldValueClassUnboxInfo(boxedType, generationState)
    val unboxedTypes: List<Type> = getUnboxedTypes(boxedType, generationState, multiFieldValueClassUnboxInfo)

    fun getUnboxTypeOrOtherwiseMethodReturnType(methodInsnNode: MethodInsnNode?) =
        unboxedTypes.singleOrNull() ?: Type.getReturnType(methodInsnNode!!.desc)

    val isValueClassValue = isValueClassValue(boxedType)

    fun getAssociatedInsns() = associatedInsns.toList()

    fun sortAssociatedInsns(indexes: Map<AbstractInsnNode, Int>) {
        val newOrder = associatedInsns.sortedBy { indexes[it]!! }
        associatedInsns.clear()
        associatedInsns.addAll(newOrder)
    }

    fun addInsn(insnNode: AbstractInsnNode) {
        associatedInsns.add(insnNode)
    }

    fun addVariableIndex(index: Int) {
        associatedVariables.add(index)
    }

    fun getVariablesIndexes(): List<Int> =
        ArrayList(associatedVariables)

    fun addMergedWith(descriptor: BoxedValueDescriptor) {
        mergedWith.add(descriptor)
    }

    fun getMergedWith(): Iterable<BoxedValueDescriptor> =
        mergedWith

    fun markAsUnsafeToRemove() {
        isSafeToRemove = false
    }

    fun getTotalUnboxSize() = unboxedTypes.sumOf { it.size }

    fun isFromProgressionIterator() = progressionIterator != null

    fun addUnboxingWithCastTo(insn: AbstractInsnNode, type: Type) {
        unboxingWithCastInsns.add(Pair.create(insn, type))
    }

    fun getUnboxingWithCastInsns(): Set<Pair<AbstractInsnNode, Type>> =
        unboxingWithCastInsns

    fun sortUnboxingWithCastInsns(indexes: Map<AbstractInsnNode, Int>) {
        val newInsnsAndResultTypes = unboxingWithCastInsns.sortedBy { insnAndResultType -> indexes[insnAndResultType.first]!! }
        unboxingWithCastInsns.clear()
        unboxingWithCastInsns.addAll(newInsnsAndResultTypes)
    }
}

internal fun makePops(unboxedTypes: List<Type>) = InsnList().apply {
    var restSingleSize = false
    for (unboxType in unboxedTypes.asReversed()) {
        restSingleSize = when (unboxType.size) {
            1 -> {
                if (restSingleSize) add(InsnNode(Opcodes.POP2))
                !restSingleSize
            }

            2 -> {
                if (restSingleSize) add(InsnNode(Opcodes.POP))
                add(InsnNode(Opcodes.POP2))
                false
            }

            else -> error("Illegal type size: ${unboxType.size}")
        }
    }

    if (restSingleSize) {
        add(InsnNode(Opcodes.POP))
    }
}


fun getUnboxedTypes(
    boxedType: Type,
    state: GenerationState,
    multiFieldValueClassUnboxInfo: MultiFieldValueClassUnboxInfo?
): List<Type> {
    val primitiveType = AsmUtil.unboxPrimitiveTypeOrNull(boxedType)
    if (primitiveType != null) return listOf(primitiveType)

    if (boxedType == AsmTypes.K_CLASS_TYPE) return listOf(AsmTypes.JAVA_CLASS_TYPE)

    unboxedTypeOfInlineClass(boxedType, state)?.let { return listOf(it) }
    multiFieldValueClassUnboxInfo?.let { return it.unboxedTypes }

    throw IllegalArgumentException("Expected primitive type wrapper or KClass or inline class wrapper, got: $boxedType")
}

fun unboxedTypeOfInlineClass(boxedType: Type, state: GenerationState): Type? {
    val descriptor =
        state.jvmBackendClassResolver.resolveToClassDescriptors(boxedType).singleOrNull()?.takeIf { it.isInlineClass() } ?: return null
    return state.mapInlineClass(descriptor)
}

fun getMultiFieldValueClassUnboxInfo(boxedType: Type, state: GenerationState): MultiFieldValueClassUnboxInfo? {
    if (!state.config.supportMultiFieldValueClasses) return null

    val descriptor =
        state.jvmBackendClassResolver.resolveToClassDescriptors(boxedType).singleOrNull()?.takeIf { it.isMultiFieldValueClass() }
            ?: return null
    return state.multiFieldValueClassUnboxInfo(descriptor)
}

private fun isValueClassValue(boxedType: Type): Boolean {
    return !AsmUtil.isBoxedPrimitiveType(boxedType) && boxedType != AsmTypes.K_CLASS_TYPE
}
