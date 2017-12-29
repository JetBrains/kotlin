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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType

abstract class ConstantValue<out T>(open val value: T) {
    abstract fun getType(module: ModuleDescriptor): KotlinType

    abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun toString() = value.toString()
}

abstract class IntegerValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)

class AnnotationValue(value: AnnotationDescriptor) : ConstantValue<AnnotationDescriptor>(value) {

    override fun getType(module: ModuleDescriptor): KotlinType = value.type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitAnnotationValue(this, data)
    override fun toString() = value.toString()
}

class ArrayValue(
        value: List<ConstantValue<*>>,
        private val computeType: (ModuleDescriptor) -> KotlinType
) : ConstantValue<List<ConstantValue<*>>>(value) {
    override fun getType(module: ModuleDescriptor): KotlinType = computeType(module).also { type ->
        assert(KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) { "Type should be an array, but was $type: $value" }
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitArrayValue(this, data)

    override fun equals(other: Any?): Boolean = this === other || value == (other as? ArrayValue)?.value

    override fun hashCode() = value.hashCode()
}

class BooleanValue(value: Boolean) : ConstantValue<Boolean>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.booleanType
    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitBooleanValue(this, data)
}

class ByteValue(value: Byte) : IntegerValueConstant<Byte>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.byteType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitByteValue(this, data)
    override fun toString(): String = "$value.toByte()"
}

class CharValue(value: Char) : IntegerValueConstant<Char>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.charType

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

class DoubleValue(value: Double) : ConstantValue<Double>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.doubleType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitDoubleValue(this, data)

    override fun toString() = "$value.toDouble()"
}

class EnumValue(value: ClassDescriptor) : ConstantValue<ClassDescriptor>(value) {
    val enumClassId: ClassId get() = (value.containingDeclaration as ClassDescriptor).classId!!

    val enumEntryName: Name get() = value.name

    override fun getType(module: ModuleDescriptor): KotlinType =
            value.classValueType
            ?: ErrorUtils.createErrorType("Containing class for error-class based enum entry ${value.fqNameUnsafe}")

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitEnumValue(this, data)

    override fun toString() = "${enumClassId.shortClassName}.$enumEntryName"

    override fun equals(other: Any?): Boolean = this === other || value == (other as? EnumValue)?.value

    override fun hashCode() = value.hashCode()
}

abstract class ErrorValue : ConstantValue<Unit>(Unit) {

    @Deprecated("Should not be called, for this is not a real value, but a indication of an error")
    override val value: Unit
        get() = throw UnsupportedOperationException()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitErrorValue(this, data)

    class ErrorValueWithMessage(val message: String) : ErrorValue() {

        override fun getType(module: ModuleDescriptor) = ErrorUtils.createErrorType(message)

        override fun toString() = message
    }

    companion object {
        fun create(message: String): ErrorValue {
            return ErrorValueWithMessage(message)
        }
    }
}

class FloatValue(value: Float) : ConstantValue<Float>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.floatType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitFloatValue(this, data)

    override fun toString() = "$value.toFloat()"
}

class IntValue(value: Int) : IntegerValueConstant<Int>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.intType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitIntValue(this, data)

    override fun equals(other: Any?): Boolean = this === other || value == (other as? IntValue)?.value

    override fun hashCode() = value
}

class KClassValue(private val type: KotlinType) : ConstantValue<KotlinType>(type) {
    override fun getType(module: ModuleDescriptor): KotlinType = type

    override val value: KotlinType
        get() = type.arguments.single().type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitKClassValue(this, data)
}

class LongValue(value: Long) : IntegerValueConstant<Long>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.longType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitLongValue(this, data)

    override fun toString() = "$value.toLong()"
}

class NullValue : ConstantValue<Void?>(null) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.nullableNothingType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitNullValue(this, data)

    override fun toString() = "null"
}

class ShortValue(value: Short) : IntegerValueConstant<Short>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.shortType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitShortValue(this, data)

    override fun toString() = "$value.toShort()"
}

class StringValue(value: String) : ConstantValue<String>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.stringType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitStringValue(this, data)

    override fun toString() = "\"$value\""

    override fun equals(other: Any?): Boolean = this === other || value == (other as? StringValue)?.value

    override fun hashCode() = value.hashCode()
}
