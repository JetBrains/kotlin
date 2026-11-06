/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.metadata.ExtensionRegistryLite
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.java.JavaClassProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import java.io.DataInput
import java.io.DataOutput

class SerializedJavaClass(
        val proto: ProtoBuf.Class,
        val stringTable: ProtoBuf.StringTable,
        val qualifiedNameTable: ProtoBuf.QualifiedNameTable
) {
    val classId: ClassId
        get() = NameResolverImpl(stringTable, qualifiedNameTable).getClassId(proto.fqName)
}

fun SerializedJavaClass.toProtoData() = ClassProtoData(proto, NameResolverImpl(stringTable, qualifiedNameTable))

val JAVA_CLASS_PROTOBUF_REGISTRY =
    ExtensionRegistryLite {
        JavaClassProtoBuf.registerAllExtensions(this)
        // Built-ins extensions are used for annotations' serialization
        BuiltInsProtoBuf.registerAllExtensions(this)
    }

object JavaClassProtoMapValueExternalizer : DataExternalizer<SerializedJavaClass> {
    override fun save(output: DataOutput, value: SerializedJavaClass) {
        output.writeBytesWithSize(value.proto.toByteArray())
        output.writeBytesWithSize(value.stringTable.toByteArray())
        output.writeBytesWithSize(value.qualifiedNameTable.toByteArray())
    }

    private fun DataOutput.writeBytesWithSize(bytes: ByteArray) {
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInput.readBytesWithSize(): ByteArray {
        val bytesLength = readInt()
        return ByteArray(bytesLength).also {
            readFully(it, 0, bytesLength)
        }
    }

    override fun read(input: DataInput): SerializedJavaClass {
        val proto = ProtoBuf.Class.parseFrom(input.readBytesWithSize(), JAVA_CLASS_PROTOBUF_REGISTRY)
        val stringTable = ProtoBuf.StringTable.parseFrom(input.readBytesWithSize(), JAVA_CLASS_PROTOBUF_REGISTRY)
        val qualifiedNameTable = ProtoBuf.QualifiedNameTable.parseFrom(input.readBytesWithSize(), JAVA_CLASS_PROTOBUF_REGISTRY)

        return SerializedJavaClass(proto, stringTable, qualifiedNameTable)
    }
}
