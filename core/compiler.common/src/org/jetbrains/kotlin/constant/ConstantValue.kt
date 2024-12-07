/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.constant

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue

// Note 1: can be combined with org.jetbrains.kotlin.resolve.constants.ConstantValue but where is some questions to `AnnotationValue`.
// Note 2: if we are not going to implement previous idea, then this class can be moved to `fir` module.
// The problem here is that `ConstantValue` somehow must be accessible to `EvaluatedConstTracker`
// which in turn must be accessible to `CommonConfigurationKeys`.
sealed class ConstantValue<out T>(open val value: T) {
    abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun equals(other: Any?): Boolean = this === other || value == (other as? ConstantValue<*>)?.value

    override fun hashCode(): Int = value?.hashCode() ?: 0

    override fun toString(): String = value.toString()

    open fun stringTemplateValue(): String = value.toString()
}

abstract class IntegerValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)
abstract class UnsignedValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)

class AnnotationValue private constructor(value: Value) : ConstantValue<AnnotationValue.Value>(value) {
    class Value(val classId: ClassId, val argumentsMapping: Map<Name, ConstantValue<*>>) {
        override fun toString(): String {
            return "Value(classId=$classId, argumentsMapping=$argumentsMapping)"
        }
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitAnnotationValue(this, data)

    companion object {
        fun create(classId: ClassId, argumentsMapping: Map<Name, ConstantValue<*>>): AnnotationValue {
            return AnnotationValue(Value(classId, argumentsMapping))
        }
    }
}

class ArrayValue(
    value: List<ConstantValue<*>>,
) : ConstantValue<List<ConstantValue<*>>>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitArrayValue(this, data)
}

class BooleanValue(value: Boolean) : ConstantValue<Boolean>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitBooleanValue(this, data)
}

class ByteValue(value: Byte) : IntegerValueConstant<Byte>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitByteValue(this, data)

    override fun toString(): String = "$value.toByte()"
}

class CharValue(value: Char) : IntegerValueConstant<Char>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitCharValue(this, data)

    override fun toString(): String = "\\u%04X ('%s')".format(value.code, getPrintablePart(value))

    private fun getPrintablePart(c: Char): String = when (c) {
        '\b' -> "\\b"
        '\t' -> "\\t"
        '\n' -> "\\n"
        '\u000c' -> "\\f"
        '\r' -> "\\r"
        else -> if (isPrintableUnicode(c)) c.toString() else "?"
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

class DoubleValue(value: Double) : ConstantValue<Double>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitDoubleValue(this, data)

    override fun toString(): String = "$value.toDouble()"
}

class EnumValue(
    val enumClassId: ClassId,
    val enumEntryName: Name
) : ConstantValue<Pair<ClassId, Name>>(enumClassId to enumEntryName) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitEnumValue(this, data)

    override fun toString(): String = "${enumClassId.shortClassName}.$enumEntryName"
}

abstract class ErrorValue : ConstantValue<Unit>(Unit) {
    @Deprecated("Should not be called, for this is not a real value, but a indication of an error", level = DeprecationLevel.HIDDEN)
    override val value: Unit
        get() = throw UnsupportedOperationException()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitErrorValue(this, data)

    class ErrorValueWithMessage(val message: String) : ErrorValue() {
        override fun toString(): String = message
    }

    companion object {
        fun create(message: String): ErrorValue {
            return ErrorValueWithMessage(message)
        }
    }
}

class FloatValue(value: Float) : ConstantValue<Float>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitFloatValue(this, data)

    override fun toString(): String = "$value.toFloat()"
}

class IntValue(value: Int) : IntegerValueConstant<Int>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitIntValue(this, data)
}

class KClassValue private constructor(value: Value) : ConstantValue<KClassValue.Value>(value) {
    sealed class Value {
        data class NormalClass(val value: ClassLiteralValue) : Value() {
            val classId: ClassId get() = value.classId
            val arrayDimensions: Int get() = value.arrayNestedness
        }

        data object LocalClass : Value()
    }

    constructor() : this(Value.LocalClass)

    constructor(value: ClassLiteralValue) : this(Value.NormalClass(value))

    constructor(classId: ClassId, arrayDimensions: Int) : this(ClassLiteralValue(classId, arrayDimensions))

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitKClassValue(this, data)
}

class LongValue(value: Long) : IntegerValueConstant<Long>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitLongValue(this, data)

    override fun toString(): String = "$value.toLong()"
}

object NullValue : ConstantValue<Nothing?>(null) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitNullValue(this, data)
}

class ShortValue(value: Short) : IntegerValueConstant<Short>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitShortValue(this, data)

    override fun toString(): String = "$value.toShort()"
}

class StringValue(value: String) : ConstantValue<String>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitStringValue(this, data)

    override fun toString(): String = "\"$value\""
}

class UByteValue(byteValue: Byte) : UnsignedValueConstant<Byte>(byteValue) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitUByteValue(this, data)

    override fun toString(): String = "$value.toUByte()"

    override fun stringTemplateValue(): String = (value.toInt() and 0xFF).toString()
}

class UShortValue(shortValue: Short) : UnsignedValueConstant<Short>(shortValue) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitUShortValue(this, data)

    override fun toString(): String = "$value.toUShort()"

    override fun stringTemplateValue(): String = (value.toInt() and 0xFFFF).toString()
}

class UIntValue(intValue: Int) : UnsignedValueConstant<Int>(intValue) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitUIntValue(this, data)

    override fun toString(): String = "$value.toUInt()"

    override fun stringTemplateValue(): String = (value.toLong() and 0xFFFFFFFFL).toString()
}

class ULongValue(longValue: Long) : UnsignedValueConstant<Long>(longValue) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitULongValue(this, data)

    override fun toString(): String = "$value.toULong()"

    override fun stringTemplateValue(): String {
        if (value >= 0) return value.toString()

        val div10 = (value ushr 1) / 5
        val mod10 = value - 10 * div10

        return "$div10$mod10"
    }
}
