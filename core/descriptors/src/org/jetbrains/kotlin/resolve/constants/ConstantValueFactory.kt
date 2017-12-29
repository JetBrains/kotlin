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
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

object ConstantValueFactory {
    fun createLongValue(value: Long) = LongValue(value)

    fun createIntValue(value: Int) = IntValue(value)

    fun createErrorValue(message: String) = ErrorValue.create(message)

    fun createShortValue(value: Short) = ShortValue(value)

    fun createByteValue(value: Byte) = ByteValue(value)

    fun createDoubleValue(value: Double) = DoubleValue(value)

    fun createFloatValue(value: Float) = FloatValue(value)

    fun createBooleanValue(value: Boolean) = BooleanValue(value)

    fun createCharValue(value: Char) = CharValue(value)

    fun createStringValue(value: String) = StringValue(value)

    fun createNullValue() = NullValue()

    fun createArrayValue(value: List<ConstantValue<*>>, type: KotlinType) = createArrayValue(value) { type }

    fun createAnnotationValue(value: AnnotationDescriptor) = AnnotationValue(value)

    fun createKClassValue(type: KotlinType) = KClassValue(type)

    fun createConstantValue(value: Any?): ConstantValue<*>? {
        return when (value) {
            is Byte -> createByteValue(value)
            is Short -> createShortValue(value)
            is Int -> createIntValue(value)
            is Long -> createLongValue(value)
            is Char -> createCharValue(value)
            is Float -> createFloatValue(value)
            is Double -> createDoubleValue(value)
            is Boolean -> createBooleanValue(value)
            is String -> createStringValue(value)
            is ByteArray -> createArrayValue(value.toList().arrayToList(), PrimitiveType.BYTE.arrayType())
            is ShortArray -> createArrayValue(value.toList().arrayToList(), PrimitiveType.SHORT.arrayType())
            is IntArray -> createArrayValue(value.toList().arrayToList(), PrimitiveType.INT.arrayType())
            is LongArray -> createArrayValue(value.toList().arrayToList(), PrimitiveType.LONG.arrayType())
            is CharArray -> createArrayValue(value.toList().arrayToList(), PrimitiveType.CHAR.arrayType())
            is FloatArray -> createArrayValue(value.toList().arrayToList(), PrimitiveType.FLOAT.arrayType())
            is DoubleArray -> createArrayValue(value.toList().arrayToList(), PrimitiveType.DOUBLE.arrayType())
            is BooleanArray -> createArrayValue(value.toList().arrayToList(), PrimitiveType.BOOLEAN.arrayType())
            null -> createNullValue()
            else -> null
        }
    }

    private fun createArrayValue(value: List<ConstantValue<*>>, computeType: (ModuleDescriptor) -> KotlinType): ArrayValue =
            ArrayValue(value, computeType)

    private fun List<*>.arrayToList(): List<ConstantValue<*>> =
            this.toList().mapNotNull { createConstantValue(it) }

    private fun PrimitiveType.arrayType(): (ModuleDescriptor) -> KotlinType =
            { module -> module.builtIns.getPrimitiveArrayKotlinType(this) }

    fun createIntegerConstantValue(
            value: Long,
            expectedType: KotlinType
    ): ConstantValue<*>? {
        val notNullExpected = TypeUtils.makeNotNullable(expectedType)
        return when {
            KotlinBuiltIns.isLong(notNullExpected) -> createLongValue(value)
            KotlinBuiltIns.isInt(notNullExpected) && value == value.toInt().toLong() -> createIntValue(value.toInt())
            KotlinBuiltIns.isShort(notNullExpected) && value == value.toShort().toLong() -> createShortValue(value.toShort())
            KotlinBuiltIns.isByte(notNullExpected) && value == value.toByte().toLong() -> createByteValue(value.toByte())
            KotlinBuiltIns.isChar(notNullExpected) -> createIntValue(value.toInt())
            else -> null
        }
    }
}
