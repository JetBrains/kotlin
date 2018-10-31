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
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections

abstract class ConstantValue<out T>(open val value: T) {
    abstract fun getType(module: ModuleDescriptor): KotlinType

    abstract fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R

    override fun equals(other: Any?): Boolean = this === other || value == (other as? ConstantValue<*>)?.value

    override fun hashCode(): Int = value?.hashCode() ?: 0

    override fun toString(): String = value.toString()

    open fun stringTemplateValue(): String = value.toString()
}

abstract class IntegerValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)
abstract class UnsignedValueConstant<out T> protected constructor(value: T) : ConstantValue<T>(value)

class AnnotationValue(value: AnnotationDescriptor) : ConstantValue<AnnotationDescriptor>(value) {
    override fun getType(module: ModuleDescriptor): KotlinType = value.type

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitAnnotationValue(this, data)
}

class ArrayValue(
        value: List<ConstantValue<*>>,
        private val computeType: (ModuleDescriptor) -> KotlinType
) : ConstantValue<List<ConstantValue<*>>>(value) {
    override fun getType(module: ModuleDescriptor): KotlinType = computeType(module).also { type ->
        assert(KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) { "Type should be an array, but was $type: $value" }
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitArrayValue(this, data)
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

class EnumValue(val enumClassId: ClassId, val enumEntryName: Name) : ConstantValue<Pair<ClassId, Name>>(enumClassId to enumEntryName) {
    override fun getType(module: ModuleDescriptor): KotlinType =
            module.findClassAcrossModuleDependencies(enumClassId)?.takeIf(DescriptorUtils::isEnumClass)?.defaultType
            ?: ErrorUtils.createErrorType("Containing class for error-class based enum entry $enumClassId.$enumEntryName")

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitEnumValue(this, data)

    override fun toString() = "${enumClassId.shortClassName}.$enumEntryName"
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
}

class KClassValue(value: ClassLiteralValue) : ConstantValue<ClassLiteralValue>(value) {
    val classId: ClassId get() = value.classId
    val arrayDimensions: Int get() = value.arrayNestedness

    constructor(classId: ClassId, arrayDimensions: Int) : this(ClassLiteralValue(classId, arrayDimensions))

    override fun getType(module: ModuleDescriptor): KotlinType =
        KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, module.builtIns.kClass, listOf(TypeProjectionImpl(getArgumentType(module))))

    fun getArgumentType(module: ModuleDescriptor): KotlinType {
        val descriptor = module.findClassAcrossModuleDependencies(classId)
            ?: return ErrorUtils.createErrorType("Unresolved type: $classId (arrayDimensions=$arrayDimensions)")

        // If this value refers to a class named test.Foo.Bar where both Foo and Bar have generic type parameters,
        // we're constructing a type `test.Foo<*>.Bar<*>` below
        var type = descriptor.defaultType.replaceArgumentsWithStarProjections()
        repeat(arrayDimensions) {
            type = module.builtIns.getArrayType(Variance.INVARIANT, type)
        }

        return type
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitKClassValue(this, data)

    companion object {
        fun create(argumentType: KotlinType): ConstantValue<*>? {
            if (argumentType.isError) return null

            var type = argumentType
            var arrayDimensions = 0
            while (KotlinBuiltIns.isArray(type)) {
                type = type.arguments.single().type
                arrayDimensions++
            }

            return when (val descriptor = type.constructor.declarationDescriptor) {
                is ClassDescriptor -> {
                    val classId = descriptor.classId ?: return null
                    KClassValue(classId, arrayDimensions)
                }
                is TypeParameterDescriptor -> {
                    // This is possible if a reified type parameter is used in annotation on a local class / anonymous object.
                    // In JVM class file, we can't represent such literal properly, so we're writing java.lang.Object instead.
                    // This has no effect on the compiler front-end or other back-ends, so we use kotlin.Any for simplicity here.
                    // Overall, it looks like such code should be disallowed: https://youtrack.jetbrains.com/issue/KT-27799
                    KClassValue(ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.any.toSafe()), 0)
                }
                else -> null
            }
        }
    }
}

class LongValue(value: Long) : IntegerValueConstant<Long>(value) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.longType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitLongValue(this, data)

    override fun toString() = "$value.toLong()"
}

class NullValue : ConstantValue<Void?>(null) {
    override fun getType(module: ModuleDescriptor) = module.builtIns.nullableNothingType

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitNullValue(this, data)
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
}

class UByteValue(byteValue: Byte) : UnsignedValueConstant<Byte>(byteValue) {
    override fun getType(module: ModuleDescriptor): KotlinType {
        return module.findClassAcrossModuleDependencies(KotlinBuiltIns.FQ_NAMES.uByte)?.defaultType
                ?: ErrorUtils.createErrorType("Unsigned type UByte not found")
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitUByteValue(this, data)

    override fun toString() = "$value.toUByte()"

    override fun stringTemplateValue(): String = (value.toInt() and 0xFF).toString()
}

class UShortValue(shortValue: Short) : UnsignedValueConstant<Short>(shortValue) {
    override fun getType(module: ModuleDescriptor): KotlinType {
        return module.findClassAcrossModuleDependencies(KotlinBuiltIns.FQ_NAMES.uShort)?.defaultType
                ?: ErrorUtils.createErrorType("Unsigned type UShort not found")
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitUShortValue(this, data)

    override fun toString() = "$value.toUShort()"

    override fun stringTemplateValue(): String = (value.toInt() and 0xFFFF).toString()
}

class UIntValue(intValue: Int) : UnsignedValueConstant<Int>(intValue) {
    override fun getType(module: ModuleDescriptor): KotlinType {
        return module.findClassAcrossModuleDependencies(KotlinBuiltIns.FQ_NAMES.uInt)?.defaultType
                ?: ErrorUtils.createErrorType("Unsigned type UInt not found")
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitUIntValue(this, data)

    override fun toString() = "$value.toUInt()"

    override fun stringTemplateValue(): String = (value.toLong() and 0xFFFFFFFFL).toString()
}

class ULongValue(longValue: Long) : UnsignedValueConstant<Long>(longValue) {
    override fun getType(module: ModuleDescriptor): KotlinType {
        return module.findClassAcrossModuleDependencies(KotlinBuiltIns.FQ_NAMES.uLong)?.defaultType
                ?: ErrorUtils.createErrorType("Unsigned type ULong not found")
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D): R = visitor.visitULongValue(this, data)

    override fun toString() = "$value.toULong()"

    override fun stringTemplateValue(): String {
        if (value >= 0) return value.toString()

        val div10 = (value ushr 1) / 5
        val mod10 = value - 10 * div10

        return "$div10$mod10"
    }
}
