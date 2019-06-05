/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Files

class SimpleIrTableFileReader(file: File) {
    private val buffer = file.map(FileChannel.MapMode.READ_ONLY)
    private val indexToOffset: IntArray

    init {
        val count = buffer.int
        indexToOffset = IntArray(count + 1)
        indexToOffset[0] = 4 * (count + 1)
        for (i in 0 until count) {
            val size = buffer.int
            indexToOffset[i + 1] = indexToOffset[i] + size
        }
    }

    fun tableItemBytes(id: Int): ByteArray {
        val offset = indexToOffset[id]
        val size = indexToOffset[id + 1] - offset
        val result = ByteArray(size)
        buffer.position(offset)
        buffer.get(result, 0, size)
        return result
    }
}

class SimpleIrTableFileWriter(val tableName: String, val itemsCount: Int) {
    private var currentItemIndex = 0
    private var currentPosition = 0
    private val file = Files.createTempFile(tableName, "").toFile()
    private val randomAccessFile = RandomAccessFile(file.path, "rw")

    init {
        randomAccessFile.writeInt(itemsCount)
        assert(randomAccessFile.filePointer.toInt() == 4)
        for (i in 0 until itemsCount) {
            randomAccessFile.writeInt(-1)  // size
        }
        currentPosition = randomAccessFile.filePointer.toInt()
        assert(currentPosition == 4 + 4 * itemsCount)
    }

    fun addItem(bytes: ByteArray) {
        randomAccessFile.seek((currentItemIndex * 4 + 4).toLong())
        randomAccessFile.writeInt(bytes.size)
        randomAccessFile.seek(currentPosition.toLong())
        randomAccessFile.write(bytes)
        assert(randomAccessFile.filePointer < Int.MAX_VALUE.toLong())
        currentPosition = randomAccessFile.filePointer.toInt()
        currentItemIndex++
    }

    fun finishWriting(): java.io.File {
        assert(currentItemIndex == itemsCount) { "Expected: $itemsCount, but was: $currentItemIndex" }
        randomAccessFile.close()
        return file
    }
}