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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.ErrorUtils
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
        val isUnsigned = parameters.isUnsigned

        // TODO: fix value ranges for unsigned types
        checkBoundsAndAddSuperType(
            value,
            Integer.MIN_VALUE.toLong(),
            Integer.MAX_VALUE.toLong(),
            if (isUnsigned) unsignedType(KotlinBuiltIns.FQ_NAMES.uInt) else builtIns.intType
        )
        checkBoundsAndAddSuperType(
            value,
            java.lang.Byte.MIN_VALUE.toLong(),
            java.lang.Byte.MAX_VALUE.toLong(),
            if (isUnsigned) unsignedType(KotlinBuiltIns.FQ_NAMES.uByte) else builtIns.byteType
        )
        checkBoundsAndAddSuperType(
            value,
            java.lang.Short.MIN_VALUE.toLong(),
            java.lang.Short.MAX_VALUE.toLong(),
            if (isUnsigned) unsignedType(KotlinBuiltIns.FQ_NAMES.uShort) else builtIns.shortType
        )
        supertypes.add(if (isUnsigned) unsignedType(KotlinBuiltIns.FQ_NAMES.uLong) else builtIns.longType)
    }

    private fun checkBoundsAndAddSuperType(value: Long, minValue: Long, maxValue: Long, kotlinType: KotlinType) {
        if (value >= minValue && value <= maxValue) {
            supertypes.add(kotlinType)
        }
    }

    private fun unsignedType(classId: ClassId): SimpleType =
        module.findClassAcrossModuleDependencies(classId)?.defaultType ?: ErrorUtils.createErrorType("Error type for $classId")

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
