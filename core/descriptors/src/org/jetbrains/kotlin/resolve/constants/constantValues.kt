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
import org.jetbrains.kotlin.resolve.descriptorUtil.classObjectType
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.utils.sure

public abstract class ConstantValue<out T>(public open val value: T) {
    public abstract val type: KtType

    public abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun toString() = value.toString()
}

public abstract class IntegerValueConstant<T> protected constructor(value: T) : ConstantValue<T>(value)

public class AnnotationValue(value: AnnotationDescriptor) : ConstantValue<AnnotationDescriptor>(value) {

    override val type: KtType
        get() = value.getType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitAnnotationValue(this, data)
    override fun toString() = value.toString()
}

public class ArrayValue(
        value: List<ConstantValue<*>>,
        override val type: KtType,
        private val builtIns: KotlinBuiltIns
) : ConstantValue<List<ConstantValue<*>>>(value) {

    init {
        assert(KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) { "Type should be an array, but was " + type + ": " + value }
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitArrayValue(this, data)

    public val elementType: KtType
        get() = builtIns.getArrayElementType(type)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return value == (other as ArrayValue).value
    }

    override fun hashCode() = value.hashCode()
}

public class BooleanValue(
        value: Boolean,
        builtIns: KotlinBuiltIns
) : ConstantValue<Boolean>(value) {

    override val type = builtIns.getBooleanType()
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitBooleanValue(this, data)

}

public class ByteValue(
        value: Byte,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Byte>(value) {

    override val type = builtIns.getByteType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitByteValue(this, data)
    override fun toString(): String = "$value.toByte()"
}

public class CharValue(
        value: Char,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Char>(value) {

    override val type = builtIns.getCharType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitCharValue(this, data)

    override fun toString() = "\\u%04X ('%s')".format(value.toInt(), getPrintablePart(value))

    private fun getPrintablePart(c: Char): String {
        when (c) {
            '\b' -> return "\\b"
            '\t' -> return "\\t"
            '\n' -> return "\\n"
        //TODO: KT-8507
            12.toChar() -> return "\\f"
            '\r' -> return "\\r"
            else -> return if (isPrintableUnicode(c)) Character.toString(c) else "?"
        }
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

public class DoubleValue(
        value: Double,
        builtIns: KotlinBuiltIns
) : ConstantValue<Double>(value) {
    override val type = builtIns.getDoubleType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitDoubleValue(this, data)

    override fun toString() = "$value.toDouble()"
}

public class EnumValue(
        value: ClassDescriptor
) : ConstantValue<ClassDescriptor>(value) {

    override val type: KtType
        get() = value.classObjectType.sure { "Enum entry must have a class object type: " + value }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitEnumValue(this, data)

    override fun toString() = "$type.${value.getName()}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return value == (other as EnumValue).value
    }

    override fun hashCode() = value.hashCode()
}

public abstract class ErrorValue : ConstantValue<Unit>(Unit) {

    @Deprecated("Should not be called, for this is not a real value, but a indication of an error")
    override val value: Unit
        get() = throw UnsupportedOperationException()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitErrorValue(this, data)

    public class ErrorValueWithMessage(public val message: String) : ErrorValue() {

        override val type = ErrorUtils.createErrorType(message)

        override fun toString() = message
    }

    companion object {
        public fun create(message: String): ErrorValue {
            return ErrorValueWithMessage(message)
        }
    }
}

public class FloatValue(
        value: Float,
        builtIns: KotlinBuiltIns
) : ConstantValue<Float>(value) {
    override val type = builtIns.getFloatType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitFloatValue(this, data)

    override fun toString() = "$value.toFloat()"
}

public class IntValue(
        value: Int,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Int>(value) {

    override val type = builtIns.getIntType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitIntValue(this, data)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        val intValue = other as IntValue

        return value == intValue.value
    }

    override fun hashCode() = value
}

public class KClassValue(override val type: KtType) :
        ConstantValue<KtType>(type) {
    override val value: KtType
        get() = type.getArguments().single().getType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitKClassValue(this, data)
}

public class LongValue(
        value: Long,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Long>(value) {

    override val type = builtIns.getLongType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitLongValue(this, data)

    override fun toString() = "$value.toLong()"
}

public class NullValue(
        builtIns: KotlinBuiltIns
) : ConstantValue<Void?>(null) {

    override val type = builtIns.getNullableNothingType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitNullValue(this, data)

    override fun toString() = "null"
}

public class ShortValue(
        value: Short,
        builtIns: KotlinBuiltIns
) : IntegerValueConstant<Short>(value) {

    override val type = builtIns.getShortType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitShortValue(this, data)

    override fun toString() = "$value.toShort()"
}

public class StringValue(
        value: String,
        builtIns: KotlinBuiltIns
) : ConstantValue<String>(value) {
    override val type = builtIns.getStringType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitStringValue(this, data)

    override fun toString() = "\"$value\""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        return value != (other as StringValue).value
    }

    override fun hashCode() = value.hashCode()
}