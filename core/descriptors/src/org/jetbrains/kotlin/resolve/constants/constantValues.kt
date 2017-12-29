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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType

abstract class ConstantValue<out T>(open val value: T) {
    abstract val type: KotlinType

    abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun toString() = value.toString()
}

abstract class IntegerValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)

class AnnotationValue(value: AnnotationDescriptor) : ConstantValue<AnnotationDescriptor>(value) {

    override val type: KotlinType
        get() = value.type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitAnnotationValue(this, data)
    override fun toString() = value.toString()
}

class ArrayValue(
        value: List<ConstantValue<*>>,
        override val type: KotlinType,
        private val builtIns: KotlinBuiltIns
) : ConstantValue<List<ConstantValue<*>>>(value) {

    init {
        assert(KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) { "Type should be an array, but was " + type + ": " + value }
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitArrayValue(this, data)

    override fun equals(other: Any?): Boolean = this === other || value == (other as? ArrayValue)?.value

    override fun hashCode() = value.hashCode()
}

class BooleanValue(
        value: Boolean,
        builtIns: KotlinBuiltIns
) : ConstantValue<Boolean>(value) {

    override val type = builtIns.booleanType
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitBooleanValue(this, data)

}

class ByteValue(
        value: Byte,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Byte>(value) {

    override val type = builtIns.byteType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitByteValue(this, data)
    override fun toString(): String = "$value.toByte()"
}

class CharValue(
        value: Char,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Char>(value) {

    override val type = builtIns.charType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitCharValue(this, data)

    override fun toString() = "\\u%04X ('%s')".format(value.toInt(), getPrintablePart(value))

    private fun getPrintablePart(c: Char): String = when (c) {
        '\b' -> "\\b"
        '\t' -> "\\t"
        '\n' -> "\\n"
    //TODO: KT-8507
        12.toChar() -> "\\f"
        '\r' -> "\\r"
        else -> if (isPrintableUnicode(c)) Character.toString(c) else "?"
    }

    private fun isPrintableUnicode(c: Char): Boolean {
        val t = Character.getType(c).toByte()
        return t != Character.UNASSIGNED &&
               t != Character.LINE_SEPARATOR &&
               t != Character.PARAGRAPH_SEPARATOR &&
               t != Character.CONTROL &&
               t != Character.FORMAT &&
               t != Character.PRIVATE_USE &&
               t != Character.SURROGATE
    }
}

class DoubleValue(
        value: Double,
        builtIns: KotlinBuiltIns
) : ConstantValue<Double>(value) {
    override val type = builtIns.doubleType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitDoubleValue(this, data)

    override fun toString() = "$value.toDouble()"
}

class EnumValue(
        value: ClassDescriptor
) : ConstantValue<ClassDescriptor>(value) {

    override val type: KotlinType
        get() = value.classValueType ?: ErrorUtils.createErrorType("Containing class for error-class based enum entry $value")

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitEnumValue(this, data)

    override fun toString() = "$type.${value.name}"

    override fun equals(other: Any?): Boolean = this === other || value == (other as? EnumValue)?.value

    override fun hashCode() = value.hashCode()
}

abstract class ErrorValue : ConstantValue<Unit>(Unit) {

    @Deprecated("Should not be called, for this is not a real value, but a indication of an error")
    override val value: Unit
        get() = throw UnsupportedOperationException()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitErrorValue(this, data)

    class ErrorValueWithMessage(val message: String) : ErrorValue() {

        override val type = ErrorUtils.createErrorType(message)

        override fun toString() = message
    }

    companion object {
        fun create(message: String): ErrorValue {
            return ErrorValueWithMessage(message)
        }
    }
}

class FloatValue(
        value: Float,
        builtIns: KotlinBuiltIns
) : ConstantValue<Float>(value) {
    override val type = builtIns.floatType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitFloatValue(this, data)

    override fun toString() = "$value.toFloat()"
}

class IntValue(
        value: Int,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Int>(value) {

    override val type = builtIns.intType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitIntValue(this, data)

    override fun equals(other: Any?): Boolean = this === other || value == (other as? IntValue)?.value

    override fun hashCode() = value
}

class KClassValue(override val type: KotlinType) :
        ConstantValue<KotlinType>(type) {
    override val value: KotlinType
        get() = type.arguments.single().type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitKClassValue(this, data)
}

class LongValue(
        value: Long,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Long>(value) {

    override val type = builtIns.longType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitLongValue(this, data)

    override fun toString() = "$value.toLong()"
}

class NullValue(
        builtIns: KotlinBuiltIns
) : ConstantValue<Void?>(null) {

    override val type = builtIns.nullableNothingType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitNullValue(this, data)

    override fun toString() = "null"
}

class ShortValue(
        value: Short,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Short>(value) {

    override val type = builtIns.shortType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitShortValue(this, data)

    override fun toString() = "$value.toShort()"
}

class StringValue(
        value: String,
        builtIns: KotlinBuiltIns
) : ConstantValue<String>(value) {
    override val type = builtIns.stringType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitStringValue(this, data)

    override fun toString() = "\"$value\""

    override fun equals(other: Any?): Boolean = this === other || value == (other as? StringValue)?.value

    override fun hashCode() = value.hashCode()
}
