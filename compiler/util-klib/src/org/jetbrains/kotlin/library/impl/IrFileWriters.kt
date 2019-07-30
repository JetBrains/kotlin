/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

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

class IrTableWriter(private val data: List<ByteArray>) : IrFileWriter() {
    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(data.size)

        data.forEach { dataOutput.writeInt(it.size) }
        data.forEach { dataOutput.write(it) }
    }
}

sealed class SerializedDeclaration {
    abstract val id: Long
    abstract val local: Int
    abstract val size: Int
    abstract val bytes: ByteArray

    var offset: Int = -1
}

class TopLevelDeclaration(override val id: Long, isLocal: Boolean, override val bytes: ByteArray) : SerializedDeclaration() {
    override val local = if (isLocal) 1 else 0
    override val size = bytes.size
}

object SkippedDeclaration : SerializedDeclaration() {
    override val id = -1L
    override val local = -1
    override val size = 0
    override val bytes = ByteArray(0)
}

class IrDeclarationWriter(private val declarations: List<SerializedDeclaration>) : IrFileWriter() {

    private val SINGLE_INDEX_RECORD_SIZE = 20  // sizeof(Long) + 3 * sizeof(Int).
    private val INDEX_HEADER_SIZE = 4  // sizeof(Int).

    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(declarations.size)

        var dataOffset = INDEX_HEADER_SIZE + SINGLE_INDEX_RECORD_SIZE * declarations.size

        for (d in declarations) {
            d.offset = dataOffset
            dataOffset += d.size
        }

        for (d in declarations) {
            dataOutput.writeLong(d.id)
            dataOutput.writeInt(d.local)
            dataOutput.writeInt(d.offset)
            dataOutput.writeInt(d.size)
        }

        for (d in declarations) {
            dataOutput.write(d.bytes)
        }
    }

}