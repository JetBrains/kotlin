/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue

internal sealed class ConstantValue<out T>(open val value: T) {
    abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun equals(other: Any?): Boolean = this === other || value == (other as? ConstantValue<*>)?.value

    override fun hashCode(): Int = value?.hashCode() ?: 0

    override fun toString(): String = value.toString()

    open fun stringTemplateValue(): String = value.toString()
}

internal abstract class IntegerValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)
internal abstract class UnsignedValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)

internal class AnnotationValue(value: FirAnnotationCall) : ConstantValue<FirAnnotationCall>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitAnnotationValue(this, data)
}

internal class ArrayValue(
    value: List<ConstantValue<*>>,
) : ConstantValue<List<ConstantValue<*>>>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitArrayValue(this, data)
}

internal class BooleanValue(value: Boolean) : ConstantValue<Boolean>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitBooleanValue(this, data)
}

internal class ByteValue(value: Byte) : IntegerValueConstant<Byte>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitByteValue(this, data)

    override fun toString(): String = "$value.toByte()"
}

internal class CharValue(value: Char) : IntegerValueConstant<Char>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitCharValue(this, data)

    override fun toString(): String = "\\u%04X ('%s')".format(value.toInt(), getPrintablePart(value))

    private fun getPrintablePart(c: Char): String = when (c) {
        '\b' -> "\\b"
        '\t' -> "\\t"
        '\n' -> "\\n"
        //TODO: KT-8507
        12.toChar() -> "\\f"
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

internal class DoubleValue(value: Double) : ConstantValue<Double>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitDoubleValue(this, data)

    override fun toString(): String = "$value.toDouble()"
}

internal class EnumValue(val enumClassId: ClassId, val enumEntryName: Name) : ConstantValue<Pair<ClassId, Name>>(enumClassId to enumEntryName) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitEnumValue(this, data)

    override fun toString(): String = "${enumClassId.shortClassName}.$enumEntryName"
}

internal abstract class ErrorValue : ConstantValue<Unit>(Unit) {
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

internal class FloatValue(value: Float) : ConstantValue<Float>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitFloatValue(this, data)

    override fun toString(): String = "$value.toFloat()"
}

internal class IntValue(value: Int) : IntegerValueConstant<Int>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitIntValue(this, data)
}

internal class KClassValue(value: Value) : ConstantValue<KClassValue.Value>(value) {
    sealed class Value {
        data class NormalClass(val value: ClassLiteralValue) : Value() {
            val classId: ClassId get() = value.classId
            val arrayDimensions: Int get() = value.arrayNestedness
        }

        data class LocalClass(val type: ConeKotlinType) : Value()
    }

    constructor(value: ClassLiteralValue) : this(Value.NormalClass(value))

    constructor(classId: ClassId, arrayDimensions: Int) : this(ClassLiteralValue(classId, arrayDimensions))

    fun getArgumentType(session: FirSession): ConeKotlinType? {
        when (value) {
            is Value.LocalClass -> return value.type
            is Value.NormalClass -> {
                val (classId, arrayDimensions) = value.value
                val klass = session.firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass ?: return null
                var type: ConeKotlinType = klass.defaultType().replaceArgumentsWithStarProjections()
                repeat(arrayDimensions) {
                    type = type.createArrayOf()
                }
                return type
            }
        }
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitKClassValue(this, data)

    companion object {
        fun create(argumentType: ConeKotlinType): ConstantValue<*>? {
            if (argumentType is ConeKotlinErrorType) return null
            if (argumentType !is ConeClassLikeType) return null
            var type = argumentType
            var arrayDimensions = 0
            while (true) {
                type = type.arrayElementType() ?: break
                arrayDimensions++
            }
            val classId = type.classId ?: return null
            return KClassValue(classId, arrayDimensions)
        }
    }
}

internal class LongValue(value: Long) : IntegerValueConstant<Long>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitLongValue(this, data)

    override fun toString(): String = "$value.toLong()"
}

internal object NullValue : ConstantValue<Nothing?>(null) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitNullValue(this, data)
}

internal class ShortValue(value: Short) : IntegerValueConstant<Short>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitShortValue(this, data)

    override fun toString(): String = "$value.toShort()"
}

internal class StringValue(value: String) : ConstantValue<String>(value) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitStringValue(this, data)

    override fun toString(): String = "\"$value\""
}

internal class UByteValue(byteValue: Byte) : UnsignedValueConstant<Byte>(byteValue) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitUByteValue(this, data)

    override fun toString(): String = "$value.toUByte()"

    override fun stringTemplateValue(): String = (value.toInt() and 0xFF).toString()
}

internal class UShortValue(shortValue: Short) : UnsignedValueConstant<Short>(shortValue) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitUShortValue(this, data)

    override fun toString(): String = "$value.toUShort()"

    override fun stringTemplateValue(): String = (value.toInt() and 0xFFFF).toString()
}

internal class UIntValue(intValue: Int) : UnsignedValueConstant<Int>(intValue) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitUIntValue(this, data)

    override fun toString(): String = "$value.toUInt()"

    override fun stringTemplateValue(): String = (value.toLong() and 0xFFFFFFFFL).toString()
}

internal class ULongValue(longValue: Long) : UnsignedValueConstant<Long>(longValue) {
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitULongValue(this, data)

    override fun toString(): String = "$value.toULong()"

    override fun stringTemplateValue(): String {
        if (value >= 0) return value.toString()

        val div10 = (value ushr 1) / 5
        val mod10 = value - 10 * div10

        return "$div10$mod10"
    }
}
