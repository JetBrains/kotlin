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

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*

public interface CompileTimeConstant<out T> {
    public val isError: Boolean
        get() = false

    public val parameters: CompileTimeConstant.Parameters

    public fun toConstantValue(expectedType: JetType): ConstantValue<T>

    public fun getValue(expectedType: JetType): T = toConstantValue(expectedType).value

    public val canBeUsedInAnnotations: Boolean get() = parameters.canBeUsedInAnnotation

    public val usesVariableAsConstant: Boolean get() = parameters.usesVariableAsConstant

    public val isPure: Boolean get() = parameters.isPure

    public class Parameters(
            public val canBeUsedInAnnotation: Boolean,
            public val isPure: Boolean,
            public val usesVariableAsConstant: Boolean
    )
}

public class TypedCompileTimeConstant<out T>(
        public val constantValue: ConstantValue<T>,
        override val parameters: CompileTimeConstant.Parameters
) : CompileTimeConstant<T> {
    override val isError: Boolean
        get() = constantValue is ErrorValue

    public val type: JetType = constantValue.type

    override fun toConstantValue(expectedType: JetType): ConstantValue<T> = constantValue
}

public class IntegerValueTypeConstant(
        private val value: Number,
        private val builtIns: KotlinBuiltIns,
        override val parameters: CompileTimeConstant.Parameters
) : CompileTimeConstant<Number> {
    private val typeConstructor = IntegerValueTypeConstructor(value.toLong(), builtIns)

    override fun toConstantValue(expectedType: JetType): ConstantValue<Number> {
        val factory = ConstantValueFactory(builtIns)
        val type = getType(expectedType)
        return when {
            KotlinBuiltIns.isInt(type) -> {
                factory.createIntValue(value.toInt())
            }
            KotlinBuiltIns.isByte(type) -> {
                factory.createByteValue(value.toByte())
            }
            KotlinBuiltIns.isShort(type) -> {
                factory.createShortValue(value.toShort())
            }
            else -> {
                factory.createLongValue(value.toLong())
            }
        }
    }

    val unknownIntegerType = JetTypeImpl.create(
            Annotations.EMPTY, typeConstructor, false, emptyList<TypeProjection>(),
            ErrorUtils.createErrorScope("Scope for number value type (" + typeConstructor.toString() + ")", true)
    )

    public fun getType(expectedType: JetType): JetType = TypeUtils.getPrimitiveNumberType(typeConstructor, expectedType)

    override fun toString() = typeConstructor.toString()
}
