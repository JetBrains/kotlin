/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    order(ByteOrder.LITTLE_ENDIAN)
    val extraSize =
        getUnsignedShort(zipEntryDescription.offsetInFile + LOCAL_FILE_HEADER_EXTRA_OFFSET)

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
    order(ByteOrder.LITTLE_ENDIAN)

    var endOfCentralDirectoryOffset = capacity() - END_OF_CENTRAL_DIR_SIZE
    while (endOfCentralDirectoryOffset >= 0) {
        // header of "End of central directory"
        if (getInt(endOfCentralDirectoryOffset) == 0x06054b50) break
        endOfCentralDirectoryOffset--
    }

    val entriesNumber = getUnsignedShort(endOfCentralDirectoryOffset + 10)
    val offsetOfCentralDirectory = getInt(endOfCentralDirectoryOffset + 16)

    var currentOffset = offsetOfCentralDirectory

    val result = mutableListOf<ZipEntryDescription>()
    for (i in 0 until entriesNumber) {
        val headerConst = getInt(currentOffset)
        require(headerConst == 0x02014b50) {
            "$i: $headerConst"
        }

        val versionNeededToExtract =
            getShort(currentOffset + 6).toInt()

        val compressionMethod = getShort(currentOffset + 10).toInt()

        val compressedSize = getInt(currentOffset + 20)
        val uncompressedSize = getInt(currentOffset + 24)
        val fileNameLength = getUnsignedShort(currentOffset + 28).toInt()
        val extraLength = getUnsignedShort(currentOffset + 30).toInt()
        val fileCommentLength = getUnsignedShort(currentOffset + 32).toInt()

        val offsetOfFileData = getInt(currentOffset + 42)

        val bytesForName = ByteArray(fileNameLength)

        position(currentOffset + 46)
        get(bytesForName)

        val name =
            if (bytesForName.all { it >= 0 })
                String(bytesForName.asASCICharArray())
            else
                String(bytesForName, Charsets.UTF_8)

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

private fun ByteArray.asASCICharArray(): CharArray = CharArray(size) { this@asASCICharArray[it].toChar() }

private fun ByteBuffer.getUnsignedShort(offset: Int): Int = java.lang.Short.toUnsignedInt(getShort(offset))
