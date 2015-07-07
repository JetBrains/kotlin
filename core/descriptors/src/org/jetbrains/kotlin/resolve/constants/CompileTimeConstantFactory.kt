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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils

public class CompileTimeConstantFactory(
        private val parameters: CompileTimeConstant.Parameters,
        private val builtins: KotlinBuiltIns
) {
    fun createLongValue(value: Long) = LongValue(value, parameters, builtins)

    fun createIntValue(value: Int) = IntValue(value, parameters, builtins)

    fun createErrorValue(message: String) = ErrorValue.create(message)

    fun createShortValue(value: Short) = ShortValue(value, parameters, builtins)

    fun createByteValue(value: Byte) = ByteValue(value, parameters, builtins)

    fun createDoubleValue(value: Double) = DoubleValue(value, parameters, builtins)

    fun createFloatValue(value: Float) = FloatValue(value, parameters, builtins)

    fun createBooleanValue(value: Boolean) = BooleanValue(value, parameters, builtins)

    fun createCharValue(value: Char) = CharValue(value, parameters, builtins)

    fun createStringValue(value: String) = StringValue(value, parameters, builtins)

    fun createNullValue() = NullValue(builtins)

    fun createEnumValue(enumEntryClass: ClassDescriptor): EnumValue = EnumValue(enumEntryClass)

    fun createArrayValue(
            value: List<CompileTimeConstant<*>>,
            type: JetType
    ) = ArrayValue(value, type, parameters)

    fun createAnnotationValue(value: AnnotationDescriptor) = AnnotationValue(value)

    fun createKClassValue(type: JetType) = KClassValue(type)

    fun createNumberTypeValue(value: Number) = IntegerValueTypeConstant(value, parameters)


    fun createCompileTimeConstant(
            value: Any?,
            expectedType: JetType? = null
    ): CompileTimeConstant<*>? {
        // TODO: primitive arrays
        if (expectedType == null) {
            when (value) {
                is Byte -> return createByteValue(value)
                is Short -> return createShortValue(value)
                is Int -> return createIntValue(value)
                is Long -> return createLongValue(value)
            }
        }
        return when (value) {
            is Byte, is Short, is Int, is Long -> getIntegerValue((value as Number).toLong(), expectedType)
            is Char -> createCharValue(value)
            is Float -> createFloatValue(value)
            is Double -> createDoubleValue(value)
            is Boolean -> createBooleanValue(value)
            is String -> createStringValue(value)
            null -> createNullValue()
            else -> null
        }
    }

    private fun getIntegerValue(
            value: Long,
            expectedType: JetType
    ): CompileTimeConstant<*>? {
        fun defaultIntegerValue(value: Long) = when (value) {
            value.toInt().toLong() -> createIntValue(value.toInt())
            else -> createLongValue(value)
        }

        if (TypeUtils.noExpectedType(expectedType) || expectedType.isError()) {
            return createNumberTypeValue(value)
        }

        val notNullExpected = TypeUtils.makeNotNullable(expectedType)
        return when {
            KotlinBuiltIns.isLong(notNullExpected) -> createLongValue(value)

            KotlinBuiltIns.isShort(notNullExpected) ->
                if (value == value.toShort().toLong())
                    createShortValue(value.toShort())
                else
                    defaultIntegerValue(value)

            KotlinBuiltIns.isByte(notNullExpected) ->
                if (value == value.toByte().toLong())
                    createByteValue(value.toByte())
                else
                    defaultIntegerValue(value)

            KotlinBuiltIns.isChar(notNullExpected) ->
                createIntValue(value.toInt())

            else -> defaultIntegerValue(value)
        }
    }
}