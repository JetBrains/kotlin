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

import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.TypeUtils

public fun createCompileTimeConstant(
        value: Any?,
        parameters: CompileTimeConstant.Parameters,
        expectedType: JetType? = null
): CompileTimeConstant<*>? {
    // TODO: primitive arrays
    if (expectedType == null) {
        when(value) {
            is Byte -> return ByteValue(value, parameters)
            is Short -> return ShortValue(value, parameters)
            is Int -> return IntValue(value, parameters)
            is Long -> return LongValue(value, parameters)
        }
    }
    return when(value) {
        is Byte, is Short, is Int, is Long -> getIntegerValue((value as Number).toLong(), parameters, expectedType)
        is Char -> CharValue(value, parameters)
        is Float -> FloatValue(value, parameters)
        is Double -> DoubleValue(value, parameters)
        is Boolean -> BooleanValue(value, parameters)
        is String -> StringValue(value, parameters)
        null -> NullValue
        else -> null
    }
}

private fun getIntegerValue(
        value: Long,
        parameters: CompileTimeConstant.Parameters,
        expectedType: JetType
): CompileTimeConstant<*>? {
    fun defaultIntegerValue(value: Long) = when (value) {
        value.toInt().toLong() -> IntValue(value.toInt(), parameters)
        else -> LongValue(value, parameters)
    }

    if (TypeUtils.noExpectedType(expectedType) || expectedType.isError()) {
        return IntegerValueTypeConstant(value, parameters)
    }

    val notNullExpected = TypeUtils.makeNotNullable(expectedType)
    return when {
        KotlinBuiltIns.isLong(notNullExpected) -> LongValue(value, parameters)

        KotlinBuiltIns.isShort(notNullExpected) ->
            if (value == value.toShort().toLong())
                ShortValue(value.toShort(), parameters)
            else
                defaultIntegerValue(value)

        KotlinBuiltIns.isByte(notNullExpected) ->
            if (value == value.toByte().toLong())
                ByteValue(value.toByte(), parameters)
            else
                defaultIntegerValue(value)

        KotlinBuiltIns.isChar(notNullExpected) ->
            IntValue(value.toInt(), parameters)

        else -> defaultIntegerValue(value)
    }
}
