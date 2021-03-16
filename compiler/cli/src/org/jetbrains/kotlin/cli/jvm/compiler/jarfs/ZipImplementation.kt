/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import java.io.File
import java.io.RandomAccessFile
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

fun File.contentsToByteArray(zipEntryDescription: ZipEntryDescription): ByteArray {
    return RandomAccessFile(this, "r").use { randomAccessFile ->
        val bufferedRandomAccessFile = BufferedRandomAccessFile(randomAccessFile)
        bufferedRandomAccessFile.contentsToByteArray(zipEntryDescription)
    }
}

fun BufferedRandomAccessFile.contentsToByteArray(
    zipEntryDescription: ZipEntryDescription
): ByteArray {
    val extraSize =
        readShortLittleEndianFromOffset((zipEntryDescription.offsetInFile + LOCAL_FILE_HEADER_EXTRA_OFFSET).toLong())

    seek(
        (zipEntryDescription.offsetInFile + LOCAL_FILE_HEADER_SIZE + zipEntryDescription.fileNameSize + extraSize).toLong()
    )
    val compressed = ByteArray(zipEntryDescription.compressedSize + 1)
    readFully(compressed, zipEntryDescription.compressedSize)

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

fun File.parseCentralDirectory(): List<ZipEntryDescription> {
    return RandomAccessFile(this, "r").use { randomAccessFile ->
        BufferedRandomAccessFile(randomAccessFile).parseCentralDirectory()
    }
}

fun BufferedRandomAccessFile.parseCentralDirectory(): List<ZipEntryDescription> {
    val randomAccessFile = randomAccessFile

    val endOfCentralDirectoryOffset = (randomAccessFile.length() - END_OF_CENTRAL_DIR_SIZE downTo 0).first { offset ->
        // header of "End of central directory"
        randomAccessFile.readIntLittleEndianFromOffset(offset) == 0x06054b50
    }

    val entriesNumber = randomAccessFile.readShortLittleEndianFromOffset(endOfCentralDirectoryOffset + 10)
    val offsetOfCentralDirectory = randomAccessFile.readIntLittleEndianFromOffset(endOfCentralDirectoryOffset + 16).toLong()

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

        val name = readString(fileNameLength)

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

class BufferedRandomAccessFile(
    val randomAccessFile: RandomAccessFile
) {
    private val buffer = ByteArray(BUFFER_SIZE)
    private var offsetInBuffer: Int = -1
    private var currentBegin: Long = -1L

    fun seek(globalOffset: Long, force: Boolean = false) {
        if (!force && currentBegin != -1L && globalOffset >= currentBegin && globalOffset < currentBegin + BUFFER_SIZE) {
            offsetInBuffer = (globalOffset - currentBegin).toInt()
            return
        }
        offsetInBuffer = 0
        currentBegin = globalOffset
        randomAccessFile.seek(globalOffset)
        randomAccessFile.readFully(buffer, 0, minOf(randomAccessFile.length() - globalOffset, BUFFER_SIZE.toLong()).toInt())
    }

    fun read(): Int {
        require(offsetInBuffer < BUFFER_SIZE)
        return buffer[offsetInBuffer++].let {
            if (offsetInBuffer == BUFFER_SIZE && currentBegin + BUFFER_SIZE < randomAccessFile.length()) {
                seek(currentBegin + BUFFER_SIZE)
            }

            java.lang.Byte.toUnsignedInt(it)
        }
    }

    fun readString(length: Int): String {
        if (length > BUFFER_SIZE) {
            randomAccessFile.seek(currentBegin + offsetInBuffer)
            val byteArray = ByteArray(length)
            randomAccessFile.readFully(byteArray)
            return String(byteArray)
        }

        if (length > BUFFER_SIZE - offsetInBuffer) {
            seek(currentBegin + offsetInBuffer, force = true)
        }

        return String(buffer, offsetInBuffer, length)
    }

    fun readFully(dst: ByteArray, length: Int) {
        if (length > BUFFER_SIZE) {
            randomAccessFile.seek(currentBegin + offsetInBuffer)
            randomAccessFile.readFully(dst, 0, length)
            return
        }

        if (length > BUFFER_SIZE - offsetInBuffer) {
            seek(currentBegin + offsetInBuffer, force = true)
        }

        System.arraycopy(buffer, offsetInBuffer, dst, 0, length)
    }

    fun close() {
        randomAccessFile.close()
    }

    companion object {
        private const val BUFFER_SIZE = 50000
    }
}

private fun RandomAccessFile.readIntLittleEndianFromOffset(offset: Long): Int {
    seek(offset)
    return readIntLittleEndian()
}

private fun RandomAccessFile.readIntLittleEndian(): Int {
    val a = this.read()
    val b = this.read()
    val c = this.read()
    val d = this.read()
    return (d shl 24) + (c shl 16) + (b shl 8) + a
}

private fun RandomAccessFile.readShortLittleEndianFromOffset(offset: Long): Int {
    seek(offset)
    return readShortLittleEndian()
}

private fun RandomAccessFile.readShortLittleEndian(): Int {
    val a = this.read()
    val b = this.read()
    return (b shl 8) + a
}

private fun BufferedRandomAccessFile.readIntLittleEndianFromOffset(offset: Long): Int {
    seek(offset)
    return readIntLittleEndian()
}

private fun BufferedRandomAccessFile.readIntLittleEndian(): Int {
    val a = this.read()
    val b = this.read()
    val c = this.read()
    val d = this.read()
    return (d shl 24) + (c shl 16) + (b shl 8) + a
}

private fun BufferedRandomAccessFile.readShortLittleEndianFromOffset(offset: Long): Int {
    seek(offset)
    return readShortLittleEndian()
}

private fun BufferedRandomAccessFile.readShortLittleEndian(): Int {
    val a = this.read()
    val b = this.read()
    return (b shl 8) + a
}
