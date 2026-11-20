/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.ClosedFileSystemException
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipException
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class ZipFileSystemCacheableAccessorTest {
    @TempDir
    lateinit var tempDir: Path

    private var accessor = ZipFileSystemCacheableAccessor(cacheLimit = 64)

    @AfterEach
    fun tearDown() {
        accessor.reset()
    }

    @Test
    fun `concurrent access to the same zip creates a single file system`() {
        val zip = createZip("lib", mapOf(ENTRY to "content"))
        val fileSystems: MutableSet<FileSystem> = identitySet()

        runConcurrently {
            repeat(ITERATIONS) {
                withZipFileSystem(zip) { fs ->
                    fileSystems += fs
                    assertEquals("content", fs.getPath(ENTRY).readText())
                }
            }
        }

        assertEquals(1, fileSystems.size)
    }

    @Test
    fun `concurrent access to different zips does not mix file systems`() {
        val zips = List(8) { index -> createZip("lib$index", mapOf(ENTRY to "content$index")) }
        val fileSystemsPerZip = ConcurrentHashMap<Path, MutableSet<FileSystem>>()

        runConcurrently { threadIndex ->
            repeat(ITERATIONS) { iteration ->
                val zipIndex = (threadIndex + iteration) % zips.size
                val zip = zips[zipIndex]
                withZipFileSystem(zip) { fs ->
                    fileSystemsPerZip.computeIfAbsent(zip) { identitySet() } += fs
                    assertEquals("content$zipIndex", fs.getPath(ENTRY).readText())
                }
            }
        }

        assertEquals(zips.toSet(), fileSystemsPerZip.keys)
        fileSystemsPerZip.values.forEach { assertEquals(1, it.size) }

        val allFileSystems: MutableSet<FileSystem> = identitySet()
        fileSystemsPerZip.values.forEach { allFileSystems += it }
        assertEquals(zips.size, allFileSystems.size)
    }

    @Test
    fun `actions for the same zip run in parallel`() {
        val zip = createZip("lib", mapOf(ENTRY to "content"))
        val barrier = CyclicBarrier(THREADS)

        runConcurrently {
            withZipFileSystem(zip) { fs ->
                // Would time out if the actions were serialized.
                barrier.await(30, TimeUnit.SECONDS)
                assertEquals("content", fs.getPath(ENTRY).readText())
            }
        }
    }

    @Test
    fun `nested access does not deadlock`() {
        val zipA = createZip("libA", mapOf(ENTRY to "contentA"))
        val zipB = createZip("libB", mapOf(ENTRY to "contentB"))

        runConcurrently { threadIndex ->
            val (outer, inner) = if (threadIndex % 2 == 0) zipA to zipB else zipB to zipA
            val outerContent = if (outer == zipA) "contentA" else "contentB"
            val innerContent = if (inner == zipA) "contentA" else "contentB"

            repeat(ITERATIONS) {
                withZipFileSystem(outer) { outerFs ->
                    withZipFileSystem(inner) { innerFs ->
                        assertEquals(innerContent, innerFs.getPath(ENTRY).readText())
                        withZipFileSystem(outer) { reenteredFs ->
                            assertSame(outerFs, reenteredFs)
                        }
                    }
                    assertEquals(outerContent, outerFs.getPath(ENTRY).readText())
                }
            }
        }
    }

    @Test
    fun `failure to open a zip is not cached`() {
        val zip = tempDir.resolve("malformed.zip")
        zip.writeBytes(byteArrayOf(1, 2, 3, 4))

        runConcurrently {
            assertThrows<ZipException> { withZipFileSystem(zip) { } }
        }

        createZip(zip, mapOf(ENTRY to "content"))

        runConcurrently {
            withZipFileSystem(zip) { fs ->
                assertEquals("content", fs.getPath(ENTRY).readText())
            }
        }
    }

    @Test
    fun `exception thrown by action propagates and keeps cache intact`() {
        val zip = createZip("lib", mapOf(ENTRY to "content"))
        val fileSystems: MutableSet<FileSystem> = identitySet()

        runConcurrently { threadIndex ->
            repeat(ITERATIONS) {
                if (threadIndex % 2 == 0) {
                    val failure = ActionFailure()
                    val thrown = assertThrows<ActionFailure> {
                        withZipFileSystem(zip) { fs ->
                            fileSystems += fs
                            throw failure
                        }
                    }
                    assertSame(failure, thrown)
                } else {
                    withZipFileSystem(zip) { fs ->
                        fileSystems += fs
                        assertEquals("content", fs.getPath(ENTRY).readText())
                    }
                }
            }
        }

        withZipFileSystem(zip) { fs ->
            fileSystems += fs
            assertEquals("content", fs.getPath(ENTRY).readText())
        }

        assertEquals(1, fileSystems.size)
    }

    @Test
    fun `reset drops cached file systems`() {
        val zip = createZip("lib", mapOf(ENTRY to "content"))

        val first = withZipFileSystem(zip) { it }
        assertSame(first, withZipFileSystem(zip) { it })

        accessor.reset()

        val second = withZipFileSystem(zip) { it }
        assertNotSame(first, second)
        assertEquals("content", second.getPath(ENTRY).readText())
    }

    @Test
    fun `result of action is returned`() {
        val zip = createZip("lib", mapOf(ENTRY to "content"))

        runConcurrently { threadIndex ->
            repeat(ITERATIONS) { iteration ->
                val result = withZipFileSystem(zip) { fs -> fs.getPath(ENTRY).readText() + "-$threadIndex-$iteration" }
                assertEquals("content-$threadIndex-$iteration", result)
            }
        }
    }

    @Test
    fun `count limit evicts the least recently accessed idle file system`() {
        accessor = ZipFileSystemCacheableAccessor(cacheLimit = 2)
        val zipA = createZip("a", mapOf(ENTRY to "a"))
        val zipB = createZip("b", mapOf(ENTRY to "b"))
        val zipC = createZip("c", mapOf(ENTRY to "c"))
        val first = withZipFileSystem(zipA) { it }
        val second = withZipFileSystem(zipB) { it }
        assertSame(first, withZipFileSystem(zipA) { it })

        val third = withZipFileSystem(zipC) { it }

        assertTrue(first.isOpen)
        assertFalse(second.isOpen)
        assertTrue(third.isOpen)
        assertNotSame(second, withZipFileSystem(zipB) { it })
    }

    @Test
    fun `many archives do not accumulate open file systems`() {
        accessor = ZipFileSystemCacheableAccessor(cacheLimit = 2)
        val fileSystems = List(100) { index ->
            val zip = createZip("lib$index", mapOf(ENTRY to "content"))
            withZipFileSystem(zip) { it }
        }

        assertEquals(2, fileSystems.count { it.isOpen })
        assertTrue(fileSystems.takeLast(2).all { it.isOpen })
        accessor.reset()
        assertTrue(fileSystems.none { it.isOpen })
    }

    @Test
    fun `active file systems survive nested access beyond the count limit`() {
        accessor = ZipFileSystemCacheableAccessor(cacheLimit = 1)
        val zipA = createZip("a", mapOf(ENTRY to "a"))
        val zipB = createZip("b", mapOf(ENTRY to "b"))
        val first = withZipFileSystem(zipA) { outer ->
            val second = withZipFileSystem(zipB) { inner ->
                assertEquals("a", outer.getPath(ENTRY).readText())
                withZipFileSystem(zipA) { assertSame(outer, it) }
                inner
            }
            assertFalse(second.isOpen)
            assertTrue(outer.isOpen)
            outer
        }
        assertTrue(first.isOpen)
        assertSame(first, withZipFileSystem(zipA) { it })
    }

    @Test
    fun `zero count limit releases the last concurrent user only`() {
        accessor = ZipFileSystemCacheableAccessor(cacheLimit = 0)
        val zip = createZip("lib", mapOf(ENTRY to "content"))
        val barrier = CyclicBarrier(THREADS)
        val fileSystems: MutableSet<FileSystem> = identitySet()

        runConcurrently {
            withZipFileSystem(zip) { fs ->
                fileSystems += fs
                barrier.await(30, TimeUnit.SECONDS)
                assertEquals("content", fs.getPath(ENTRY).readText())
            }
        }

        assertEquals(1, fileSystems.size)
        assertTrue(fileSystems.none { it.isOpen })
    }

    @Test
    fun `action failure releases an uncacheable entry`() {
        accessor = ZipFileSystemCacheableAccessor(cacheLimit = 0)
        val zip = createZip("lib", mapOf(ENTRY to "content"))
        val failure = ActionFailure()
        lateinit var fileSystem: FileSystem

        assertSame(failure, assertThrows<ActionFailure> {
            withZipFileSystem(zip) { fs ->
                fileSystem = fs
                throw failure
            }
        })

        assertFalse(fileSystem.isOpen)
        withZipFileSystem(zip) { assertEquals("content", it.getPath(ENTRY).readText()) }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Cannot move a directory containing an open ZIP file on Windows")
    fun `eviction close failure of the used archive is ignored`() {
        accessor = ZipFileSystemCacheableAccessor(cacheLimit = 0)
        val zip = createZipInOwnDirectory("lib")
        lateinit var fileSystem: FileSystem

        val result = withZipFileSystem(zip) { fs ->
            fileSystem = fs
            makeZipFileSystemCloseFail(zip, fs)
            "OK"
        }

        assertEquals("OK", result)
        assertFalse(fileSystem.isOpen)
        accessor.reset()
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Cannot move a directory containing an open ZIP file on Windows")
    fun `eviction close failure of another archive is ignored`() {
        accessor = ZipFileSystemCacheableAccessor(cacheLimit = 1)
        val zipA = createZipInOwnDirectory("a")
        val zipB = createZip("b", mapOf(ENTRY to "b"))
        val first = withZipFileSystem(zipA) { fs ->
            makeZipFileSystemCloseFail(zipA, fs)
            fs
        }

        assertEquals("b", withZipFileSystem(zipB) { it.getPath(ENTRY).readText() })

        assertFalse(first.isOpen)
        accessor.reset()
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Cannot move a directory containing an open ZIP file on Windows")
    fun `eviction close failure does not replace action failure`() {
        accessor = ZipFileSystemCacheableAccessor(cacheLimit = 0)
        val zip = createZipInOwnDirectory("lib")
        val failure = ActionFailure()

        val thrown = assertThrows<ActionFailure> {
            withZipFileSystem(zip) { fs ->
                makeZipFileSystemCloseFail(zip, fs)
                throw failure
            }
        }

        assertSame(failure, thrown)
        accessor.reset()
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Cannot move a directory containing an open ZIP file on Windows")
    fun `reset closes and drops all file systems despite close failures`() {
        val failingZips = List(2) { index -> createZipInOwnDirectory("failing$index") }
        val zip = createZip("lib", mapOf(ENTRY to "content"))
        val failingFileSystems = failingZips.map { failingZip ->
            withZipFileSystem(failingZip) { fs ->
                makeZipFileSystemCloseFail(failingZip, fs)
                fs
            }
        }
        val first = withZipFileSystem(zip) { it }

        val thrown = assertThrows<IOException> { accessor.reset() }

        assertEquals(2, thrown.suppressed.size)
        assertTrue(failingFileSystems.none { it.isOpen })
        assertFalse(first.isOpen)
        accessor.reset()
        assertNotSame(first, withZipFileSystem(zip) { it })
    }

    @Test
    fun `reset closes active file systems and subsequent access opens a new one`() {
        val zip = createZip("lib", mapOf(ENTRY to "content"))
        val first = withZipFileSystem(zip) { outer ->
            withZipFileSystem(zip) { inner ->
                val entryPath = outer.getPath(ENTRY)
                accessor.reset()
                assertFalse(outer.isOpen)
                assertSame(outer, inner)
                assertThrows<ClosedFileSystemException> { entryPath.readText() }
                withZipFileSystem(zip) { replacement ->
                    assertNotSame(outer, replacement)
                    assertEquals("content", replacement.getPath(ENTRY).readText())
                }
            }
            assertFalse(outer.isOpen)
            outer
        }
        assertFalse(first.isOpen)
        assertTrue(withZipFileSystem(zip) { it }.isOpen)
    }

    @Test
    fun `concurrent resets can fail an active action`() {
        val zip = createZip("lib", mapOf(ENTRY to "content"))
        assertThrows<ClosedFileSystemException> {
            withZipFileSystem(zip) { fs ->
                val entryPath = fs.getPath(ENTRY)
                runConcurrently { accessor.reset() }
                assertFalse(fs.isOpen)
                entryPath.readText()
            }
        }
        withZipFileSystem(zip) { fs ->
            assertEquals("content", fs.getPath(ENTRY).readText())
        }
    }

    @Test
    fun `negative cache limits are rejected`() {
        assertThrows<IllegalArgumentException> { ZipFileSystemCacheableAccessor(cacheLimit = -1) }
    }

    private fun createZipInOwnDirectory(name: String): Path =
        createZip(tempDir.resolve("$name-archive").createDirectories().resolve("$name.zip"), mapOf(ENTRY to "content"))

    private fun makeZipFileSystemCloseFail(zip: Path, fileSystem: FileSystem) {
        fileSystem.getPath(ENTRY).writeText("updated")
        // Closing must write the modified archive back, but its original parent directory no longer exists.
        Files.move(zip.parent, zip.parent.resolveSibling("${zip.parent.fileName}-moved"))
    }

    private fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T =
        accessor.withZipFileSystem(zipFile, action)

    private fun createZip(name: String, entries: Map<String, String>): Path =
        createZip(tempDir.resolve("$name.zip"), entries)

    private fun createZip(zipFile: Path, entries: Map<String, String>): Path {
        val contentDir = tempDir.resolve("${zipFile.fileName}-content").createDirectories()
        entries.forEach { (path, text) -> contentDir.resolve(path).writeText(text) }
        contentDir.zipDirAs(zipFile)
        return zipFile
    }

    private fun runConcurrently(threads: Int = THREADS, action: (threadIndex: Int) -> Unit) {
        val executor = Executors.newFixedThreadPool(threads)
        try {
            val startLatch = CountDownLatch(1)
            val futures = List(threads) { threadIndex ->
                executor.submit {
                    startLatch.await()
                    action(threadIndex)
                }
            }
            startLatch.countDown()
            futures.forEach { it.get(1, TimeUnit.MINUTES) }
        } finally {
            executor.shutdownNow()
        }
    }

    private class ActionFailure : RuntimeException()

    companion object {
        private const val THREADS = 16
        private const val ITERATIONS = 100
        private const val ENTRY = "entry.txt"

        private fun <T> identitySet(): MutableSet<T> = Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))
    }
}
