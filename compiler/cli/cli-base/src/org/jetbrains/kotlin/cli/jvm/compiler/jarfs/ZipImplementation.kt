/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import java.nio.ByteBuffer
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
        val extraSize = getUnsignedShort(LOCAL_FILE_HEADER_EXTRA_OFFSET)
        val startPos = LOCAL_FILE_HEADER_SIZE + zipEntryDescription.fileNameSize + extraSize

        require(zipEntryDescription.compressedSize - startPos < Int.MAX_VALUE && zipEntryDescription.uncompressedSize <= Int.MAX_VALUE) {
            "Reading files bigger than Int.MAX_VALUE - $startPos is not supported yet"
        }

        when (zipEntryDescription.compressionKind) {
            ZipEntryDescription.CompressionKind.DEFLATE -> {
                val inflater = Inflater(true)
                val compressedSize = zipEntryDescription.compressedSize.toInt()
                val setInput = setInflaterInputFromBuffer
                if (setInput != null) {
                    // JDK 11+: feed a slice of the mapped buffer directly, avoiding the allocation and the get() copy
                    setInput(inflater, slicedBuffer(startPos, compressedSize))
                } else {
                    // JDK 8-10 fallback: copy the compressed bytes into a heap array first
                    inflater.setInput(getBytes(startPos, compressedSize))
                }

                val result = ByteArray(zipEntryDescription.uncompressedSize.toInt())

                inflater.inflate(result)
                inflater.end()

                result
            }

            ZipEntryDescription.CompressionKind.PLAIN -> getBytes(startPos, zipEntryDescription.compressedSize.toInt())
        }
    }

internal fun LargeDynamicMappedBuffer.parseCentralDirectory(): List<ZipEntryDescription> {

    val [entriesNumber, offsetOfCentralDirectory] = parseCentralDirectoryRecordsNumberAndOffset()

    var currentStart = offsetOfCentralDirectory

    val result = mutableListOf<ZipEntryDescription>()
    for (i in 0 until entriesNumber) {
        withMappedRangeFrom(currentStart) {
            val headerConst = getInt(0)
            require(headerConst == 0x02014b50) {
                "$i: $headerConst"
            }

            //  see the comment to the commented usage below
//            val versionNeededToExtract =
//                getShort(6).toInt()

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
            // files with incorrect version in the wild, e.g. see KT-74490
            // since there is no such check in the JDK zip implementation, the check here is also disabled
//            require(versionNeededToExtract in 0..20 || versionNeededToExtract == 45) {
//                "Unexpected versionNeededToExtract ($versionNeededToExtract) at $name"
//            }

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

/**
 * `Inflater.setInput(ByteBuffer)` exists since JDK 11. When it is available, we can feed the inflater a slice of the
 * memory-mapped buffer directly, avoiding both the allocation and the `ByteBuffer.get(...)` copy of the compressed data
 * (the latter pins a heap array via `Unsafe.copyMemory`/GCLocker since JDK 17, see KT-69758).
 *
 * On older JDKs (8-10) the method is absent, this stays `null`, and we fall back to the copying path.
 * TODO: Get rid of this property once this module is switched to JDK 11+ (KT-86803)
 */
private val setInflaterInputFromBuffer: ((Inflater, ByteBuffer) -> Unit)? =
    try {
        val method = Inflater::class.java.getMethod("setInput", ByteBuffer::class.java)
        val setter: (Inflater, ByteBuffer) -> Unit = { inflater, buffer -> method.invoke(inflater, buffer) }
        setter
    } catch (e: Throwable) {
        null
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

