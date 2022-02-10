/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import java.nio.ByteBuffer

abstract class IrArrayReader(private val buffer: ReadBuffer) {
    private val indexToOffset: IntArray

    fun entryCount() = indexToOffset.size - 1

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
        buffer.position = offset
        buffer.get(result, 0, size)
        return result
    }
}

class IrArrayFileReader(file: File) : IrArrayReader(ReadBuffer.WeakFileBuffer(file.javaFile()))
class IrArrayMemoryReader(bytes: ByteArray) : IrArrayReader(ReadBuffer.MemoryBuffer(bytes))

class IrIntArrayMemoryReader(bytes: ByteArray) {

    val array = run {
        val buffer = ReadBuffer.MemoryBuffer(bytes)

        val result = IntArray(buffer.int)

        for (i in result.indices) {
            result[i] = buffer.int
        }

        result
    }
}

class IrLongArrayMemoryReader(bytes: ByteArray) {

    val array = run {
        val buffer = ReadBuffer.MemoryBuffer(bytes)

        val result = LongArray(buffer.int)

        for (i in result.indices) {
            result[i] = buffer.long
        }

        result
    }
}

abstract class IrMultiArrayReader(val buffer: ReadBuffer) {
    private val indexToOffset: IntArray
    private val indexIndexToOffset = mutableMapOf<Int, IntArray>()

    private fun readOffsets(position: Int): IntArray {
        buffer.position = position
        val count = buffer.int
        val result = IntArray(count + 1)
        result[0] = 4 * (count + 1)
        for (i in 0 until count) {
            val size = buffer.int
            result[i + 1] = result[i] + size
        }

        return result
    }

    init {
        indexToOffset = readOffsets(0)
    }

    fun tableItemBytes(id: Int): ByteArray {
        val offset = indexToOffset[id]
        val size = indexToOffset[id + 1] - offset
        val result = ByteArray(size)
        buffer.position = offset
        buffer.get(result, 0, size)
        return result
    }

    fun tableItemBytes(row: Int, column: Int): ByteArray {
        val rowOffset = indexToOffset[row]

        val columnOffsets = indexIndexToOffset.getOrPut(row) {
            readOffsets(rowOffset)
        }

        val dataOffset = columnOffsets[column]
        val dataSize = columnOffsets[column + 1] - dataOffset
        val result = ByteArray(dataSize)

        buffer.position = rowOffset + dataOffset
        buffer.get(result, 0, dataSize)

        return result
    }
}

class IrMultiArrayFileReader(val file: File) : IrMultiArrayReader(ReadBuffer.WeakFileBuffer(file.javaFile()))
class IrMultiArrayMemoryReader(bytes: ByteArray) : IrMultiArrayReader(ReadBuffer.MemoryBuffer(bytes))

abstract class IrMultiTableReader<K>(private val buffer: ReadBuffer, private val keyReader: ReadBuffer.() -> K) {
    private val indexToOffset: IntArray
    private val indexToIndexMap = mutableMapOf<Int, Map<K, Pair<Int, Int>>>()

    private fun readOffsets(position: Int): IntArray {
        buffer.position = position
        val count = buffer.int
        val result = IntArray(count + 1)
        result[0] = 4 * (count + 1)
        for (i in 0 until count) {
            val size = buffer.int
            result[i + 1] = result[i] + size
        }

        return result
    }

    init {
        indexToOffset = readOffsets(0)
    }

    private fun readIndexMap(position: Int): Map<K, Pair<Int, Int>> {
        buffer.position = position
        val result = mutableMapOf<K, Pair<Int, Int>>()

        val count = buffer.int

        for (i in 0 until count) {
            val key = keyReader(buffer)
            val offset = buffer.int
            val size = buffer.int

            result[key] = offset to size
        }

        return result
    }

    fun tableItemBytes(idx: Int): ByteArray {
        val rowOffset = indexToOffset[idx]
        val nextOffset = indexToOffset[idx + 1]
        val size = nextOffset - rowOffset
        val result = ByteArray(size)
        buffer.position = rowOffset
        buffer.get(result, 0, size)
        return result
    }

    fun tableItemBytes(row: Int, id: K): ByteArray {

        val rowOffset = indexToOffset[row]

        val indexToMap = indexToIndexMap.getOrPut(row) {
            readIndexMap(rowOffset)
        }

        val coordinates = indexToMap[id] ?: error("No coordinates found for $id")
        val offset = coordinates.first
        val size = coordinates.second
        val result = ByteArray(size)
        buffer.position = rowOffset + offset
        buffer.get(result, 0, size)
        return result
    }
}

abstract class IrTableReader<K>(private val buffer: ReadBuffer, keyReader: ReadBuffer.() -> K) {
    private val indexToOffset = mutableMapOf<K, Pair<Int, Int>>()

    init {
        val count = buffer.int
        for (i in 0 until count) {
            val key = keyReader(buffer)
            val offset = buffer.int
            val size = buffer.int

            indexToOffset[key] = offset to size
        }
    }

    fun tableItemBytes(id: K): ByteArray {
        val coordinates = indexToOffset[id] ?: error("No coordinates found for $id")
        val offset = coordinates.first
        val size = coordinates.second
        val result = ByteArray(size)
        buffer.position = offset
        buffer.get(result, 0, size)
        return result
    }
}

val ByteArray.buffer: ByteBuffer get() = ByteBuffer.wrap(this)

fun File.javaFile(): java.io.File = java.io.File(path)

class IndexIrTableFileReader(file: File) : IrTableReader<Long>(ReadBuffer.WeakFileBuffer(file.javaFile()), { long })
class IndexIrTableMemoryReader(bytes: ByteArray) : IrTableReader<Long>(ReadBuffer.MemoryBuffer(bytes), { long })

data class DeclarationId(val id: Int)

class DeclarationIrTableFileReader(file: File) :
    IrTableReader<DeclarationId>(ReadBuffer.WeakFileBuffer(file.javaFile()), { DeclarationId(int) })

class DeclarationIrTableMemoryReader(bytes: ByteArray) :
    IrTableReader<DeclarationId>(ReadBuffer.MemoryBuffer(bytes), { DeclarationId(int) })

class DeclarationIrMultiTableFileReader(file: File) :
    IrMultiTableReader<DeclarationId>(ReadBuffer.WeakFileBuffer(file.javaFile()), { DeclarationId(int) })

class DeclarationIrMultiTableMemoryReader(bytes: ByteArray) :
    IrMultiTableReader<DeclarationId>(ReadBuffer.MemoryBuffer(bytes), { DeclarationId(int) })


fun IrArrayReader.toArray(): Array<ByteArray> = Array(this.entryCount()) { i -> this.tableItemBytes(i) }