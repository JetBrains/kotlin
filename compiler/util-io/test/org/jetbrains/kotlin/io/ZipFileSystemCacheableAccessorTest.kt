/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.FileSystem
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

    private val accessor = ZipFileSystemCacheableAccessor()

    // Keeps strong references to all opened file systems, so they are not evicted from the cache by GC
    // in the middle of a test, and so they can be closed after the test.
    private val openedFileSystems: MutableSet<FileSystem> = identitySet()

    @AfterEach
    fun tearDown() {
        accessor.reset()
        openedFileSystems.forEach { it.close() }
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

    private fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T =
        accessor.withZipFileSystem(zipFile) { fs ->
            openedFileSystems += fs
            action(fs)
        }

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
