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

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.kotlin.types.*

class ProgressionIteratorBasicValue
private constructor(
    val iteratorCallInsn: AbstractInsnNode,
    val nextMethodName: String,
    iteratorType: Type,
    private val primitiveElementType: Type,
    val boxedElementType: Type
) : StrictBasicValue(iteratorType) {

    var tainted = false
        private set

    fun taint() {
        tainted = true
    }

    val nextMethodDesc: String
        get() = "()" + primitiveElementType.descriptor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val value = other as ProgressionIteratorBasicValue
        return primitiveElementType == value.primitiveElementType
    }

    override fun hashCode(): Int =
        super.hashCode() * 31 + nextMethodName.hashCode()

    companion object {
        // TODO functions returning inline classes are mangled now, should figure out how to work with UInt/ULong iterators here
        //     ProgressionIteratorBasicValue("UInt", Type.INT_TYPE, Type.getObjectType("kotlin/UInt"))
        //     ProgressionIteratorBasicValue("ULong", Type.LONG_TYPE, Type.getObjectType("kotlin/ULong"))

        private fun progressionIteratorValue(
            iteratorCallInsn: AbstractInsnNode,
            typeName: String,
            valuesPrimitiveType: Type,
            valuesBoxedType: Type = AsmUtil.boxType(valuesPrimitiveType)
        ) =
            ProgressionIteratorBasicValue(
                iteratorCallInsn,
                "next$typeName",
                Type.getObjectType("kotlin/collections/${typeName}Iterator"),
                valuesPrimitiveType,
                valuesBoxedType
            )

        fun byProgressionClassType(iteratorCallInsn: AbstractInsnNode, progressionClassType: Type): ProgressionIteratorBasicValue? =
            when (progressionClassType.className) {
                CHAR_RANGE_FQN, CHAR_PROGRESSION_FQN ->
                    progressionIteratorValue(iteratorCallInsn, "Char", Type.CHAR_TYPE)
                INT_RANGE_FQN, INT_PROGRESSION_FQN ->
                    progressionIteratorValue(iteratorCallInsn, "Int", Type.INT_TYPE)
                LONG_RANGE_FQN, LONG_PROGRESSION_FQN ->
                    progressionIteratorValue(iteratorCallInsn, "Long", Type.LONG_TYPE)
                else ->
                    null
            }
    }
}

