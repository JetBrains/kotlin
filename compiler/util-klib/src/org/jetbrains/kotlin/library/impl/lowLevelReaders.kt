/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.utils.readUnsignedLeb128
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.readBytes

/******************************************************************************/
/** [ByteArray] readers                                                       */
/******************************************************************************/

/** Read directly from a byte array. */
fun IrArrayReader(bytes: ByteArray): IrArrayReader = IrArrayReaderFromBuffer(ReadByteBufferProvider.MemoryBuffer(bytes))

/** On-demand read from a byte array that will be loaded on the first access. */
fun IrArrayReader(loadBytes: () -> ByteArray): IrArrayReader = IrArrayReaderFromBuffer(ReadByteBufferProvider.OnDemandMemoryBuffer(loadBytes))

fun IrArrayReader(bytes: List<ByteArray>): IrArrayReader = IrArrayReaderFromMemory(bytes)

/** On-demand read from a file (potentially inside a KLIB archive file). */
inline fun <KCL : KlibComponentLayout> IrArrayReader(
    layoutReader: KlibLayoutReader<KCL>,
    crossinline getFile: KCL.() -> Path,
): IrArrayReader = IrArrayReader { layoutReader.readInPlace { it.getFile().readBytes() } }

interface IrArrayReader {
    fun entryCount(): Int
    fun tableItemBytes(index: Int): ByteArray
}

private class IrArrayReaderFromBuffer(private val buffer: ReadByteBufferProvider) : IrArrayReader {
    private val indexToOffset: IndexToOffset = buffer.use { it.readIndexToOffset(0) }

    override fun entryCount() = indexToOffset.size - 1
    override fun tableItemBytes(index: Int): ByteArray = buffer.use { it.readTableItemBytes(indexToOffset, index) }
}

private class IrArrayReaderFromMemory(private val bytes: List<ByteArray>) : IrArrayReader {
    override fun entryCount() = bytes.size
    override fun tableItemBytes(index: Int): ByteArray = bytes[index]
}

/** Read directly from a byte array. */
fun IrMultiArrayReader(bytes: ByteArray): IrMultiArrayReader = IrMultiArrayReaderFromBuffer(ReadByteBufferProvider.MemoryBuffer(bytes))

/** On-demand read from a byte array that will be loaded on the first access. */
fun IrMultiArrayReader(loadBytes: () -> ByteArray): IrMultiArrayReader = IrMultiArrayReaderFromBuffer(ReadByteBufferProvider.OnDemandMemoryBuffer(loadBytes))

fun IrMultiArrayReader(bytes: List<ByteArray>): IrMultiArrayReader = IrMultiArrayReaderFromMemory(bytes)

/** On-demand read from a file (potentially inside a KLIB archive file). */
inline fun <KCL : KlibComponentLayout> IrMultiArrayReader(
    layoutReader: KlibLayoutReader<KCL>,
    crossinline getFile: KCL.() -> Path,
): IrMultiArrayReader = IrMultiArrayReader { layoutReader.readInPlace { it.getFile().readBytes() } }

interface IrMultiArrayReader {
    fun tableItemBytes(index: Int): ByteArray
    fun tableItemBytes(rowIndex: Int, columnIndex: Int): ByteArray
}

private class IrMultiArrayReaderFromBuffer(private val buffer: ReadByteBufferProvider) : IrMultiArrayReader {
    private val indexToOffset: IndexToOffset = buffer.use { it.readIndexToOffset(0) }
    private val indexToIndexToOffset: IndexToIndexToOffset = mutableMapOf()

    override fun tableItemBytes(index: Int): ByteArray = buffer.use { it.readTableItemBytes(indexToOffset, index) }
    override fun tableItemBytes(rowIndex: Int, columnIndex: Int): ByteArray =
        buffer.use { it.readTableItemBytes(indexToOffset, indexToIndexToOffset, rowIndex, columnIndex) }
}

private class IrMultiArrayReaderFromMemory(private val bytes: List<ByteArray>) : IrMultiArrayReader {
    override fun tableItemBytes(index: Int): ByteArray = bytes[index]
    override fun tableItemBytes(rowIndex: Int, columnIndex: Int): ByteArray = IrArrayReader(bytes[rowIndex]).tableItemBytes(columnIndex)
}


/******************************************************************************/
/** [DeclarationId] readers                                                   */
/******************************************************************************/

data class DeclarationId(val id: Int)

/** Read directly from a byte array. */
fun DeclarationIdTableReader(bytes: ByteArray): DeclarationIdTableReader =
    DeclarationIdTableReaderFromBuffer(ReadByteBufferProvider.MemoryBuffer(bytes))

/** On-demand read from a byte array that will be loaded on the first access. */
fun DeclarationIdTableReader(loadBytes: () -> ByteArray): DeclarationIdTableReader =
    DeclarationIdTableReaderFromBuffer(ReadByteBufferProvider.OnDemandMemoryBuffer(loadBytes))

fun DeclarationIdTableReader(bytes: List<ByteArray>): DeclarationIdTableReader =
    DeclarationIdTableReaderFromMemory(bytes)

interface DeclarationIdTableReader {
    fun entryCount(): Int
    fun tableItemBytes(declarationId: DeclarationId): ByteArray
}

class DeclarationIdTableReaderFromBuffer(private val buffer: ReadByteBufferProvider) : DeclarationIdTableReader {
    private val declarationIdToCoordinates: DeclarationIdToCoordinates = buffer.use { it.readDeclarationIdToCoordinates(0) }

    override fun entryCount() = declarationIdToCoordinates.size
    override fun tableItemBytes(declarationId: DeclarationId): ByteArray =
        buffer.use { it.readTableItemBytes(declarationIdToCoordinates, declarationId) }
}

class DeclarationIdTableReaderFromMemory(private val bytes: List<ByteArray>) : DeclarationIdTableReader {
    override fun entryCount() = bytes.size
    override fun tableItemBytes(declarationId: DeclarationId): ByteArray = bytes[declarationId.id]
}

/** Read directly from a byte array. */
fun DeclarationIdMultiTableReader(bytes: ByteArray): DeclarationIdMultiTableReader =
    DeclarationIdMultiTableReaderFromBuffer(ReadByteBufferProvider.MemoryBuffer(bytes))

/** On-demand read from a byte array that will be loaded on the first access. */
fun DeclarationIdMultiTableReader(loadBytes: () -> ByteArray): DeclarationIdMultiTableReader =
    DeclarationIdMultiTableReaderFromBuffer(ReadByteBufferProvider.OnDemandMemoryBuffer(loadBytes))

fun DeclarationIdMultiTableReader(bytes: List<ByteArray>): DeclarationIdMultiTableReader =
    DeclarationIdMultiTableReaderFromMemory(bytes)

/** On-demand read from a file (potentially inside a KLIB archive file). */
inline fun <KCL : KlibComponentLayout> DeclarationIdMultiTableReader(
    layoutReader: KlibLayoutReader<KCL>,
    crossinline getFile: KCL.() -> Path,
): DeclarationIdMultiTableReader = DeclarationIdMultiTableReader { layoutReader.readInPlace { it.getFile().readBytes() } }

interface DeclarationIdMultiTableReader {
    fun tableItemBytes(index: Int): ByteArray
    fun tableItemBytes(rowIndex: Int, declarationId: DeclarationId): ByteArray
}

private class DeclarationIdMultiTableReaderFromBuffer(private val buffer: ReadByteBufferProvider) : DeclarationIdMultiTableReader {
    private val indexToOffset: IndexToOffset = buffer.use { it.readIndexToOffset(0) }
    private val indexToDeclarationIdToCoordinates: IndexToDeclarationIdToCoordinates = mutableMapOf()

    override fun tableItemBytes(index: Int): ByteArray = buffer.use { it.readTableItemBytes(indexToOffset, index) }
    override fun tableItemBytes(rowIndex: Int, declarationId: DeclarationId): ByteArray =
        buffer.use { it.readTableItemBytes(indexToOffset, indexToDeclarationIdToCoordinates, rowIndex, declarationId) }
}

private class DeclarationIdMultiTableReaderFromMemory(private val bytes: List<ByteArray>) : DeclarationIdMultiTableReader {
    override fun tableItemBytes(index: Int): ByteArray = error("Not supported")
    override fun tableItemBytes(rowIndex: Int, declarationId: DeclarationId): ByteArray =
        DeclarationIdTableReader(bytes[rowIndex]).tableItemBytes(declarationId)
}


/******************************************************************************/
/** Other auxiliary public API.                                               */
/******************************************************************************/

val ByteArray.buffer: ByteBuffer get() = ByteBuffer.wrap(this)
fun IrArrayReader.toArray(): Array<ByteArray> = Array(this.entryCount()) { i -> this.tableItemBytes(i) }

/******************************************************************************/
/** Private utilities.                                                        */
/******************************************************************************/

/** The coordinates of [DeclarationId]. */
private data class DeclarationCoordinates(val offset: Int, val size: Int)

private typealias IndexToOffset = IntArray
private typealias IndexToIndexToOffset = MutableMap<Int, IndexToOffset>
private typealias DeclarationIdToCoordinates = MutableMap<DeclarationId, DeclarationCoordinates>
private typealias IndexToDeclarationIdToCoordinates = MutableMap<Int, DeclarationIdToCoordinates>

private fun ByteBuffer.readIndexToOffset(position: Int): IndexToOffset {
    this.position(position)

    var count = this.int
    var usesVarInt = false
    if (count < 0) {
        // Negative count of elements means that element sizes use var-int encoding (available since 2.4.0).
        count = -count
        usesVarInt = true
    }

    val elementSizes = IntArray(count) {
        if (usesVarInt) readUnsignedLeb128(this::get).toInt() else this.int
    }

    val indexToOffset = IndexToOffset(count + 1)
    // After reading all element sizes, we know at which position the element values start.
    indexToOffset[0] = this.position() - position
    for (i in 0 until count) {
        indexToOffset[i + 1] = indexToOffset[i] + elementSizes[i]
    }

    return indexToOffset
}

private fun ByteBuffer.readDeclarationIdToCoordinates(position: Int): DeclarationIdToCoordinates {
    this.position(position)

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

private fun ByteBuffer.readTableItemBytes(indexToOffset: IndexToOffset, index: Int): ByteArray {
    val offset = indexToOffset[index]
    val size = indexToOffset[index + 1] - offset
    return readTableItemBytes(offset, size)
}

private fun ByteBuffer.readTableItemBytes(declarationIdToCoordinates: DeclarationIdToCoordinates, declarationId: DeclarationId): ByteArray {
    val (offset, size) = declarationIdToCoordinates[declarationId] ?: error("No coordinates found for $declarationId")
    return readTableItemBytes(offset, size)
}

private fun ByteBuffer.readTableItemBytes(
    indexToOffset: IndexToOffset,
    indexToIndexToOffset: IndexToIndexToOffset,
    rowIndex: Int,
    columnIndex: Int,
): ByteArray {
    val rowOffset = indexToOffset[rowIndex]
    val columnIndexToOffset: IndexToOffset = indexToIndexToOffset.getOrPut(rowIndex) { readIndexToOffset(rowOffset) }

    val offset = columnIndexToOffset[columnIndex]
    val size = columnIndexToOffset[columnIndex + 1] - offset

    return readTableItemBytes(rowOffset + offset, size)
}

private fun ByteBuffer.readTableItemBytes(
    indexToOffset: IndexToOffset,
    indexToDeclarationIdToCoordinates: IndexToDeclarationIdToCoordinates,
    rowIndex: Int,
    declarationId: DeclarationId,
): ByteArray {
    val rowOffset = indexToOffset[rowIndex]
    val declarationIdToCoordinates: DeclarationIdToCoordinates = indexToDeclarationIdToCoordinates.getOrPut(rowIndex) {
        readDeclarationIdToCoordinates(rowOffset)
    }

    val (offset, size) = declarationIdToCoordinates[declarationId] ?: error("No coordinates found for $declarationId")

    return readTableItemBytes(rowOffset + offset, size)
}

private fun ByteBuffer.readTableItemBytes(offset: Int, size: Int): ByteArray {
    val result = ByteArray(size)
    this.position(offset)
    this.get(result, 0, size)

    return result
}
