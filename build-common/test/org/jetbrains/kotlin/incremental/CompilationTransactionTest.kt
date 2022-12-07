/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

abstract class BaseCompilationTransactionTest {
    @TempDir
    protected lateinit var stashDir: Path

    @TempDir
    protected lateinit var workingDir: Path

    abstract fun createTransaction(): CompilationTransaction

    protected fun useTransaction(block: CompilationTransaction.() -> Unit) = createTransaction().also { it.use(block) }

    @Test
    fun testNoOp() {
        useTransaction() {
            // do nothing
        }
    }
}

class DummyCompilationTransactionTest : BaseCompilationTransactionTest() {
    override fun createTransaction() = DummyCompilationTransaction()

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
}