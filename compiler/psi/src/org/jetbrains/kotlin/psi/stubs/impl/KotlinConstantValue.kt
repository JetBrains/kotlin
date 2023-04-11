/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

enum class KotlinConstantValueKind {
    NULL, BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, DOUBLE, FLOAT, ENUM, KCLASS, STRING, ARRAY, UBYTE, USHORT, UINT, ULONG, ANNO;
}

fun createConstantValue(dataStream: StubInputStream): ConstantValue<*>? {
    val kind = dataStream.readInt()
    if (kind == -1) return null
    return when (KotlinConstantValueKind.values()[kind]) {
        KotlinConstantValueKind.NULL -> NullValue
        KotlinConstantValueKind.BOOLEAN -> BooleanValue(dataStream.readBoolean())
        KotlinConstantValueKind.CHAR -> CharValue(dataStream.readChar())
        KotlinConstantValueKind.BYTE -> ByteValue(dataStream.readByte())
        KotlinConstantValueKind.SHORT -> ShortValue(dataStream.readShort())
        KotlinConstantValueKind.INT -> IntValue(dataStream.readInt())
        KotlinConstantValueKind.LONG -> LongValue(dataStream.readLong())
        KotlinConstantValueKind.DOUBLE -> DoubleValue(dataStream.readDouble())
        KotlinConstantValueKind.FLOAT -> FloatValue(dataStream.readFloat())
        KotlinConstantValueKind.ENUM -> EnumValue(
            StubUtils.deserializeClassId(dataStream)!!,
            Name.identifier(dataStream.readNameString()!!)
        )
        KotlinConstantValueKind.KCLASS -> KClassValue(StubUtils.deserializeClassId(dataStream)!!, dataStream.readInt())
        KotlinConstantValueKind.STRING -> StringValue(dataStream.readNameString()!!)
        KotlinConstantValueKind.ARRAY -> {
            val arraySize = dataStream.readInt() - 1
            ArrayValue((0..arraySize).map {
                createConstantValue(dataStream)!!
            })
        }
        KotlinConstantValueKind.UBYTE -> UByteValue(dataStream.readByte())
        KotlinConstantValueKind.USHORT -> UShortValue(dataStream.readShort())
        KotlinConstantValueKind.UINT -> UIntValue(dataStream.readInt())
        KotlinConstantValueKind.ULONG -> ULongValue(dataStream.readLong())
        KotlinConstantValueKind.ANNO -> {
            val classId = StubUtils.deserializeClassId(dataStream)!!
            val numberOfArgs = dataStream.readInt() - 1
            AnnotationValue.create(KotlinClassTypeBean(classId, emptyList(), false), (0..numberOfArgs).associate {
                Name.identifier(dataStream.readNameString()!!) to createConstantValue(dataStream)!!
            })
        }
    }
}


fun serialize(constantValue: ConstantValue<*>, dataStream: StubOutputStream) {
    constantValue.accept(KotlinConstantValueSerializationVisitor(dataStream), null)
}

class KotlinConstantValueSerializationVisitor(private val dataStream: StubOutputStream) :
    AnnotationArgumentVisitor<Unit, Nothing?>() {
    override fun visitArrayValue(value: ArrayValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.ARRAY.ordinal)
        dataStream.writeInt(value.value.size)
        for (constantValue in value.value) {
            constantValue.accept(this, data)
        }
    }

    override fun visitBooleanValue(value: BooleanValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.BOOLEAN.ordinal)
        dataStream.writeBoolean(value.value)
    }

    override fun visitByteValue(value: ByteValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.BYTE.ordinal)
        dataStream.writeByte(value.value.toInt())
    }

    override fun visitCharValue(value: CharValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.CHAR.ordinal)
        dataStream.writeChar(value.value.code)
    }

    override fun visitShortValue(value: ShortValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.SHORT.ordinal)
        dataStream.writeShort(value.value.toInt())
    }

    override fun visitIntValue(value: IntValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.INT.ordinal)
        dataStream.writeInt(value.value)
    }

    override fun visitLongValue(value: LongValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.LONG.ordinal)
        dataStream.writeLong(value.value)
    }

    override fun visitDoubleValue(value: DoubleValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.DOUBLE.ordinal)
        dataStream.writeDouble(value.value)
    }

    override fun visitFloatValue(value: FloatValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.FLOAT.ordinal)
        dataStream.writeFloat(value.value)
    }

    override fun visitEnumValue(value: EnumValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.ENUM.ordinal)
        StubUtils.serializeClassId(dataStream, value.enumClassId)
        dataStream.writeName(value.enumEntryName.identifier)
    }

    override fun visitKClassValue(value: KClassValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.KCLASS.ordinal)
        val normalClass = value.value as KClassValue.Value.NormalClass
        StubUtils.serializeClassId(dataStream, normalClass.classId)
        dataStream.writeInt(normalClass.arrayDimensions)
    }

    override fun visitNullValue(value: NullValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.NULL.ordinal)
    }
    override fun visitStringValue(value: StringValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.STRING.ordinal)
        dataStream.writeName(value.value)
    }

    override fun visitUByteValue(value: UByteValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.UBYTE.ordinal)
        dataStream.writeByte(value.value.toInt())
    }

    override fun visitUShortValue(value: UShortValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.USHORT.ordinal)
        dataStream.writeShort(value.value.toInt())
    }

    override fun visitUIntValue(value: UIntValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.UINT.ordinal)
        dataStream.writeInt(value.value)
    }

    override fun visitULongValue(value: ULongValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.ULONG.ordinal)
        dataStream.writeLong(value.value)
    }

    override fun visitAnnotationValue(value: AnnotationValue, data: Nothing?) {
        dataStream.writeInt(KotlinConstantValueKind.ANNO.ordinal)
        StubUtils.serializeClassId(dataStream, (value.value.type as KotlinClassTypeBean).classId)
        val args = value.value.argumentsMapping
        dataStream.writeInt(args.size)
        for (arg in args) {
            dataStream.writeName(arg.key.asString())
            arg.value.accept(this, data)
        }
    }

    override fun visitErrorValue(value: ErrorValue, data: Nothing?) {
        error("Error values should not be reachable in compiled code")
    }
}

data class AnnotationData(val annoClassId: ClassId, val args: Map<Name, ConstantValue<*>>)
data class EnumData(val enumClassId: ClassId, val enumEntryName: Name)
data class KClassData(val classId: ClassId, val arrayNestedness: Int)
fun createConstantValue(value: Any?): ConstantValue<*> {
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
        is ByteArray -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is ShortArray -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is IntArray -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is LongArray -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is CharArray -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is FloatArray -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is DoubleArray -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is BooleanArray -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is Array<*> -> ArrayValue(value.map { createConstantValue(it) }.toList())
        is EnumData -> EnumValue(value.enumClassId, value.enumEntryName)
        is KClassData -> KClassValue(value.classId, value.arrayNestedness)
        is AnnotationData -> AnnotationValue.create(KotlinClassTypeBean(value.annoClassId, emptyList(), false), value.args)
        null -> NullValue
        else -> error("Unsupported value $value")
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
                value.annotation.argumentList.associate { nameResolver.getName(it.nameId) to createConstantValue(it.value, nameResolver) }
            AnnotationValue.create(KotlinClassTypeBean(nameResolver.getClassId(value.annotation.id), emptyList(), false), args)
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

