/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibraryLayout
import java.nio.ByteBuffer


/******************************************************************************/
/** [ByteArray] readers                                                       */
/******************************************************************************/

/** Read directly from a byte array. */
fun IrArrayReader(bytes: ByteArray): IrArrayReader = IrArrayReader(ReadBuffer.MemoryBuffer(bytes))

/** On-demand read from a byte array that will be loaded on the first access. */
fun IrArrayReader(loadBytes: () -> ByteArray): IrArrayReader = IrArrayReader(ReadBuffer.OnDemandMemoryBuffer(loadBytes))

/** On-demand read from a file (potentially inside a KLIB archive file). */
fun <L : KotlinLibraryLayout> IrArrayReader(
    access: BaseLibraryAccess<L>,
    getFile: L.() -> File
): IrArrayReader = IrArrayReader { access.inPlace { it.getFile().readBytes() } }

class IrArrayReader(private val buffer: ReadBuffer) {
    private val indexToOffset: IndexToOffset = buffer.readIndexToOffset(0)

    fun entryCount() = indexToOffset.size - 1

    fun tableItemBytes(id: Int): ByteArray {
        val offset = indexToOffset[id]
        val size = indexToOffset[id + 1] - offset
        val result = ByteArray(size)
        buffer.position = offset
        buffer.get(result, 0, size)
        return result
    }
}

/** Read directly from a byte array. */
fun IrMultiArrayReader(bytes: ByteArray): IrMultiArrayReader = IrMultiArrayReader(ReadBuffer.MemoryBuffer(bytes))

/** On-demand read from a byte array that will be loaded on the first access. */
fun IrMultiArrayReader(loadBytes: () -> ByteArray): IrMultiArrayReader = IrMultiArrayReader(ReadBuffer.OnDemandMemoryBuffer(loadBytes))

/** On-demand read from a file (potentially inside a KLIB archive file). */
fun <L : KotlinLibraryLayout> IrMultiArrayReader(
    access: BaseLibraryAccess<L>,
    getFile: L.() -> File
): IrMultiArrayReader = IrMultiArrayReader { access.inPlace { it.getFile().readBytes() } }

class IrMultiArrayReader(private val buffer: ReadBuffer) {
    private val indexToOffset: IndexToOffset = buffer.readIndexToOffset(0)
    private val indexIndexToOffset = mutableMapOf<Int, IndexToOffset>()

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
            buffer.readIndexToOffset(rowOffset)
        }

        val dataOffset = columnOffsets[column]
        val dataSize = columnOffsets[column + 1] - dataOffset
        val result = ByteArray(dataSize)

        buffer.position = rowOffset + dataOffset
        buffer.get(result, 0, dataSize)

        return result
    }
}


/******************************************************************************/
/** [DeclarationId] readers                                                   */
/******************************************************************************/

data class DeclarationId(val id: Int)

/** Read directly from a byte array. */
fun DeclarationIdTableReader(bytes: ByteArray): DeclarationIdTableReader =
    DeclarationIdTableReader(ReadBuffer.MemoryBuffer(bytes))

/** On-demand read from a byte array that will be loaded on the first access. */
fun DeclarationIdTableReader(loadBytes: () -> ByteArray): DeclarationIdTableReader =
    DeclarationIdTableReader(ReadBuffer.OnDemandMemoryBuffer(loadBytes))

/** On-demand read from a file (potentially inside a KLIB archive file). */
fun <L : KotlinLibraryLayout> DeclarationIdTableReader(
    access: BaseLibraryAccess<L>,
    getFile: L.() -> File
): DeclarationIdTableReader = DeclarationIdTableReader { access.inPlace { it.getFile().readBytes() } }

class DeclarationIdTableReader(private val buffer: ReadBuffer) {
    private val declarationIdToCoordinates: DeclarationIdToCoordinates = buffer.readDeclarationIdToCoordinates(0)

    fun tableItemBytes(declarationId: DeclarationId): ByteArray {
        val (offset, size) = declarationIdToCoordinates[declarationId] ?: error("No coordinates found for $declarationId")
        val result = ByteArray(size)
        buffer.position = offset
        buffer.get(result, 0, size)
        return result
    }
}

/** Read directly from a byte array. */
fun DeclarationIdMultiTableReader(bytes: ByteArray): DeclarationIdMultiTableReader =
    DeclarationIdMultiTableReader(ReadBuffer.MemoryBuffer(bytes))

/** On-demand read from a byte array that will be loaded on the first access. */
fun DeclarationIdMultiTableReader(loadBytes: () -> ByteArray): DeclarationIdMultiTableReader =
    DeclarationIdMultiTableReader(ReadBuffer.OnDemandMemoryBuffer(loadBytes))

/** On-demand read from a file (potentially inside a KLIB archive file). */
fun <L : KotlinLibraryLayout> DeclarationIdMultiTableReader(
    access: BaseLibraryAccess<L>,
    getFile: L.() -> File
): DeclarationIdMultiTableReader = DeclarationIdMultiTableReader { access.inPlace { it.getFile().readBytes() } }

class DeclarationIdMultiTableReader(private val buffer: ReadBuffer) {
    private val indexToOffset: IndexToOffset = buffer.readIndexToOffset(0)
    private val indexToIndexMap = mutableMapOf<Int, DeclarationIdToCoordinates>()

    fun tableItemBytes(idx: Int): ByteArray {
        val rowOffset = indexToOffset[idx]
        val nextOffset = indexToOffset[idx + 1]
        val size = nextOffset - rowOffset
        val result = ByteArray(size)
        buffer.position = rowOffset
        buffer.get(result, 0, size)
        return result
    }

    fun tableItemBytes(row: Int, declarationId: DeclarationId): ByteArray {

        val rowOffset = indexToOffset[row]

        val indexToMap = indexToIndexMap.getOrPut(row) {
            buffer.readDeclarationIdToCoordinates(rowOffset)
        }

        val (offset, size) = indexToMap[declarationId] ?: error("No coordinates found for $declarationId")
        val result = ByteArray(size)
        buffer.position = rowOffset + offset
        buffer.get(result, 0, size)
        return result
    }
}


/******************************************************************************/
/** Other auxiliary public API.                                               */
/******************************************************************************/

val ByteArray.buffer: ByteBuffer get() = ByteBuffer.wrap(this)
fun IrArrayReader.toArray(): Array<ByteArray> = Array(this.entryCount()) { i -> this.tableItemBytes(i) }

fun File.javaFile(): java.io.File = java.io.File(path)


/******************************************************************************/
/** Private utilities.                                                        */
/******************************************************************************/

/** The coordinates of [DeclarationId]. */
private data class DeclarationCoordinates(val offset: Int, val size: Int)

private typealias IndexToOffset = IntArray
private typealias DeclarationIdToCoordinates = MutableMap<DeclarationId, DeclarationCoordinates>

private fun ReadBuffer.readIndexToOffset(position: Int): IndexToOffset {
    this.position = position

    val count = this.int
    val indexToOffset = IndexToOffset(count + 1)

    indexToOffset[0] = 4 * (count + 1)
    for (i in 0 until count) {
        val size = this.int
        indexToOffset[i + 1] = indexToOffset[i] + size
    }

    return indexToOffset
}

private fun ReadBuffer.readDeclarationIdToCoordinates(position: Int): DeclarationIdToCoordinates {
    this.position = position

    val count = this.int
    val declarationIdToCoordinates: DeclarationIdToCoordinates = mutableMapOf()

    for (i in 0 until count) {
        val declarationId = DeclarationId(this.int)
        val offset = this.int
        val size = this.int
        declarationIdToCoordinates[declarationId] = DeclarationCoordinates(offset, size)
    }

    return declarationIdToCoordinates
}
