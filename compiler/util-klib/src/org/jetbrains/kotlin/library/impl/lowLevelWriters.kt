/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.SerializedDeclaration
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import java.io.ByteArrayOutputStream
import java.io.DataOutput
import java.io.DataOutputStream
import java.io.FileOutputStream

sealed class IrDataWriter {
    protected abstract fun writeData(dataOutput: DataOutput)

    fun writeIntoFile(path: String) {
        FileOutputStream(path).use { fos ->
            DataOutputStream(fos).use { dos -> writeData(dos) }
        }
    }

    fun writeIntoMemory(): ByteArray {
        return ByteArrayOutputStream().also { baos ->
            DataOutputStream(baos).use { dos -> writeData(dos) }
        }.toByteArray()
    }
}

class IrArrayWriter(private val data: List<ByteArray>, private val useVarInt: Boolean) : IrDataWriter() {
    override fun writeData(dataOutput: DataOutput) {
        if (useVarInt) {
            // Designate that var-int encoding is used for sizes of elements by writing the number of elements as negative number.
            dataOutput.writeInt(-data.size)
        } else {
            dataOutput.writeInt(data.size)
        }

        data.forEach {
            if (useVarInt) {
                dataOutput.writeVarInt(it.size.toUInt())
            } else {
                dataOutput.writeInt(it.size)
            }
        }
        data.forEach { dataOutput.write(it) }
    }
}

class IrStringWriter(private val data: List<String>, private val useVarInt: Boolean) : IrDataWriter() {
    override fun writeData(dataOutput: DataOutput) {
        if (useVarInt) {
            // Designate that var-int encoding is used for sizes of elements by writing the number of elements as negative number.
            dataOutput.writeInt(-data.size)
        } else {
            dataOutput.writeInt(data.size)
        }

        val transformedData = data.map(WobblyTF8::encode)

        transformedData.forEach {
            if (useVarInt) {
                dataOutput.writeVarInt(it.size.toUInt())
            } else {
                dataOutput.writeInt(it.size)
            }
        }
        transformedData.forEach { dataOutput.write(it) }
    }
}

class IrDeclarationWriter(private val declarations: List<SerializedDeclaration>) : IrDataWriter() {
    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(declarations.size)

        var dataOffset = INDEX_HEADER_SIZE + SINGLE_INDEX_RECORD_SIZE * declarations.size

        for (d in declarations) {
            dataOutput.writeInt(d.id)
            dataOutput.writeInt(dataOffset)
            dataOutput.writeInt(d.size)
            dataOffset += d.size
        }

        for (d in declarations) {
            dataOutput.write(d.bytes)
        }
    }

    companion object {
        private const val SINGLE_INDEX_RECORD_SIZE = 3 * Int.SIZE_BYTES
        private const val INDEX_HEADER_SIZE = Int.SIZE_BYTES
    }
}

private fun DataOutput.writeVarInt(value: UInt) {
    // Taken from Android source, Apache licensed
    var v = value
    var remaining = v shr 7
    while (remaining != 0u) {
        val byte = (v and 0x7fu) or 0x80u
        writeByte(byte.toInt())
        v = remaining
        remaining = remaining shr 7
    }
    val byte = v and 0x7fu
    writeByte(byte.toInt())
}