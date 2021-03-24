/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import java.nio.MappedByteBuffer
import java.util.zip.Inflater


class ZipEntryDescription(
    val relativePath: String,
    val compressedSize: Int,
    val uncompressedSize: Int,
    val offsetInFile: Int,
    val compressionKind: CompressionKind,
    val fileNameSize: Int
) {

    enum class CompressionKind {
        PLAIN, DEFLATE
    }

    val isDirectory get() = uncompressedSize == 0
}

private const val END_OF_CENTRAL_DIR_SIZE = 22
private const val LOCAL_FILE_HEADER_EXTRA_OFFSET = 28
private const val LOCAL_FILE_HEADER_SIZE = LOCAL_FILE_HEADER_EXTRA_OFFSET + 2

fun MappedByteBuffer.contentsToByteArray(
    zipEntryDescription: ZipEntryDescription
): ByteArray {
    val extraSize =
        readShortLittleEndianFromOffset((zipEntryDescription.offsetInFile + LOCAL_FILE_HEADER_EXTRA_OFFSET))

    position(
        zipEntryDescription.offsetInFile + LOCAL_FILE_HEADER_SIZE + zipEntryDescription.fileNameSize + extraSize
    )
    val compressed = ByteArray(zipEntryDescription.compressedSize + 1)
    get(compressed, 0, zipEntryDescription.compressedSize)

    return when (zipEntryDescription.compressionKind) {
        ZipEntryDescription.CompressionKind.DEFLATE -> {
            val inflater = Inflater(true)
            inflater.setInput(compressed, 0, zipEntryDescription.compressedSize)

            val result = ByteArray(zipEntryDescription.uncompressedSize)

            inflater.inflate(result)

            result
        }
        ZipEntryDescription.CompressionKind.PLAIN -> compressed.copyOf(zipEntryDescription.compressedSize)
    }
}

fun MappedByteBuffer.parseCentralDirectory(): List<ZipEntryDescription> {

    val endOfCentralDirectoryOffset = (capacity() - END_OF_CENTRAL_DIR_SIZE downTo 0).first { offset ->
        // header of "End of central directory"
        readIntLittleEndianFromOffset(offset) == 0x06054b50
    }

    val entriesNumber = readShortLittleEndianFromOffset(endOfCentralDirectoryOffset + 10)
    val offsetOfCentralDirectory = readIntLittleEndianFromOffset(endOfCentralDirectoryOffset + 16)

    var currentOffset = offsetOfCentralDirectory

    val result = mutableListOf<ZipEntryDescription>()
    for (i in 0 until entriesNumber) {
        val headerConst = readIntLittleEndianFromOffset(currentOffset)
        require(headerConst == 0x02014b50) {
            "$i: $headerConst"
        }

        val versionNeededToExtract =
            readShortLittleEndianFromOffset(currentOffset + 6)

        val compressionMethod = readShortLittleEndianFromOffset(currentOffset + 10)

        val compressedSize = readIntLittleEndianFromOffset(currentOffset + 20)
        val uncompressedSize = readIntLittleEndian()
        val fileNameLength = readShortLittleEndian()
        val extraLength = readShortLittleEndian()
        val fileCommentLength = readShortLittleEndian()

        val offsetOfFileData = readIntLittleEndianFromOffset(currentOffset + 42)

        val bytesForName = ByteArray(fileNameLength)

        get(bytesForName)

        val name = String(bytesForName)

        currentOffset += 46 + fileNameLength + extraLength + fileCommentLength

        require(versionNeededToExtract == 10 || versionNeededToExtract == 20) {
            "Unexpected versionNeededToExtract ($versionNeededToExtract) at $name"
        }

        val compressionKind = when (compressionMethod) {
            0 -> ZipEntryDescription.CompressionKind.PLAIN
            8 -> ZipEntryDescription.CompressionKind.DEFLATE
            else -> error("Unexpected compression method ($compressionMethod) at $name")
        }

        result += ZipEntryDescription(
            name, compressedSize, uncompressedSize, offsetOfFileData, compressionKind,
            fileNameLength
        )
    }

    return result
}

private fun MappedByteBuffer.readIntLittleEndianFromOffset(offset: Int): Int {
    position(offset)
    return readIntLittleEndian()
}

private fun MappedByteBuffer.getByteAsInt(): Int = java.lang.Byte.toUnsignedInt(get())

private fun MappedByteBuffer.readIntLittleEndian(): Int {
    val a = this.getByteAsInt()
    val b = this.getByteAsInt()
    val c = this.getByteAsInt()
    val d = this.getByteAsInt()
    return (d shl 24) + (c shl 16) + (b shl 8) + a
}

private fun MappedByteBuffer.readShortLittleEndianFromOffset(offset: Int): Int {
    position(offset)
    return readShortLittleEndian()
}

private fun MappedByteBuffer.readShortLittleEndian(): Int {
    val a = this.getByteAsInt()
    val b = this.getByteAsInt()
    return (b shl 8) + a
}
