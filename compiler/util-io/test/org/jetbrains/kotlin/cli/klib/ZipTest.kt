/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.cli.klib.Entry.Directory
import org.jetbrains.kotlin.cli.klib.Entry.File
import org.jetbrains.kotlin.io.unzipTo
import org.jetbrains.kotlin.io.zipDirAs
import org.jetbrains.kotlin.io.zipDirAsInternal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.Collections
import java.util.TreeMap
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import kotlin.collections.Iterable
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.measureTime

class ZipTest {
    private lateinit var tmpDir: Path

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        tmpDir = createTempDirectory(testInfo.testClass.get().simpleName + "_" + testInfo.testMethod.get().name)
    }

    @OptIn(ExperimentalPathApi::class)
    @AfterEach
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun testKlibCompressionSimulation() = doTestWithPayload {
        simulationOfKlibPayload()
    }

    @Test
    @Disabled
    @OptIn(ExperimentalPathApi::class)
    fun benchmarkKlibCompressionSimulation() {
        val uncompressed = Root(tmpDir).simulationOfKlibPayload()
        val compressed = tmpDir.resolve("compressed.zip")

        runBenchWithWarmup(
            name = "KLIB compression simulation",
            warmupRounds = 20,
            benchmarkRounds = 10,
            pre = System::gc,
            post = { compressed.deleteIfExists() },
        ) {
            uncompressed.zipDirAs(compressed)
        }
    }

    // Simulate a real-world KLIB.
    private fun HasPath.simulationOfKlibPayload(): Path =
        directory("default") {
            directory("ir") {
                file("bodies.knb", 3871899u)
                file("debugInfo.knd", 1251075u)
                file("files.knf", 384889u)
                file("irDeclarations.knd", 1217974u)
                file("signatures.knt", 1014653u)
                file("strings.knt", 497269u)
            }
            directory("linkdata") {
                directory("package_foo") {
                    file("00_foo.knm", 1003u)
                    file("01_foo.knm", 3194u)
                }
                directory("package_foo.bar") {
                    file("00_bar.knm", 1003u)
                }
                file("module", 905u)
            }
            directory("resources")
            directory("targets") {
                directory("macos_arm64") {
                    directory("included")
                    directory("native")
                }
            }
            file("manifest", 438u)
        }

    @Test
    fun testSymlinks() = doTestWithPayload {
        directory("dir1") {
            file("file1", 100u)
        }
        directory("dir2") {
            symlink("dir2.link1", "./")
            symlink("dir1.link1", "../dir1")

            file("file2", 100u)

            symlink("file2.link1", "file2")
            symlink("file2.link2", "./file2")
            symlink("file2.link3", "../dir2/file2")

            symlink("file1.link1", "../dir1/file1")

            symlink("file1.link2", path.resolve("../dir1/file1").absolutePathString()) // absolute path in symlink
        }
    }

    @Test
    fun testSymlinkToFileOutsideCompressedDirectory1() {
        val externalFile = tmpDir.resolve("externalFile").apply { writeBytes(Random(System.nanoTime()).nextBytes(100)) }

        assertThrows<ZipException> {
            doTestWithPayload {
                symlink("link", externalFile.absolutePathString())
            }
        }
    }

    @Test
    fun testSymlinkToFileOutsideCompressedDirectory2() {
        val externalFile = tmpDir.resolve("externalFile").apply { writeBytes(Random(System.nanoTime()).nextBytes(100)) }

        assertThrows<ZipException> {
            doTestWithPayload {
                symlink("link", "../${externalFile.name}")
            }
        }
    }

    @Test
    fun testSymlinkToDirectoryOutsideCompressedDirectory1() {
        val externalDir = tmpDir.resolve("externalDir").apply { createDirectories() }

        assertThrows<ZipException> {
            doTestWithPayload {
                symlink("link", externalDir.absolutePathString())
            }
        }
    }

    @Test
    fun testSymlinkToDirectoryOutsideCompressedDirectory2() {
        val externalDir = tmpDir.resolve("externalDir").apply { createDirectories() }

        assertThrows<ZipException> {
            doTestWithPayload {
                symlink("link", "../${externalDir.name}")
            }
        }
    }

    @Test
    fun testStableFileTreeTraversal() {
        val originalPayloadPath = Root(tmpDir).let {
            it.directory("original") {
                simulationOfKlibPayload()
            }
        }

        // Compress with stable traversal order.
        val compressedWithStableTraversalPayloadPath = tmpDir.resolve("compressed-stable.zip")
        zipDirAsInternal(
            dirPath = originalPayloadPath,
            zipFilePath = compressedWithStableTraversalPayloadPath
        )

        fun assertSameCompressedPayload(shuffledPayloadPath: Path) {
            assertEquals(compressedWithStableTraversalPayloadPath.fileSize(), shuffledPayloadPath.fileSize())

            val stablePayloadBytes = compressedWithStableTraversalPayloadPath.readBytes()
            val shuffledPayloadBytes = shuffledPayloadPath.readBytes()

            assertTrue(stablePayloadBytes.contentEquals(shuffledPayloadBytes))
        }

        repeat(5) { index ->
            val compressedWithRandomTraversalPayloadPath = tmpDir.resolve("compressed-random-$index.zip")
            zipDirAsInternal(
                dirPath = originalPayloadPath,
                zipFilePath = compressedWithRandomTraversalPayloadPath,
                shuffle = Iterable<Path>::shuffled
            )

            assertSameCompressedPayload(compressedWithRandomTraversalPayloadPath)
        }

        repeat(5) { index ->
            val compressedWithReverseTraversalPayloadPath = tmpDir.resolve("compressed-reverse-$index.zip")
            zipDirAsInternal(
                dirPath = originalPayloadPath,
                zipFilePath = compressedWithReverseTraversalPayloadPath,
                shuffle = Iterable<Path>::reversed
            )

            assertSameCompressedPayload(compressedWithReverseTraversalPayloadPath)
        }

        repeat(5) { index ->
            val compressedWithRotatedTraversalPayloadPath = tmpDir.resolve("compressed-rotated-$index.zip")
            zipDirAsInternal(
                dirPath = originalPayloadPath,
                zipFilePath = compressedWithRotatedTraversalPayloadPath,
                shuffle = { Collections.rotate(it, it.size / 2 + index) }
            )

            assertSameCompressedPayload(compressedWithRotatedTraversalPayloadPath)
        }
    }

    private inline fun doTestWithPayload(buildOriginalPayload: HasPath.() -> Unit) {
        val originalPayload = Root(tmpDir).let {
            it.directory("original") {
                buildOriginalPayload()
            }
        }

        // Compress.
        val compressedPayload = tmpDir.resolve("compressed.zip")
        originalPayload.zipDirAs(compressedPayload)

        // Verify uncompressed entries.
        verifyCompressedPayload(compressedPayload)

        // Uncompress.
        val uncompressedPayload = tmpDir.resolve("uncompressed")
        compressedPayload.unzipTo(uncompressedPayload)

        // Compare entries.
        compareEntries(originalPayload, uncompressedPayload)
    }

    private fun verifyCompressedPayload(compressed: Path) {
        fun assertTime(actual: FileTime?) {
            if (actual == null) {
                // OK, time is not set.
            } else {
                val timeMillis = actual.toMillis()
                if (timeMillis == 0L || timeMillis == -1L) {
                    // OK.
                } else {
                    fail("Unexpected time: $actual ($timeMillis) for $compressed.")
                }
            }
        }

        Files.newInputStream(compressed).use { inputStream ->
            ZipInputStream(inputStream).use { zipInputStream ->
                var entry: ZipEntry? = zipInputStream.nextEntry
                while (entry != null) {
                    assertTime(entry.creationTime)
                    assertTime(entry.lastModifiedTime)
                    assertTime(entry.lastAccessTime)

                    if (!entry.isDirectory) {
                        assertTrue(entry.method > 0, "Compression rate is not set for ${entry.name} in $compressed.")
                    }

                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
        }
    }

    private fun compareEntries(original: Path, uncompressed: Path) {
        val originalAttributes = Files.readAttributes(original, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        val uncompressedAttributes = Files.readAttributes(uncompressed, BasicFileAttributes::class.java)

        when {
            originalAttributes.isDirectory -> {
                assertTrue(uncompressedAttributes.isDirectory, "Not a directory: $uncompressed")
                compareEntriesAsSubDirs(original.listDirectoryEntries(), uncompressed.listDirectoryEntries())
            }
            originalAttributes.isRegularFile -> {
                assertTrue(uncompressedAttributes.isRegularFile, "Not a regular file: $uncompressed")
                compareEntriesAsFiles(original, uncompressed)
            }
            originalAttributes.isSymbolicLink -> {
                // Yes, we don't store symlinks in KLIB archives. Instead, we just copy the original file's content.
                when {
                    uncompressedAttributes.isRegularFile -> compareEntriesAsFiles(original, uncompressed)
                    uncompressedAttributes.isDirectory -> compareEntriesAsSubDirs(emptyList(), uncompressed.listDirectoryEntries())
                    else -> error("Unsupported file type: $uncompressed")
                }
            }
            else -> error("Unsupported file type: $original")
        }
    }

    private fun compareEntriesAsSubDirs(originalSubDirs: List<Path>, uncompressedSubDirs: List<Path>) {
        val originalSubDirEntries: Map<String, Path> = originalSubDirs.associateByTo(TreeMap()) { it.name }
        val uncompressedSubDirEntries: Map<String, Path> = uncompressedSubDirs.associateByTo(TreeMap()) { it.name }

        assertEquals(
            originalSubDirEntries.keys,
            originalSubDirEntries.keys,
            "Different subdirs: ${originalSubDirEntries.keys} vs ${originalSubDirEntries.keys}"
        )

        for ((originalEntry, uncompressedEntry) in originalSubDirEntries.values.zip(uncompressedSubDirEntries.values)) {
            compareEntries(originalEntry, uncompressedEntry)
        }
    }

    private fun compareEntriesAsFiles(original: Path, uncompressed: Path) {
        assertTrue(original.readBytes().contentEquals(uncompressed.readBytes()), "Different contents: $original vs $uncompressed")
    }
}

private interface HasPath {
    val path: Path
}

private class Root(override val path: Path) : HasPath

private sealed interface Entry : HasPath {
    fun create()

    class Directory(override val path: Path) : Entry {
        override fun create() {
            path.createDirectories()
        }
    }

    class File(override val path: Path, private val size: UInt) : Entry {
        override fun create() {
            path.writeBytes(Random(System.nanoTime()).nextBytes(size.toInt()))
        }
    }

    class Symlink(override val path: Path, val targetPath: Path) : Entry {
        override fun create() {
            path.createSymbolicLinkPointingTo(targetPath)
        }
    }
}

private inline fun HasPath.directory(name: String, block: HasPath.() -> Unit = {}): Path {
    val directory = Directory(this.path.resolve(name))
    directory.create()
    directory.block()
    return directory.path
}

private fun HasPath.file(name: String, size: UInt) {
    val file = File(this.path.resolve(name), size)
    file.create()
}

private fun HasPath.symlink(name: String, target: String) {
    val symlink = Entry.Symlink(this.path.resolve(name), Path(target))
    symlink.create()
}

private fun runBenchWithWarmup(
    name: String,
    warmupRounds: Int,
    benchmarkRounds: Int,
    pre: () -> Unit,
    post: () -> Unit,
    bench: () -> Unit,
) {
    println("Run $name benchmark")
    println("Warmup: $warmupRounds times...")

    repeat(warmupRounds) {
        println("W: ${it + 1} out of $warmupRounds")
        pre()
        bench()
        post()
    }

    var total = Duration.ZERO

    println("Run bench: $benchmarkRounds times...")

    repeat(benchmarkRounds) {
        print("B: ${it + 1} out of $benchmarkRounds ")
        pre()
        val duration = measureTime { bench() }
        println("takes $duration")
        post()
        total += duration
    }

    println("$name takes ${total / benchmarkRounds}")
}
