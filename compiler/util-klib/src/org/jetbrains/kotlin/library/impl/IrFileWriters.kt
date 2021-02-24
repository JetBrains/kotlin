/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.SerializedDeclaration
import java.io.ByteArrayOutputStream
import java.io.DataOutput
import java.io.DataOutputStream
import java.io.FileOutputStream

abstract class IrFileWriter {

    protected abstract fun writeData(dataOutput: DataOutput)

    fun writeIntoFile(path: String) {
        val fileStream = FileOutputStream(path)
        val dataOutputStream = DataOutputStream(fileStream)

        writeData(dataOutputStream)

        dataOutputStream.close()
        fileStream.close()
    }
}

abstract class IrMemoryWriter {

    protected abstract fun writeData(dataOutput: DataOutput)

    fun writeIntoMemory(): ByteArray {
        val memoryStream = ByteArrayOutputStream()
        val dataOutputStream = DataOutputStream(memoryStream)

        writeData(dataOutputStream)

        dataOutputStream.close()
        memoryStream.close()

        return memoryStream.toByteArray()
    }
}


class IrArrayWriter(private val data: List<ByteArray>) : IrFileWriter() {
    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(data.size)

        data.forEach { dataOutput.writeInt(it.size) }
        data.forEach { dataOutput.write(it) }
    }
}

class IrMemoryArrayWriter(private val data: List<ByteArray>) : IrMemoryWriter() {
    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(data.size)

        data.forEach { dataOutput.writeInt(it.size) }
        data.forEach { dataOutput.write(it) }
    }
}


class IrByteArrayWriter(private val data: List<ByteArray>) : IrFileWriter() {
    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(data.size)

        data.forEach { dataOutput.writeInt(it.size) }
        data.forEach { dataOutput.write(it) }
    }
}

class IrTableWriter(private val data: List<Pair<Long, ByteArray>>) : IrFileWriter() {
    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(data.size)

        var dataOffset = Int.SIZE_BYTES + data.size * (Long.SIZE_BYTES + 2 * Int.SIZE_BYTES)

        data.forEach {
            dataOutput.writeLong(it.first)
            dataOutput.writeInt(dataOffset)
            dataOutput.writeInt(it.second.size)
            dataOffset += it.second.size
        }

        data.forEach { dataOutput.write(it.second) }
    }
}

class IrDeclarationWriter(private val declarations: List<SerializedDeclaration>) : IrFileWriter() {

    private val SINGLE_INDEX_RECORD_SIZE = 3 * Int.SIZE_BYTES
    private val INDEX_HEADER_SIZE = Int.SIZE_BYTES

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

}

class IrMemoryDeclarationWriter(private val declarations: List<SerializedDeclaration>) : IrMemoryWriter() {

    private val SINGLE_INDEX_RECORD_SIZE = 3 * Int.SIZE_BYTES
    private val INDEX_HEADER_SIZE = Int.SIZE_BYTES

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

}