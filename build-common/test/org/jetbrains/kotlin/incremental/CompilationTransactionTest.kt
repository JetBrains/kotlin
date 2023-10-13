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

class NonRecoverableCompilationTransactionTest : BaseCompilationTransactionTest() {
    override fun createTransaction() = NonRecoverableCompilationTransaction()

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

class RecoverableCompilationTransactionTest : BaseCompilationTransactionTest() {
    override fun createTransaction() = RecoverableCompilationTransaction(DoNothingBuildReporter, stashDir)

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