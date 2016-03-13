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
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

import java.util.ArrayList
import java.util.HashSet

class BoxedBasicValue(
        boxedType: Type,
        val boxingInsn: AbstractInsnNode,
        val progressionIterator: ProgressionIteratorBasicValue?
) : BasicValue(boxedType) {
    private val associatedInsns = HashSet<AbstractInsnNode>()
    private val unboxingWithCastInsns = HashSet<Pair<AbstractInsnNode, Type>>()
    private val associatedVariables = HashSet<Int>()
    private val mergedWith = HashSet<BoxedBasicValue>()

    val primitiveType: Type = unboxType(boxedType)
    var isSafeToRemove = true; private set

    override fun equals(other: Any?) =
            this === other

    fun typeEquals(other: BasicValue) =
            other is BoxedBasicValue && type == other.type

    override fun hashCode() =
            System.identityHashCode(this)

    fun getAssociatedInsns(): List<AbstractInsnNode> =
            ArrayList(associatedInsns)

    fun addInsn(insnNode: AbstractInsnNode) {
        associatedInsns.add(insnNode)
    }

    fun addVariableIndex(index: Int) {
        associatedVariables.add(index)
    }

    fun getVariablesIndexes(): List<Int> =
            ArrayList(associatedVariables)

    fun addMergedWith(value: BoxedBasicValue) {
        mergedWith.add(value)
    }

    fun getMergedWith(): Iterable<BoxedBasicValue> =
            mergedWith

    fun markAsUnsafeToRemove() {
        isSafeToRemove = false
    }

    fun isDoubleSize() =
            primitiveType.size == 2

    fun isFromProgressionIterator() =
            progressionIterator != null

    fun addUnboxingWithCastTo(insn: AbstractInsnNode, type: Type) {
        unboxingWithCastInsns.add(Pair.create(insn, type))
    }

    fun getUnboxingWithCastInsns(): Set<Pair<AbstractInsnNode, Type>> =
            unboxingWithCastInsns

    companion object {
        private fun unboxType(boxedType: Type): Type {
            val primitiveType = AsmUtil.unboxPrimitiveTypeOrNull(boxedType)
            if (primitiveType != null) return primitiveType

            if (boxedType == AsmTypes.K_CLASS_TYPE) return AsmTypes.JAVA_CLASS_TYPE

            throw IllegalArgumentException("Expected primitive type wrapper or KClass, got: $boxedType")
        }
    }
}
