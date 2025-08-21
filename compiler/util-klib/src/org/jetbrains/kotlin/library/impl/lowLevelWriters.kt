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

class IrArrayWriter(private val data: List<ByteArray>) : IrDataWriter() {
    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(data.size)

        data.forEach { dataOutput.writeInt(it.size) }
        data.forEach { dataOutput.write(it) }
    }
}

class IrStringWriter(private val data: List<String>) : IrDataWriter() {
    override fun writeData(dataOutput: DataOutput) {
        dataOutput.writeInt(data.size)

        val transformedData = data.map(WobblyTF8::encode)

        transformedData.forEach { dataOutput.writeInt(it.size) }
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