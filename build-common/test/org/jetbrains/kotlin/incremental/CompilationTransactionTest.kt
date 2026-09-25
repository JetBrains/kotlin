/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.incremental.storage.InMemoryStorageInterface
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private class CacheMock(private val throwsException: Boolean = false) : Closeable {
    var closed = false
    override fun close() {
        if (throwsException) {
            throw Exception()
        }
        closed = true
    }
}

private class InMemoryStorageWrapperMock : InMemoryStorageInterface<Any, Any> {
    var reset = false

    override fun applyChanges() {}

    override fun clearChanges() {
        reset = true
    }

    override val storageFile = File("")

    override val keys: Set<Any> = emptySet()

    override fun flush() {}

    override fun close() {}

    override fun remove(key: Any) {}

    override fun set(key: Any, value: Any) {}

    override fun get(key: Any) = null

    override fun contains(key: Any) = false

    override fun clean() {}
}

abstract class BaseCompilationTransactionTest {
    @TempDir
    protected lateinit var stashDir: Path

    @TempDir
    protected lateinit var workingDir: Path

    abstract fun createTransaction(): CompilationTransaction

    protected fun useTransaction(block: CompilationTransaction.() -> Unit) = createTransaction().also { it.runWithin(body = block) }

    @Test
    fun testNoOp() {
        useTransaction {
            // do nothing
        }
    }

    @Test
    fun testCachesClosedOnSuccessfulTransaction() {
        val cacheMock = CacheMock()
        useTransaction {
            cachesManager = cacheMock
            markAsSuccessful()
        }
        assertTrue(cacheMock.closed)
    }

    @Test
    fun testCachesClosedOnNonSuccessfulTransaction() {
        val cacheMock = CacheMock()
        useTransaction {
            cachesManager = cacheMock
        }
        assertTrue(cacheMock.closed)
    }

    @Test
    fun testCachesClosedOnExceptionInsideTransaction() {
        val cacheMock = CacheMock()
        assertThrows<Exception> {
            useTransaction {
                cachesManager = cacheMock
                throw Exception()
            }
        }
        assertTrue(cacheMock.closed)
    }

    @Test
    fun testCachesCloseExceptionIsWrapped() {
        val cacheMock = CacheMock(true)
        assertThrows<CachesManagerCloseException> {
            useTransaction {
                cachesManager = cacheMock
            }
        }
    }

    @Test
    fun testInMemoryWrappersAreResetOnUnsuccessfulTransaction() {
        val inMemoryStorageWrapperMock = InMemoryStorageWrapperMock()
        useTransaction {
            registerInMemoryStorageWrapper(inMemoryStorageWrapperMock)
        }
        assertTrue(inMemoryStorageWrapperMock.reset)
    }

    @Test
    fun testInMemoryWrappersAreResetOnExecutionException() {
        val inMemoryStorageWrapperMock = InMemoryStorageWrapperMock()
        assertThrows<Exception> {
            useTransaction {
                registerInMemoryStorageWrapper(inMemoryStorageWrapperMock)
                markAsSuccessful()
                throw Exception()
            }
        }
        assertTrue(inMemoryStorageWrapperMock.reset)
    }

    @Test
    fun testInMemoryWrappersAreNotResetOnSuccessfulTransaction() {
        val inMemoryStorageWrapperMock = InMemoryStorageWrapperMock()
        useTransaction {
            registerInMemoryStorageWrapper(inMemoryStorageWrapperMock)
            markAsSuccessful()
        }
        assertFalse(inMemoryStorageWrapperMock.reset)
    }
}

class ReadOnlyCompilationTransactionTest : BaseCompilationTransactionTest() {
    override fun createTransaction() = ReadOnlyCompilationTransaction()

    @Test
    fun testRegisteringChangedFileFails() {
        assertThrows<IllegalStateException> {
            useTransaction {
                registerAddedOrChangedFile(workingDir.resolve("1.txt"))
            }
        }
    }

    @Test
    fun testDeletingFileFails() {
        assertThrows<IllegalStateException> {
            useTransaction {
                deleteFile(workingDir.resolve("1.txt"))
            }
        }
    }

    @Test
    fun testDeletingEmptyClassDirectoriesFails() {
        assertThrows<IllegalStateException> {
            useTransaction {
                deleteEmptyClassDirectories()
            }
        }
    }
}

abstract class BaseOutputsAwareCompilationTransactionTest : BaseCompilationTransactionTest() {
    @Test
    fun testDeletingLastFileRemovesEmptyParentDirectoriesUpToOutputRoot() {
        val file = workingDir.resolve("test/bar/A.class")
        Files.createDirectories(file.parent)
        Files.write(file, "something".toByteArray())
        useTransaction {
            deleteFile(file)
            assertTrue(Files.isDirectory(file.parent))
            deleteEmptyClassDirectories()
            markAsSuccessful()
        }
        assertFalse(Files.exists(workingDir.resolve("test")))
        assertTrue(Files.isDirectory(workingDir))
    }

    @Test
    fun testDeletingFileKeepsNonEmptyParentDirectories() {
        val file = workingDir.resolve("test/bar/A.class")
        val sibling = workingDir.resolve("test/B.class")
        Files.createDirectories(file.parent)
        Files.write(file, "something".toByteArray())
        Files.write(sibling, "something".toByteArray())
        useTransaction {
            deleteFile(file)
            deleteEmptyClassDirectories()
            markAsSuccessful()
        }
        assertFalse(Files.exists(workingDir.resolve("test/bar")))
        assertTrue(Files.exists(sibling))
    }

    @Test
    fun testDeletingMultipleFilesRemovesEmptyDirectories() {
        val files = listOf("test/bar/A.class", "test/bar/B.class", "test/baz/deep/C.class", "test/D.class").map { workingDir.resolve(it) }
        for (file in files) {
            Files.createDirectories(file.parent)
            Files.write(file, "something".toByteArray())
        }
        useTransaction {
            for (file in files) {
                deleteFile(file)
            }
            deleteEmptyClassDirectories()
            markAsSuccessful()
        }
        assertFalse(Files.exists(workingDir.resolve("test")))
        assertTrue(Files.isDirectory(workingDir))
    }
}

class NonRecoverableCompilationTransactionTest : BaseOutputsAwareCompilationTransactionTest() {
    override fun createTransaction() = NonRecoverableCompilationTransaction(classesDir = workingDir)

    @Test
    fun testModifyingExistingFileOnSuccess() {
        val file = workingDir.resolve("1.txt")
        Files.write(file, "something".toByteArray())
        useTransaction {
            registerAddedOrChangedFile(file)
            Files.write(file, "other".toByteArray())
            markAsSuccessful()
        }
        assertEquals("other", String(Files.readAllBytes(file)))
    }

    @Test
    fun testAddingNewFileOnSuccess() {
        val file = workingDir.resolve("1.txt")
        useTransaction {
            registerAddedOrChangedFile(file)
            Files.write(file, "other".toByteArray())
            markAsSuccessful()
        }
        assertEquals("other", String(Files.readAllBytes(file)))
    }

    @Test
    fun testDeletingFileOnSuccess() {
        val file = workingDir.resolve("1.txt")
        Files.write(file, "something".toByteArray())
        useTransaction {
            deleteFile(file)
            markAsSuccessful()
        }
        assertFalse(Files.exists(file))
    }

    @Test
    fun testDeletingNotExistingFileOnSuccess() {
        val file = workingDir.resolve("1.txt")
        useTransaction {
            deleteFile(file)
            markAsSuccessful()
        }
        assertFalse(Files.exists(file))
    }

    @Test
    fun testModifyingExistingFileOnFailure() {
        val file = workingDir.resolve("1.txt")
        Files.write(file, "something".toByteArray())
        useTransaction {
            registerAddedOrChangedFile(file)
            Files.write(file, "other".toByteArray())
        }
        assertEquals("other", String(Files.readAllBytes(file)))
    }

    @Test
    fun testAddingNewFileOnFailure() {
        val file = workingDir.resolve("1.txt")
        useTransaction {
            registerAddedOrChangedFile(file)
            Files.write(file, "other".toByteArray())
        }
        assertTrue(Files.exists(file))
    }

    @Test
    fun testDeletingFileOnFailure() {
        val file = workingDir.resolve("1.txt")
        Files.write(file, "something".toByteArray())
        useTransaction {
            deleteFile(file)
        }
        assertFalse(Files.exists(file))
    }

    @Test
    fun testDeletingNotExistingFileOnFailure() {
        val file = workingDir.resolve("1.txt")
        useTransaction {
            deleteFile(file)
        }
        assertFalse(Files.exists(file))
    }
}

class RecoverableCompilationTransactionTest : BaseOutputsAwareCompilationTransactionTest() {
    override fun createTransaction() = RecoverableCompilationTransaction(DoNothingBuildReporter, stashDir, workingDir)

    @Test
    fun testDeletedFileWithRemovedParentDirectoriesIsRestoredOnFailure() {
        val file = workingDir.resolve("test/bar/A.class")
        Files.createDirectories(file.parent)
        Files.write(file, "something".toByteArray())
        useTransaction {
            deleteFile(file)
            deleteEmptyClassDirectories()
            assertFalse(Files.exists(workingDir.resolve("test")))
        }
        assertEquals("something", String(Files.readAllBytes(file)))
    }

    @Test
    fun testModifyingExistingFileOnSuccess() {
        val file = workingDir.resolve("1.txt")
        Files.write(file, "something".toByteArray())
        useTransaction {
            registerAddedOrChangedFile(file)
            Files.write(file, "other".toByteArray())
            markAsSuccessful()
        }
        assertEquals("other", String(Files.readAllBytes(file)))
    }

    @Test
    fun testAddingNewFileOnSuccess() {
        val file = workingDir.resolve("1.txt")
        useTransaction {
            registerAddedOrChangedFile(file)
            Files.write(file, "other".toByteArray())
            markAsSuccessful()
        }
        assertEquals("other", String(Files.readAllBytes(file)))
    }

    @Test
    fun testDeletingFileOnSuccess() {
        val file = workingDir.resolve("1.txt")
        Files.write(file, "something".toByteArray())
        useTransaction {
            deleteFile(file)
            markAsSuccessful()
        }
        assertFalse(Files.exists(file))
    }

    @Test
    fun testDeletingNotExistingFileOnSuccess() {
        val file = workingDir.resolve("1.txt")
        useTransaction {
            deleteFile(file)
            markAsSuccessful()
        }
        assertFalse(Files.exists(file))
    }

    @Test
    fun testModifyingExistingFileOnFailure() {
        val file = workingDir.resolve("1.txt")
        Files.write(file, "something".toByteArray())
        useTransaction {
            registerAddedOrChangedFile(file)
            Files.write(file, "other".toByteArray())
        }
        assertEquals("something", String(Files.readAllBytes(file)))
    }

    @Test
    fun testAddingNewFileOnFailure() {
        val file = workingDir.resolve("1.txt")
        useTransaction {
            registerAddedOrChangedFile(file)
            Files.write(file, "other".toByteArray())
        }
        assertFalse(Files.exists(file))
    }

    @Test
    fun testDeletingFileOnFailure() {
        val file = workingDir.resolve("1.txt")
        Files.write(file, "something".toByteArray())
        useTransaction {
            deleteFile(file)
        }
        assertEquals("something", String(Files.readAllBytes(file)))
    }

    @Test
    fun testDeletingNotExistingFileOnFailure() {
        val file = workingDir.resolve("1.txt")
        useTransaction {
            deleteFile(file)
        }
        assertFalse(Files.exists(file))
    }

    @Test
    fun testChangesAreRevertedOnExecutionException() {
        val file1 = workingDir.resolve("1.txt")
        val file2 = workingDir.resolve("2.txt")
        Files.write(file1, "something".toByteArray())
        assertThrows<Exception> {
            useTransaction {
                registerAddedOrChangedFile(file1)
                Files.write(file1, "other".toByteArray())
                registerAddedOrChangedFile(file2)
                Files.write(file2, "other".toByteArray())
                markAsSuccessful()
                throw Exception()
            }
        }
        assertEquals("something", String(Files.readAllBytes(file1)))
        assertFalse(Files.exists(file2))
    }

    @Test
    fun testChangesAreRevertedOnCachesCloseException() {
        val file1 = workingDir.resolve("1.txt")
        val file2 = workingDir.resolve("2.txt")
        Files.write(file1, "something".toByteArray())
        assertThrows<CachesManagerCloseException> {
            useTransaction {
                cachesManager = CacheMock(true)
                registerAddedOrChangedFile(file1)
                Files.write(file1, "other".toByteArray())
                registerAddedOrChangedFile(file2)
                Files.write(file2, "other".toByteArray())
                markAsSuccessful()
            }
        }
        assertEquals("something", String(Files.readAllBytes(file1)))
        assertFalse(Files.exists(file2))
    }
}
