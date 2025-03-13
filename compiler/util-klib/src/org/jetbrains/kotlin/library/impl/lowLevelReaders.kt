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
    fun tableItemBytes(index: Int): ByteArray = buffer.readTableItemBytes(indexToOffset, index)
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
    private val indexToIndexToOffset: IndexToIndexToOffset = mutableMapOf()

    fun tableItemBytes(index: Int): ByteArray = buffer.readTableItemBytes(indexToOffset, index)
    fun tableItemBytes(rowIndex: Int, columnIndex: Int): ByteArray =
        buffer.readTableItemBytes(indexToOffset, indexToIndexToOffset, rowIndex, columnIndex)
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

    fun tableItemBytes(declarationId: DeclarationId): ByteArray = buffer.readTableItemBytes(declarationIdToCoordinates, declarationId)
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
    private val indexToDeclarationIdToCoordinates: IndexToDeclarationIdToCoordinates = mutableMapOf()

    fun tableItemBytes(index: Int): ByteArray = buffer.readTableItemBytes(indexToOffset, index)
    fun tableItemBytes(rowIndex: Int, declarationId: DeclarationId): ByteArray =
        buffer.readTableItemBytes(indexToOffset, indexToDeclarationIdToCoordinates, rowIndex, declarationId)
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
private typealias IndexToIndexToOffset = MutableMap<Int, IndexToOffset>
private typealias DeclarationIdToCoordinates = MutableMap<DeclarationId, DeclarationCoordinates>
private typealias IndexToDeclarationIdToCoordinates = MutableMap<Int, DeclarationIdToCoordinates>

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

private fun ReadBuffer.readTableItemBytes(indexToOffset: IndexToOffset, index: Int): ByteArray {
    val offset = indexToOffset[index]
    val size = indexToOffset[index + 1] - offset
    return readTableItemBytes(offset, size)
}

private fun ReadBuffer.readTableItemBytes(declarationIdToCoordinates: DeclarationIdToCoordinates, declarationId: DeclarationId): ByteArray {
    val (offset, size) = declarationIdToCoordinates[declarationId] ?: error("No coordinates found for $declarationId")
    return readTableItemBytes(offset, size)
}

private fun ReadBuffer.readTableItemBytes(
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

private fun ReadBuffer.readTableItemBytes(
    indexToOffset: IndexToOffset,
    indexToDeclarationIdToCoordinates: IndexToDeclarationIdToCoordinates,
    rowIndex: Int,
    declarationId: DeclarationId
): ByteArray {
    val rowOffset = indexToOffset[rowIndex]
    val declarationIdToCoordinates: DeclarationIdToCoordinates = indexToDeclarationIdToCoordinates.getOrPut(rowIndex) {
        readDeclarationIdToCoordinates(rowOffset)
    }

    val (offset, size) = declarationIdToCoordinates[declarationId] ?: error("No coordinates found for $declarationId")

    return readTableItemBytes(rowOffset + offset, size)
}

private fun ReadBuffer.readTableItemBytes(offset: Int, size: Int): ByteArray {
    val result = ByteArray(size)
    this.position = offset
    this.get(result, 0, size)

    return result
}
