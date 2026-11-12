/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArchivesTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var destination: Path

    @BeforeEach
    fun createDestination() {
        destination = Files.createDirectories(tempDir.resolve("unpacked"))
    }

    private fun zip(vararg entries: Pair<String, String>) = writeZip(tempDir.resolve("archive.zip"), *entries)

    @Test
    fun `unpacks files and directories`() {
        val archive = zip(
            "tool-1.0/" to "",
            "tool-1.0/bin/tool" to "binary",
            // no entry for the parent directory, as some zip tools write it
            "tool-1.0/lib/deep/nested/file.txt" to "nested",
            "tool-1.0/empty/" to "",
        )

        Archives.zip().extract(archive, destination)

        assertEquals("binary", destination.resolve("tool-1.0/bin/tool").readText())
        assertEquals("nested", destination.resolve("tool-1.0/lib/deep/nested/file.txt").readText())
        assertTrue(Files.isDirectory(destination.resolve("tool-1.0/empty")))
    }

    @Test
    fun `keeps names with spaces and non-ASCII characters`() {
        val longName = "a".repeat(200)
        val archive = zip("Größe und Weite/инструмент.txt" to "text", "$longName/$longName" to "long")

        Archives.zip().extract(archive, destination)

        assertEquals("text", destination.resolve("Größe und Weite/инструмент.txt").readText())
        assertEquals("long", destination.resolve("$longName/$longName").readText())
    }

    @Test
    fun `an empty archive unpacks to nothing`() {
        Archives.zip().extract(zip(), destination)

        assertEquals(0L, Files.list(destination).use { it.count() })
    }

    @Test
    fun `entries that leave the destination are rejected`() {
        for (name in listOf("../evil", "tool/../../evil", "/tmp/evil-${System.nanoTime()}")) {
            val archive = zip("tool/fine" to "fine", name to "evil")

            val exception = assertFailsWith<UnsafeArchiveException>(name) { Archives.zip().extract(archive, destination) }

            assertEquals(name, exception.entryName)
            assertEquals(
                "The archive '$archive' has an entry '$name' that would be written outside the directory it is unpacked into. " +
                        "The archive may have been tampered with.",
                exception.message,
            )
        }
        assertFalse(Files.exists(tempDir.resolve("evil")))
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "creating symlinks needs a privilege on Windows")
    fun `entries are not written through a symlink that leads outside`() {
        val outside = Files.createDirectories(tempDir.resolve("outside"))
        Files.createSymbolicLink(destination.resolve("link"), outside)

        assertFailsWith<UnsafeArchiveException> { Archives.zip().extract(zip("link/evil" to "evil"), destination) }
        assertFailsWith<UnsafeArchiveException> { Archives.zip().extract(zip("link/a/b/" to ""), destination) }
        assertFailsWith<UnsafeArchiveException> { Archives.zip().extract(zip("link/a/file" to "evil"), destination) }

        assertEquals(0L, Files.list(outside).use { it.count() }, "nothing is created outside, not even an empty directory")
    }

    @Test
    fun `a file entry where a directory already is fails`() {
        val archive = zip("tool/file" to "x", "tool" to "not a directory")

        assertFailsWith<IOException> { Archives.zip().extract(archive, destination) }
    }

    @Test
    fun `a file that is not a zip fails`() {
        val notZip = Files.write(tempDir.resolve("archive.zip"), "plain text".toByteArray())

        assertFailsWith<ZipException> { Archives.zip().extract(notZip, destination) }
    }
}
