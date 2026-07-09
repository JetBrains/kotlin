/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.merger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class KlibMergerTest {
    @Test
    fun packExtractRoundTrip(@TempDir tmp: Path) {
        val klibA = sampleKlib(
            tmp.resolve("libA.klib"),
            mapOf(
                "default/manifest" to "unique_name=libA\n",
                "default/ir/types.knt" to "A-".repeat(5000),
                "default/linkdata/module" to "moduleA",
            ),
        )
        val klibB = sampleKlib(
            tmp.resolve("libB.klib"),
            mapOf(
                "default/manifest" to "unique_name=libB\n",
                "default/ir/types.knt" to "B-".repeat(5000),
            ),
        )

        val archive = tmp.resolve("bundle.klibz")
        KlibMerger.packageKlibDirectories(listOf(klibA, klibB), archive)

        // Self-describing header: magic "KZLB" (0x4B5A4C42) + format version 1 at offset 0.
        java.io.DataInputStream(archive.inputStream()).use { header ->
            assertEquals(0x4B5A4C42, header.readInt(), "leading magic")
            assertEquals(1, header.readUnsignedByte(), "format version")
        }

        val entries = KlibMerger.listEntries(archive)
        assertEquals(listOf("libA", "libB"), entries.map { it.name }.sorted())

        // The bundle should be smaller than the raw klib contents thanks to zstd on the repetitive payload.
        val rawSize = entries.sumOf { it.uncompressedSize }
        assertTrue(archive.fileSize() < rawSize, "archive (${archive.fileSize()}) should be smaller than raw ($rawSize)")

        val extracted = tmp.resolve("extracted")
        KlibMerger.extractKlib(archive, "libA", extracted)
        assertKlibTreesEqual(klibA, extracted)
    }

    @Test
    fun packExtractWithReferenceRoundTrip(@TempDir tmp: Path) {
        // High-entropy blob shared verbatim by all klibs (so per-frame zstd cannot shrink it on its own;
        // only cross-klib dedup against the reference can), plus a small per-klib unique marker.
        val shared = ByteArray(64 * 1024).also { bytes ->
            var state = 0x12345678
            for (i in bytes.indices) {
                state = state * 1103515245 + 12345
                bytes[i] = (state ushr 16).toByte()
            }
        }
        val klibs = listOf("libA", "libB", "libC").map { makeSharedKlib(tmp.resolve("$it.klib"), shared, "unique-$it") }

        val refArchive = tmp.resolve("bundle-ref.klibz")
        KlibMerger.packageKlibDirectories(klibs, refArchive, referenceKlibName = "libA")

        // Index metadata: libA is the standalone reference; the others are compressed against it.
        assertEquals("libA", KlibMerger.referenceKlibName(refArchive))
        val entries = KlibMerger.listEntries(refArchive).associateBy { it.name }
        assertFalse(entries.getValue("libA").usesReference)
        assertTrue(entries.getValue("libB").usesReference)
        assertTrue(entries.getValue("libC").usesReference)

        // Round-trip both the reference klib and a reference-compressed klib — no codec passed:
        // extraction reconstructs the LDM codec from the descriptor stored in the archive.
        val outA = tmp.resolve("outA")
        KlibMerger.extractKlib(refArchive, "libA", outA)
        assertKlibTreesEqual(klibs[0], outA)
        val outB = tmp.resolve("outB")
        KlibMerger.extractKlib(refArchive, "libB", outB)
        assertKlibTreesEqual(klibs[1], outB)

        // On near-identical klibs the reference scheme must beat fully independent frames
        // (same LDM window; the only difference is the shared reference dictionary).
        val independentArchive = tmp.resolve("bundle-independent.klibz")
        KlibMerger.packageKlibDirectories(klibs, independentArchive)
        assertTrue(
            refArchive.fileSize() < independentArchive.fileSize(),
            "reference bundle (${refArchive.fileSize()}) should be smaller than independent (${independentArchive.fileSize()})",
        )
    }

    private fun makeSharedKlib(root: Path, shared: ByteArray, uniqueMarker: String): Path {
        root.resolve("default/ir").createDirectories()
        root.resolve("default/manifest").writeText("unique_name=${root.fileName}\n")
        root.resolve("default/ir/shared.knt").writeBytes(shared)
        root.resolve("default/ir/unique.knt").writeText(uniqueMarker)
        return root
    }

    private fun sampleKlib(root: Path, files: Map<String, String>): Path {
        for ((relativePath, content) in files) {
            val file = root.resolve(relativePath)
            file.parent.createDirectories()
            file.writeText(content)
        }
        return root
    }

    private fun assertKlibTreesEqual(expected: Path, actual: Path) {
        val expectedFiles = relativeFiles(expected)
        val actualFiles = relativeFiles(actual)
        assertEquals(expectedFiles.keys.sorted(), actualFiles.keys.sorted(), "file sets differ")
        for ((relativePath, bytes) in expectedFiles) {
            assertEquals(bytes.toList(), actualFiles.getValue(relativePath).toList(), "content mismatch for $relativePath")
        }
    }

    private fun relativeFiles(root: Path): Map<String, ByteArray> {
        val rootFile = root.toFile()
        return rootFile.walkTopDown()
            .filter { it.isFile }
            .associate { it.relativeTo(rootFile).path.replace('\\', '/') to it.readBytes() }
    }
}
