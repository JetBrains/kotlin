/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import java.util.*

class IntegerValueTypeConstructor(
    private val value: Long,
    private val module: ModuleDescriptor,
    parameters: CompileTimeConstant.Parameters
) : TypeConstructor {
    private val supertypes = ArrayList<KotlinType>(4)

    init {
        // order of types matters
        // 'getPrimitiveNumberType' returns first of supertypes that is a subtype of expected type
        // for expected type 'Any' result type 'Int' should be returned
        val isUnsigned = parameters.isUnsignedNumberLiteral

        if (isUnsigned) {
            assert(hasUnsignedTypesInModuleDependencies(module)) {
                "Unsigned types should be on classpath to create an unsigned type constructor"
            }
        }

        checkBoundsAndAddSuperType(value, if (isUnsigned) unsignedType(KotlinBuiltIns.FQ_NAMES.uInt) else builtIns.intType)
        checkBoundsAndAddSuperType(value, if (isUnsigned) unsignedType(KotlinBuiltIns.FQ_NAMES.uByte) else builtIns.byteType)
        checkBoundsAndAddSuperType(value, if (isUnsigned) unsignedType(KotlinBuiltIns.FQ_NAMES.uShort) else builtIns.shortType)
        supertypes.add(if (isUnsigned) unsignedType(KotlinBuiltIns.FQ_NAMES.uLong) else builtIns.longType)
    }

    private fun checkBoundsAndAddSuperType(value: Long, kotlinType: KotlinType) {
        if (value in kotlinType.minValue()..kotlinType.maxValue()) {
            supertypes.add(kotlinType)
        }
    }

    private fun unsignedType(classId: ClassId): SimpleType = module.findClassAcrossModuleDependencies(classId)!!.defaultType

    override fun getSupertypes(): Collection<KotlinType> = supertypes

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun isFinal() = false

    override fun isDenotable() = false

    override fun getDeclarationDescriptor() = null

    fun getValue(): Long = value

    override fun getBuiltIns(): KotlinBuiltIns {
        return module.builtIns
    }

    override fun toString() = "IntegerValueType($value)"
}

private fun KotlinType.minValue(): Long {
    if (UnsignedTypes.isUnsignedType(this)) return 0
    return when {
        KotlinBuiltIns.isByte(this) -> Byte.MIN_VALUE.toLong()
        KotlinBuiltIns.isShort(this) -> Short.MIN_VALUE.toLong()
        KotlinBuiltIns.isInt(this) -> Int.MIN_VALUE.toLong()
        else -> error("Can't get min value for type: $this")
    }
}

private fun KotlinType.maxValue(): Long {
    return when {
        KotlinBuiltIns.isByte(this) -> Byte.MAX_VALUE.toLong()
        KotlinBuiltIns.isShort(this) -> Short.MAX_VALUE.toLong()
        KotlinBuiltIns.isInt(this) -> Int.MAX_VALUE.toLong()

        KotlinBuiltIns.isUByte(this) -> UBYTE_MAX_VALUE
        KotlinBuiltIns.isUShort(this) -> USHORT_MAX_VALUE
        KotlinBuiltIns.isUInt(this) -> UINT_MAX_VALUE

        else -> error("Can't get max value for type: $this")
    }
}

private val UBYTE_MAX_VALUE = (-1).toByte().fromUByteToLong()
private val USHORT_MAX_VALUE = (-1).toShort().fromUShortToLong()
private val UINT_MAX_VALUE = (-1).fromUIntToLong()

