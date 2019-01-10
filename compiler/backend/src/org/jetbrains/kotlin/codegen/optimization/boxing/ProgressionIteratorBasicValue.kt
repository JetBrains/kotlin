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

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.intrinsics.IteratorNext
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.codegen.range.getPrimitiveRangeOrProgressionElementType
import org.jetbrains.kotlin.codegen.range.supportedRangeTypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Type

class ProgressionIteratorBasicValue
private constructor(private val valuesPrimitiveTypeName: String) :
    StrictBasicValue(IteratorNext.getPrimitiveIteratorType(Name.identifier(valuesPrimitiveTypeName))) {

    val valuesPrimitiveType: Type = VALUES_TYPENAME_TO_TYPE[valuesPrimitiveTypeName] ?: error("Unexpected type $valuesPrimitiveTypeName")

    val nextMethodName: String
        get() = "next$valuesPrimitiveTypeName"

    val nextMethodDesc: String
        get() = "()" + valuesPrimitiveType.descriptor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false

        val value = other as ProgressionIteratorBasicValue?

        return valuesPrimitiveType == value!!.valuesPrimitiveType
    }

    override fun hashCode(): Int =
        super.hashCode() * 31 + valuesPrimitiveType.hashCode()

    companion object {
        private val VALUES_TYPENAME_TO_TYPE: Map<String, Type> =
            supportedRangeTypes.associate { primitiveType ->
                primitiveType.typeName.asString() to Type.getType(JvmPrimitiveType.get(primitiveType).desc)
            }

        private val ITERATOR_VALUE_BY_ELEMENT_PRIMITIVE_TYPE: Map<PrimitiveType, ProgressionIteratorBasicValue> =
            supportedRangeTypes.associate { elementType ->
                elementType to ProgressionIteratorBasicValue(elementType.typeName.asString())
            }

        fun byProgressionClassType(progressionClassType: Type): ProgressionIteratorBasicValue? {
            val classFqName = FqName(progressionClassType.className)
            val elementType = getPrimitiveRangeOrProgressionElementType(classFqName)
            return ITERATOR_VALUE_BY_ELEMENT_PRIMITIVE_TYPE[elementType]
        }
    }
}

