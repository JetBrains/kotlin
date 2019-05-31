/**
 * Copyright 2010-2019 JetBrains s.r.o.
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

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

data class DeclarationId(val id: Long, val isLocal: Boolean)

class CombinedIrFileReader(file: File) {
    private val buffer = file.map(FileChannel.MapMode.READ_ONLY)
    private val declarationToOffsetSize = mutableMapOf<DeclarationId, Pair<Int, Int>>()

    init {
        val declarationsCount = buffer.int
        for (i in 0 until declarationsCount) {
            val id = buffer.long
            val isLocal = buffer.int != 0
            val offset = buffer.int
            val size = buffer.int
            declarationToOffsetSize[DeclarationId(id, isLocal)] = offset to size
        }
    }

    fun declarationBytes(id: DeclarationId): ByteArray {
        val offsetSize = declarationToOffsetSize[id] ?: throw Error("No declaration with $id here")
        val result = ByteArray(offsetSize.second)
        buffer.position(offsetSize.first)
        buffer.get(result, 0, offsetSize.second)
        return result
    }
}

private const val SINGLE_INDEX_RECORD_SIZE = 20  // sizeof(Long) + 3 * sizeof(Int).
private const val INDEX_HEADER_SIZE = 4  // sizeof(Int).

class CombinedIrFileWriter(val declarationCount: Int) {
    private var currentDeclaration = 0
    private var currentPosition = 0
    private val file = org.jetbrains.kotlin.konan.file.createTempFile("ir").deleteOnExit()
    private val randomAccessFile = RandomAccessFile(file.path, "rw")

    init {
        randomAccessFile.writeInt(declarationCount)
        assert(randomAccessFile.filePointer.toInt() == INDEX_HEADER_SIZE)
        for (i in 0 until declarationCount) {
            randomAccessFile.writeLong(-1) // id
            randomAccessFile.writeInt(-1)  // isLocal
            randomAccessFile.writeInt(-1)  // offset
            randomAccessFile.writeInt(-1)  // size
        }
        currentPosition = randomAccessFile.filePointer.toInt()
        assert(currentPosition == INDEX_HEADER_SIZE + SINGLE_INDEX_RECORD_SIZE * declarationCount)
    }

    fun skipDeclaration() {
        currentDeclaration++
    }

    fun addDeclaration(id: DeclarationId, bytes: ByteArray) {
        randomAccessFile.seek((currentDeclaration * SINGLE_INDEX_RECORD_SIZE + INDEX_HEADER_SIZE).toLong())
        randomAccessFile.writeLong(id.id)
        randomAccessFile.writeInt(if (id.isLocal) 1 else 0)
        randomAccessFile.writeInt(currentPosition)
        randomAccessFile.writeInt(bytes.size)
        randomAccessFile.seek(currentPosition.toLong())
        randomAccessFile.write(bytes)
        assert(randomAccessFile.filePointer < Int.MAX_VALUE.toLong())
        currentPosition = randomAccessFile.filePointer.toInt()
        currentDeclaration++
    }

    fun finishWriting(): File {
        assert(currentDeclaration == declarationCount)
        randomAccessFile.close()
        return file
    }
}

