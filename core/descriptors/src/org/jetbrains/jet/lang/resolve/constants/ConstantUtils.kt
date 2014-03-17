/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.constants

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.TypeUtils

public fun createCompileTimeConstant(
        value: Any?,
        canBeUsedInAnnotation: Boolean,
        isPureIntConstant: Boolean,
        expectedType: JetType? = null
): CompileTimeConstant<*>? {
    if (expectedType == null) {
        when(value) {
            is Byte -> return ByteValue(value, canBeUsedInAnnotation, isPureIntConstant)
            is Short -> return ShortValue(value, canBeUsedInAnnotation, isPureIntConstant)
            is Int -> return IntValue(value, canBeUsedInAnnotation, isPureIntConstant)
            is Long -> return LongValue(value, canBeUsedInAnnotation, isPureIntConstant)
        }
    }
    return when(value) {
        is Byte, is Short, is Int, is Long -> getIntegerValue((value as Number).toLong(), canBeUsedInAnnotation, isPureIntConstant, expectedType)
        is Char -> CharValue(value, canBeUsedInAnnotation, isPureIntConstant)
        is Float -> FloatValue(value, canBeUsedInAnnotation)
        is Double -> DoubleValue(value, canBeUsedInAnnotation)
        is Boolean -> BooleanValue(value, canBeUsedInAnnotation)
        is String -> StringValue(value, canBeUsedInAnnotation)
        null -> NullValue.NULL
        else -> null
    }
}

private fun getIntegerValue(
        value: Long,
        canBeUsedInAnnotation: Boolean,
        isPureIntConstant: Boolean,
        expectedType: JetType
): CompileTimeConstant<*>? {
    fun defaultIntegerValue(value: Long) = when (value) {
        value.toInt().toLong() -> IntValue(value.toInt(), canBeUsedInAnnotation, isPureIntConstant)
        else -> LongValue(value, canBeUsedInAnnotation, isPureIntConstant)
    }

    if (TypeUtils.noExpectedType(expectedType) || expectedType.isError()) {
        return IntegerValueTypeConstant(value, canBeUsedInAnnotation)
    }

    val builtIns = KotlinBuiltIns.getInstance()

    return when (TypeUtils.makeNotNullable(expectedType)) {
        builtIns.getLongType() -> LongValue(value, canBeUsedInAnnotation, isPureIntConstant)
        builtIns.getShortType() -> when (value) {
            value.toShort().toLong() -> ShortValue(value.toShort(), canBeUsedInAnnotation, isPureIntConstant)
            else -> defaultIntegerValue(value)
        }
        builtIns.getByteType() -> when (value) {
            value.toByte().toLong() -> ByteValue(value.toByte(), canBeUsedInAnnotation, isPureIntConstant)
            else -> defaultIntegerValue(value)
        }
        builtIns.getCharType() -> IntValue(value.toInt(), canBeUsedInAnnotation, isPureIntConstant)
        else -> defaultIntegerValue(value)
    }
}