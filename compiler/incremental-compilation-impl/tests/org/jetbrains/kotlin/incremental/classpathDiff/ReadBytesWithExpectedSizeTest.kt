/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.util.readBytesWithExpectedSize
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ReadBytesWithExpectedSizeTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun testCorrectAndIncorrectSizes() {
        for (size in listOf(0, 1, 8191, 8192, 8193, 1024 * 1024 + 1)) {
            val bytes = ByteArray(size) { it.toByte() }
            for (expectedSize in listOf(-1L, 0L, 1L, size.toLong(), size + 7L, 1024 * 1024L, Long.MAX_VALUE)) {
                assertArrayEquals(bytes, bytes.inputStream().readBytesWithExpectedSize(expectedSize), "size=$size, expected=$expectedSize")
            }
        }
    }

    @Test
    fun testPartialReadsAndZeroProgress() {
        val bytes = ByteArray(100) { it.toByte() }
        for (expectedSize in listOf(50L, 100L, 150L)) {
            val stream = object : ByteArrayInputStream(bytes) {
                private var zeroProgress = true

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    if (zeroProgress) {
                        zeroProgress = false
                        return 0
                    }
                    return super.read(buffer, offset, minOf(length, 3))
                }
            }
            assertArrayEquals(bytes, stream.readBytesWithExpectedSize(expectedSize))
        }
    }

    @Test
    fun testUntrustedAvailableSize() {
        val bytes = byteArrayOf(1, 2, 3)
        for (expectedSize in listOf(-1L, Long.MAX_VALUE)) {
            val stream = object : ByteArrayInputStream(bytes) {
                override fun available(): Int = Int.MAX_VALUE
            }
            assertArrayEquals(bytes, stream.readBytesWithExpectedSize(expectedSize))
        }
    }

    @Test
    fun testReadFailureIsPropagated() {
        val stream = object : InputStream() {
            override fun read(): Int = throw IOException("read failed")
        }
        assertThrows(IOException::class.java) { stream.readBytesWithExpectedSize(10) }
    }

    @Test
    fun testStoredZipEntry() {
        val contents = ByteArray(10000) { it.toByte() }
        val jar = temporaryDirectory.resolve("stored.jar").toFile()
        ZipOutputStream(jar.outputStream()).use {
            it.putNextEntry(ZipEntry("Input.class").apply {
                method = ZipEntry.STORED
                size = contents.size.toLong()
                crc = CRC32().apply { update(contents) }.value
            })
            it.write(contents)
            it.closeEntry()
        }
        ZipFile(jar).use { zip ->
            val entry = zip.getEntry("Input.class")
            assertArrayEquals(contents, zip.getInputStream(entry).use { it.readBytesWithExpectedSize(entry.size) })
        }
    }

    @Test
    fun testDeflatedZipEntryWithIncorrectSize() {
        val contents = ByteArray(10000) { it.toByte() }
        val jar = temporaryDirectory.resolve("incorrect-size.jar").toFile()
        ZipOutputStream(jar.outputStream()).use {
            it.putNextEntry(ZipEntry("Input.class"))
            it.write(contents)
            it.closeEntry()
        }
        val original = jar.readBytes()
        val buffer = ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN)
        val centralDirectory = (0..original.size - 4).single { buffer.getInt(it) == 0x02014b50 }
        for (size in listOf(0, 1, contents.size - 1, contents.size, contents.size + 1, Int.MAX_VALUE)) {
            buffer.putInt(centralDirectory + 24, size)
            jar.writeBytes(original)
            ZipFile(jar).use { zip ->
                val entry = zip.getEntry("Input.class")
                val actual = zip.getInputStream(entry).use { it.readBytesWithExpectedSize(entry.size) }
                assertArrayEquals(contents, actual, "ZIP uncompressed size=$size")
            }
        }
    }
}
