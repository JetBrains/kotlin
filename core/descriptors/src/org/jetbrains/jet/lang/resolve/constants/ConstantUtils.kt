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
        usesVariableAsConstant: Boolean = false,
        expectedType: JetType? = null
): CompileTimeConstant<*>? {
    // TODO: primitive arrays
    if (expectedType == null) {
        when(value) {
            is Byte -> return ByteValue(value, canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
            is Short -> return ShortValue(value, canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
            is Int -> return IntValue(value, canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
            is Long -> return LongValue(value, canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
        }
    }
    return when(value) {
        is Byte, is Short, is Int, is Long -> getIntegerValue((value as Number).toLong(), canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant, expectedType)
        is Char -> CharValue(value, canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
        is Float -> FloatValue(value, canBeUsedInAnnotation, usesVariableAsConstant)
        is Double -> DoubleValue(value, canBeUsedInAnnotation, usesVariableAsConstant)
        is Boolean -> BooleanValue(value, canBeUsedInAnnotation, usesVariableAsConstant)
        is String -> StringValue(value, canBeUsedInAnnotation, usesVariableAsConstant)
        null -> NullValue.NULL
        else -> null
    }
}

private fun getIntegerValue(
        value: Long,
        canBeUsedInAnnotation: Boolean,
        isPureIntConstant: Boolean,
        usesVariableAsConstant: Boolean,
        expectedType: JetType
): CompileTimeConstant<*>? {
    fun defaultIntegerValue(value: Long) = when (value) {
        value.toInt().toLong() -> IntValue(value.toInt(), canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
        else -> LongValue(value, canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
    }

    if (TypeUtils.noExpectedType(expectedType) || expectedType.isError()) {
        return IntegerValueTypeConstant(value, canBeUsedInAnnotation, usesVariableAsConstant)
    }

    val builtIns = KotlinBuiltIns.getInstance()

    return when (TypeUtils.makeNotNullable(expectedType)) {
        builtIns.getLongType() -> LongValue(value, canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
        builtIns.getShortType() -> when (value) {
            value.toShort().toLong() -> ShortValue(value.toShort(), canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
            else -> defaultIntegerValue(value)
        }
        builtIns.getByteType() -> when (value) {
            value.toByte().toLong() -> ByteValue(value.toByte(), canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
            else -> defaultIntegerValue(value)
        }
        builtIns.getCharType() -> IntValue(value.toInt(), canBeUsedInAnnotation, isPureIntConstant, usesVariableAsConstant)
        else -> defaultIntegerValue(value)
    }
}