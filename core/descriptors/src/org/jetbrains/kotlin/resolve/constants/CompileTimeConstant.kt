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
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeUtils

interface CompileTimeConstant<out T> {
    val isError: Boolean
        get() = false

    val parameters: CompileTimeConstant.Parameters

    fun toConstantValue(expectedType: KotlinType): ConstantValue<T>

    fun getValue(expectedType: KotlinType): T = toConstantValue(expectedType).value

    val canBeUsedInAnnotations: Boolean get() = parameters.canBeUsedInAnnotation

    val usesVariableAsConstant: Boolean get() = parameters.usesVariableAsConstant

    val usesNonConstValAsConstant: Boolean get() = parameters.usesNonConstValAsConstant

    val isPure: Boolean get() = parameters.isPure

    class Parameters(
            val canBeUsedInAnnotation: Boolean,
            val isPure: Boolean,
            val usesVariableAsConstant: Boolean,
            val usesNonConstValAsConstant: Boolean
    )
}

class TypedCompileTimeConstant<out T>(
        val constantValue: ConstantValue<T>,
        module: ModuleDescriptor,
        override val parameters: CompileTimeConstant.Parameters
) : CompileTimeConstant<T> {
    override val isError: Boolean
        get() = constantValue is ErrorValue

    val type: KotlinType = constantValue.getType(module)

    override fun toConstantValue(expectedType: KotlinType): ConstantValue<T> = constantValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypedCompileTimeConstant<*>) return false
        if (isError) return other.isError
        if (other.isError) return false
        return constantValue.value == other.constantValue.value && type == other.type
    }

    override fun hashCode(): Int {
        if (isError) return 13
        var result = constantValue.value?.hashCode() ?: 0
        result = 31 * result + type.hashCode()
        return result
    }
}

class IntegerValueTypeConstant(
        private val value: Number,
        builtIns: KotlinBuiltIns,
        override val parameters: CompileTimeConstant.Parameters
) : CompileTimeConstant<Number> {
    private val typeConstructor = IntegerValueTypeConstructor(value.toLong(), builtIns)

    override fun toConstantValue(expectedType: KotlinType): ConstantValue<Number> {
        val type = getType(expectedType)
        return when {
            KotlinBuiltIns.isInt(type) -> IntValue(value.toInt())
            KotlinBuiltIns.isByte(type) -> ByteValue(value.toByte())
            KotlinBuiltIns.isShort(type) -> ShortValue(value.toShort())
            else -> LongValue(value.toLong())
        }
    }

    val unknownIntegerType = KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            Annotations.EMPTY, typeConstructor, emptyList(), false,
            ErrorUtils.createErrorScope("Scope for number value type ($typeConstructor)", true)
    )

    fun getType(expectedType: KotlinType): KotlinType = TypeUtils.getPrimitiveNumberType(typeConstructor, expectedType)

    override fun toString() = typeConstructor.toString()

    override fun equals(other: Any?) = other is IntegerValueTypeConstant && value == other.value

    override fun hashCode() = value.hashCode()
}
