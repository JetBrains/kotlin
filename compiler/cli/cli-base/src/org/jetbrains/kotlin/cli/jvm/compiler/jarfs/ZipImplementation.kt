/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import java.util.zip.Inflater


class ZipEntryDescription(
    val relativePath: CharSequence,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val offsetInFile: Long,
    val compressionKind: CompressionKind,
    val fileNameSize: Int,
) {

    enum class CompressionKind {
        PLAIN, DEFLATE
    }

    val isDirectory get() = uncompressedSize == 0L
}

private const val END_OF_CENTRAL_DIR_SIZE = 22
private const val END_OF_CENTRAL_DIR_ZIP64_SIZE = 56
private const val LOCAL_FILE_HEADER_EXTRA_OFFSET = 28
private const val LOCAL_FILE_HEADER_SIZE = LOCAL_FILE_HEADER_EXTRA_OFFSET + 2

internal fun LargeDynamicMappedBuffer.contentsToByteArray(
    zipEntryDescription: ZipEntryDescription
): ByteArray =
    withMappedRangeFrom(zipEntryDescription.offsetInFile) {
        val extraSize = getUnsignedShort((LOCAL_FILE_HEADER_EXTRA_OFFSET).toInt())
        val startPos = LOCAL_FILE_HEADER_SIZE + zipEntryDescription.fileNameSize + extraSize

        require(zipEntryDescription.compressedSize - startPos < Int.MAX_VALUE && zipEntryDescription.uncompressedSize <= Int.MAX_VALUE) {
            "Reading files bigger than Int.MAX_VALUE - $startPos is not supported yet"
        }

        when (zipEntryDescription.compressionKind) {
            ZipEntryDescription.CompressionKind.DEFLATE -> {
                val inflater = Inflater(true)
                // Note that starting from JDK 16 it is possible to call MappedByteBuffer.slice to get a "sub-buffer" and also
                // use it directly with inflater.setInput, which would avoid copying of the data
                // TODO: consider implementing (maybe JDK-specific) optimization that avoids unnecessary copying (see KT-69758)
                inflater.setInput(getBytes(startPos, zipEntryDescription.compressedSize.toInt()))

                val result = ByteArray(zipEntryDescription.uncompressedSize.toInt())

                inflater.inflate(result)
                inflater.end()

                result
            }

            ZipEntryDescription.CompressionKind.PLAIN -> getBytes(startPos, zipEntryDescription.compressedSize.toInt())
        }
    }

internal fun LargeDynamicMappedBuffer.parseCentralDirectory(): List<ZipEntryDescription> {

    val (entriesNumber, offsetOfCentralDirectory) = parseCentralDirectoryRecordsNumberAndOffset()

    var currentStart = offsetOfCentralDirectory

    val result = mutableListOf<ZipEntryDescription>()
    for (i in 0 until entriesNumber) {
        withMappedRangeFrom(currentStart) {
            val headerConst = getInt(0)
            require(headerConst == 0x02014b50) {
                "$i: $headerConst"
            }

            val versionNeededToExtract =
                getShort(6).toInt()

            val compressionMethod = getShort(10).toInt()

            val fileNameLength = getUnsignedShort(28)
            val extraLength = getUnsignedShort(30)
            val extraFieldOffset = 46 + fileNameLength

            val compressedSize32 = getInt(20)
            val uncompressedSize32 = getInt(24)
            val fileCommentLength = getUnsignedShort(32)

            val offsetOfFileData32 = getInt(42)

            // NOTE: order of Zip64 fields is fixed, see "4.5.3 -Zip64 Extended Information Extra Field" in zip file format specs
            var extraFieldNo = 0
            val extraFieldsSize = getShort(extraFieldOffset + 2)

            fun Int.toLongOrNextZip64ExtrField(): Long =
                if (this != -1) toUInt().toLong()
                else {
                    require(extraFieldsSize >= (extraFieldNo + 1) * 8)
                    getLong(extraFieldOffset + 4 + extraFieldNo * 8).also { extraFieldNo++ }
                }

            val compressedSize = compressedSize32.toLongOrNextZip64ExtrField()
            val uncompressedSize = uncompressedSize32.toLongOrNextZip64ExtrField()
            val offsetOfFileData = offsetOfFileData32.toLongOrNextZip64ExtrField()

            val bytesForName = getBytes(46, fileNameLength)

            val name =
                if (bytesForName.all { it >= 0 })
                    ByteArrayCharSequence(bytesForName)
                else
                    String(bytesForName, Charsets.UTF_8)
            currentStart += 46 + fileNameLength + extraLength + fileCommentLength

            // We support version needed to extract 1.0, 2.0 and 4.5. However, there are zip
            // files in the eco-system with entries with invalid version to extract
            // of 0. Therefore, we just check that the version is between 0 and 20 and additionally for version 4.5.
            require(versionNeededToExtract in 0..20 || versionNeededToExtract == 45) {
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
    }

    return result
}

private fun LargeDynamicMappedBuffer.parseCentralDirectoryRecordsNumberAndOffset(): Pair<Long, Long> =
    withMappedTail {
        var endOfCentralDirectoryOffset = endOffset() - END_OF_CENTRAL_DIR_SIZE
        while (endOfCentralDirectoryOffset >= 0) {
            // header of "End of central directory" (see https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT)
            if (getInt(endOfCentralDirectoryOffset) == 0x06054b50) break
            endOfCentralDirectoryOffset--
        }

        val entriesNumber = getUnsignedShort(endOfCentralDirectoryOffset + 10)
        val offsetOfCentralDirectory = getInt(endOfCentralDirectoryOffset + 16)
        // Offset of start of central directory, relative to start of archive (or -1 for ZIP64)
        // (see https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT)
        if (entriesNumber == 0xffff || offsetOfCentralDirectory == -1) parseZip64CentralDirectoryRecordsNumberAndOffset()
        else Pair(entriesNumber.toLong(), offsetOfCentralDirectory.toUInt().toLong())
    }


private fun LargeDynamicMappedBuffer.Mapping.parseZip64CentralDirectoryRecordsNumberAndOffset(): Pair<Long, Long> {
    var endOfCentralDirectoryOffset = endOffset() - END_OF_CENTRAL_DIR_ZIP64_SIZE
    while (endOfCentralDirectoryOffset >= 0) {
        // header of "End of central directory" (see https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT)
        if (getInt(endOfCentralDirectoryOffset) == 0x06064b50) break
        endOfCentralDirectoryOffset--
    }

    val entriesNumber = getLong(endOfCentralDirectoryOffset + 32)
    val offsetOfCentralDirectory = getLong(endOfCentralDirectoryOffset + 48)

    return Pair(entriesNumber, offsetOfCentralDirectory)
}

private fun LargeDynamicMappedBuffer.Mapping.getUnsignedShort(offset: Int): Int = java.lang.Short.toUnsignedInt(getShort(offset))

