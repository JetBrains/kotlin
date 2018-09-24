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
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

object ConstantValueFactory {
    fun createArrayValue(value: List<ConstantValue<*>>, type: KotlinType) = ArrayValue(value) { type }

    fun createConstantValue(value: Any?): ConstantValue<*>? {
        return when (value) {
            is Byte -> ByteValue(value)
            is Short -> ShortValue(value)
            is Int -> IntValue(value)
            is Long -> LongValue(value)
            is Char -> CharValue(value)
            is Float -> FloatValue(value)
            is Double -> DoubleValue(value)
            is Boolean -> BooleanValue(value)
            is String -> StringValue(value)
            is ByteArray -> createArrayValue(value.toList(), PrimitiveType.BYTE)
            is ShortArray -> createArrayValue(value.toList(), PrimitiveType.SHORT)
            is IntArray -> createArrayValue(value.toList(), PrimitiveType.INT)
            is LongArray -> createArrayValue(value.toList(), PrimitiveType.LONG)
            is CharArray -> createArrayValue(value.toList(), PrimitiveType.CHAR)
            is FloatArray -> createArrayValue(value.toList(), PrimitiveType.FLOAT)
            is DoubleArray -> createArrayValue(value.toList(), PrimitiveType.DOUBLE)
            is BooleanArray -> createArrayValue(value.toList(), PrimitiveType.BOOLEAN)
            null -> NullValue()
            else -> null
        }
    }

    fun createUnsignedValue(constantValue: ConstantValue<*>, type: KotlinType): UnsignedValueConstant<*>? {
        return when (constantValue) {
            is ByteValue -> UByteValue(constantValue.value)
            is ShortValue -> UShortValue(constantValue.value)
            is IntValue -> UIntValue(constantValue.value)
            is LongValue -> ULongValue(constantValue.value)
            else -> null
        }
    }

    private fun createArrayValue(value: List<*>, componentType: PrimitiveType): ArrayValue =
        ArrayValue(value.toList().mapNotNull(this::createConstantValue)) { module ->
            module.builtIns.getPrimitiveArrayKotlinType(componentType)
        }

    fun createIntegerConstantValue(
            value: Long,
            expectedType: KotlinType,
            isUnsigned: Boolean
    ): ConstantValue<*>? {
        val notNullExpected = TypeUtils.makeNotNullable(expectedType)
        return if (isUnsigned) {
            when {
                KotlinBuiltIns.isUByte(notNullExpected) && value == value.toByte().fromUByteToLong() -> UByteValue(value.toByte())
                KotlinBuiltIns.isUShort(notNullExpected) && value == value.toShort().fromUShortToLong() -> UShortValue(value.toShort())
                KotlinBuiltIns.isUInt(notNullExpected) && value == value.toInt().fromUIntToLong() -> UIntValue(value.toInt())
                KotlinBuiltIns.isULong(notNullExpected) -> ULongValue(value)
                else -> null
            }
        } else {
            when {
                KotlinBuiltIns.isLong(notNullExpected) -> LongValue(value)
                KotlinBuiltIns.isInt(notNullExpected) && value == value.toInt().toLong() -> IntValue(value.toInt())
                KotlinBuiltIns.isShort(notNullExpected) && value == value.toShort().toLong() -> ShortValue(value.toShort())
                KotlinBuiltIns.isByte(notNullExpected) && value == value.toByte().toLong() -> ByteValue(value.toByte())
                KotlinBuiltIns.isChar(notNullExpected) -> IntValue(value.toInt())
                else -> null
            }
        }
    }
}

fun Byte.fromUByteToLong(): Long = this.toLong() and 0xFF
fun Short.fromUShortToLong(): Long = this.toLong() and 0xFFFF
fun Int.fromUIntToLong(): Long = this.toLong() and 0xFFFF_FFFF
