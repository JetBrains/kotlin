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
import org.jetbrains.kotlin.codegen.range.*
import org.jetbrains.org.objectweb.asm.Type

class ProgressionIteratorBasicValue
private constructor(
    val nextMethodName: String,
    iteratorType: Type,
    private val primitiveElementType: Type,
    val boxedElementType: Type
) : StrictBasicValue(iteratorType) {

    private constructor(typeName: String, valuesPrimitiveType: Type, valuesBoxedType: Type = AsmUtil.boxType(valuesPrimitiveType)) :
            this("next$typeName", Type.getObjectType("kotlin/collections/${typeName}Iterator"), valuesPrimitiveType, valuesBoxedType)

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
        private val CHAR_PROGRESSION_ITERATOR_VALUE = ProgressionIteratorBasicValue("Char", Type.CHAR_TYPE)
        private val INT_PROGRESSION_ITERATOR_VALUE = ProgressionIteratorBasicValue("Int", Type.INT_TYPE)
        private val LONG_PROGRESSION_ITERATOR_VALUE = ProgressionIteratorBasicValue("Long", Type.LONG_TYPE)

        private val UINT_PROGRESSION_ITERATOR_VALUE =
            ProgressionIteratorBasicValue("UInt", Type.INT_TYPE, Type.getObjectType("kotlin/UInt"))
        private val ULONG_PROGRESSION_ITERATOR_VALUE =
            ProgressionIteratorBasicValue("ULong", Type.LONG_TYPE, Type.getObjectType("kotlin/ULong"))

        private val PROGRESSION_CLASS_NAME_TO_ITERATOR_VALUE: Map<String, ProgressionIteratorBasicValue> =
            hashMapOf(
                CHAR_RANGE_FQN to CHAR_PROGRESSION_ITERATOR_VALUE,
                CHAR_PROGRESSION_FQN to CHAR_PROGRESSION_ITERATOR_VALUE,
                INT_RANGE_FQN to INT_PROGRESSION_ITERATOR_VALUE,
                INT_PROGRESSION_FQN to INT_PROGRESSION_ITERATOR_VALUE,
                LONG_RANGE_FQN to LONG_PROGRESSION_ITERATOR_VALUE,
                LONG_PROGRESSION_FQN to LONG_PROGRESSION_ITERATOR_VALUE,
                UINT_RANGE_FQN to UINT_PROGRESSION_ITERATOR_VALUE,
                UINT_PROGRESSION_FQN to UINT_PROGRESSION_ITERATOR_VALUE,
                ULONG_RANGE_FQN to ULONG_PROGRESSION_ITERATOR_VALUE,
                ULONG_PROGRESSION_FQN to ULONG_PROGRESSION_ITERATOR_VALUE
            )

        fun byProgressionClassType(progressionClassType: Type): ProgressionIteratorBasicValue? =
            PROGRESSION_CLASS_NAME_TO_ITERATOR_VALUE[progressionClassType.className]
    }
}

