/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.StubUtils

enum class ConstantValueKind {
    NULL, BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, DOUBLE, FLOAT, ENUM, KCLASS, STRING, ARRAY, UBYTE, USHORT, UINT, ULONG, ANNO;
}

sealed class ConstantValue<out T>(open val value: T, val constantValueKind: ConstantValueKind) {
    fun getKind(): Int {
        return constantValueKind.ordinal
    }

    override fun toString(): String {
        return value.toString()
    }

    abstract fun serializeValue(dataStream: StubOutputStream)

    companion object {
        fun createConstantValue(dataStream: StubInputStream): ConstantValue<*>? {
            val kind = dataStream.readInt()
            if (kind == -1) return null
            return when (ConstantValueKind.values()[kind]) {
                ConstantValueKind.NULL -> NullValue
                ConstantValueKind.BOOLEAN -> BooleanValue(dataStream.readBoolean())
                ConstantValueKind.CHAR -> CharValue(dataStream.readChar())
                ConstantValueKind.BYTE -> ByteValue(dataStream.readByte())
                ConstantValueKind.SHORT -> ShortValue(dataStream.readShort())
                ConstantValueKind.INT -> IntValue(dataStream.readInt())
                ConstantValueKind.LONG -> LongValue(dataStream.readLong())
                ConstantValueKind.DOUBLE -> DoubleValue(dataStream.readDouble())
                ConstantValueKind.FLOAT -> FloatValue(dataStream.readFloat())
                ConstantValueKind.ENUM -> EnumValue(
                    StubUtils.deserializeClassId(dataStream)!!,
                    Name.identifier(dataStream.readNameString()!!)
                )
                ConstantValueKind.KCLASS -> KClassValue(StubUtils.deserializeClassId(dataStream)!!, dataStream.readInt())
                ConstantValueKind.STRING -> StringValue(dataStream.readNameString()!!)
                ConstantValueKind.ARRAY -> {
                    val arraySize = dataStream.readInt() - 1
                    ArrayValue((0..arraySize).map {
                        createConstantValue(dataStream)!!
                    })
                }
                ConstantValueKind.UBYTE -> UByteValue(dataStream.readByte())
                ConstantValueKind.USHORT -> UShortValue(dataStream.readShort())
                ConstantValueKind.UINT -> UIntValue(dataStream.readInt())
                ConstantValueKind.ULONG -> ULongValue(dataStream.readLong())
                ConstantValueKind.ANNO -> {
                    val classId = StubUtils.deserializeClassId(dataStream)!!
                    val numberOfArgs = dataStream.readInt() - 1
                    AnnotationValue(classId, (0..numberOfArgs).map {
                        createConstantValue(dataStream)!!
                    })
                }
            }
        }
    }
}
abstract class IntegerValueConstant<out T> protected constructor(value: T, constantValueKind: ConstantValueKind) : ConstantValue<T>(value, constantValueKind)
abstract class UnsignedValueConstant<out T> protected constructor(value: T, constantValueKind: ConstantValueKind) : ConstantValue<T>(value, constantValueKind)

class ArrayValue(
    value: List<ConstantValue<*>>,
) : ConstantValue<List<ConstantValue<*>>>(value, ConstantValueKind.ARRAY) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeInt(value.size)
        for (constantValue in value) {
            dataStream.writeInt(constantValue.getKind())
            constantValue.serializeValue(dataStream)
        }
    }

    override fun toString(): String {
        return value.joinToString(", ", "[", "]") { it.toString() }
    }
}

class BooleanValue(value: Boolean) : ConstantValue<Boolean>(value, ConstantValueKind.BOOLEAN) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeBoolean(value)
    }
}

class ByteValue(value: Byte) : IntegerValueConstant<Byte>(value, ConstantValueKind.BYTE) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeByte(value.toInt())
    }
}

class CharValue(value: Char) : IntegerValueConstant<Char>(value, ConstantValueKind.CHAR) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeChar(value.code)
    }
}

class ShortValue(value: Short) : IntegerValueConstant<Short>(value, ConstantValueKind.SHORT) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeShort(value.toInt())
    }
}

class IntValue(value: Int) : IntegerValueConstant<Int>(value, ConstantValueKind.INT) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeInt(value)
    }
}

class LongValue(value: Long) : IntegerValueConstant<Long>(value, ConstantValueKind.LONG) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeLong(value)
    }
}

class DoubleValue(value: Double) : ConstantValue<Double>(value, ConstantValueKind.DOUBLE) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeDouble(value)
    }
}

class FloatValue(value: Float) : ConstantValue<Float>(value, ConstantValueKind.FLOAT) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeFloat(value)
    }
}

data class EnumData(val enumClassId: ClassId, val enumEntryName: Name)

class EnumValue(
    val enumClassId: ClassId,
    val enumEntryName: Name
) : ConstantValue<Pair<ClassId, Name>>(enumClassId to enumEntryName, ConstantValueKind.ENUM) {
    override fun serializeValue(dataStream: StubOutputStream) {
        StubUtils.serializeClassId(dataStream, enumClassId)
        dataStream.writeName(enumEntryName.identifier)
    }
}

data class KClassData(val classId: ClassId, val arrayNestedness: Int)
class KClassValue(classId: ClassId, arrayNestedness: Int) : ConstantValue<Pair<ClassId, Int>>(classId to arrayNestedness, ConstantValueKind.KCLASS) {
    override fun serializeValue(dataStream: StubOutputStream) {
        StubUtils.serializeClassId(dataStream, value.first)
        dataStream.writeInt(value.second)
    }
}

object NullValue : ConstantValue<Nothing?>(null, ConstantValueKind.NULL) {
    override fun serializeValue(dataStream: StubOutputStream) {}
}

class StringValue(value: String) : ConstantValue<String>(value, ConstantValueKind.STRING) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeName(value)
    }
}

class UByteValue(byteValue: Byte) : UnsignedValueConstant<Byte>(byteValue, ConstantValueKind.UBYTE) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeByte(value.toInt())
    }
}

class UShortValue(shortValue: Short) : UnsignedValueConstant<Short>(shortValue, ConstantValueKind.USHORT) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeShort(value.toInt())
    }
}

class UIntValue(intValue: Int) : UnsignedValueConstant<Int>(intValue, ConstantValueKind.UINT) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeInt(value)
    }
}

class ULongValue(longValue: Long) : UnsignedValueConstant<Long>(longValue, ConstantValueKind.ULONG) {
    override fun serializeValue(dataStream: StubOutputStream) {
        dataStream.writeLong(value)
    }
}

data class AnnotationData(val annoClassId: ClassId, val args: List<ConstantValue<*>>)
class AnnotationValue(val annoClassId: ClassId, val args: List<ConstantValue<*>>) :
    ConstantValue<Pair<ClassId, List<ConstantValue<*>>>>(annoClassId to args, ConstantValueKind.ANNO) {

    override fun toString(): String {
        return args.joinToString(", ", "${annoClassId.asFqNameString()}[", "]") { it.toString() }
    }

    override fun serializeValue(dataStream: StubOutputStream) {
        StubUtils.serializeClassId(dataStream, annoClassId)
        dataStream.writeInt(args.size)
        for (arg in args) {
            dataStream.writeInt(arg.getKind())
            arg.serializeValue(dataStream)
        }
    }
}

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
        is ByteArray -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is ShortArray -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is IntArray -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is LongArray -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is CharArray -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is FloatArray -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is DoubleArray -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is BooleanArray -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is Array<*> -> ArrayValue(value.map { createConstantValue(it)!! }.toList())
        is EnumData -> EnumValue(value.enumClassId, value.enumEntryName)
        is KClassData -> KClassValue(value.classId, value.arrayNestedness)
        is AnnotationData -> AnnotationValue(value.annoClassId, value.args)
        null -> NullValue
        else -> null
    }
}

fun createConstantValue(value: ProtoBuf.Annotation.Argument.Value, nameResolver: NameResolver): ConstantValue<*> {
    val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

    fun <T, R> T.letIf(predicate: Boolean, f: (T) -> R, g: (T) -> R): R =
        if (predicate) f(this) else g(this)

    return when (value.type) {
        ProtoBuf.Annotation.Argument.Value.Type.BYTE -> value.intValue.toByte().letIf(isUnsigned, ::UByteValue, ::ByteValue)
        ProtoBuf.Annotation.Argument.Value.Type.CHAR -> CharValue(value.intValue.toInt().toChar())
        ProtoBuf.Annotation.Argument.Value.Type.SHORT -> value.intValue.toShort().letIf(isUnsigned, ::UShortValue, ::ShortValue)
        ProtoBuf.Annotation.Argument.Value.Type.INT -> value.intValue.toInt().letIf(isUnsigned, ::UIntValue, ::IntValue)
        ProtoBuf.Annotation.Argument.Value.Type.LONG -> value.intValue.letIf(isUnsigned, ::ULongValue, ::LongValue)
        ProtoBuf.Annotation.Argument.Value.Type.FLOAT -> FloatValue(value.floatValue)
        ProtoBuf.Annotation.Argument.Value.Type.DOUBLE -> DoubleValue(value.doubleValue)
        ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN -> BooleanValue(value.intValue != 0L)
        ProtoBuf.Annotation.Argument.Value.Type.STRING -> StringValue(nameResolver.getString(value.stringValue))
        ProtoBuf.Annotation.Argument.Value.Type.CLASS -> KClassValue(nameResolver.getClassId(value.classId), value.arrayDimensionCount)
        ProtoBuf.Annotation.Argument.Value.Type.ENUM -> EnumValue(
            nameResolver.getClassId(value.classId),
            nameResolver.getName(value.enumValueId)
        )
        ProtoBuf.Annotation.Argument.Value.Type.ANNOTATION -> {
            val args =
                value.annotation.argumentList.map { createConstantValue(it.value, nameResolver) }
            AnnotationValue(nameResolver.getClassId(value.annotation.id), args)
        }
        ProtoBuf.Annotation.Argument.Value.Type.ARRAY -> ArrayValue(
            value.arrayElementList.map { createConstantValue(it, nameResolver) }
        )
        else -> error("Unsupported annotation argument type: ${value.type}")
    }
}
private fun NameResolver.getClassId(index: Int): ClassId {
    return ClassId.fromString(getQualifiedClassName(index), isLocalClassName(index))
}

private fun NameResolver.getName(index: Int): Name =
    Name.guessByFirstCharacter(getString(index))

