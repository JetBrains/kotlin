/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class TransactionOutputsRegistrarTest {
    @TempDir
    private lateinit var stashDir: Path

    @TempDir
    private lateinit var workingDir: Path

    private class MockOutputItemsCollector : OutputItemsCollector {
        val addedOutputItems = mutableSetOf<Pair<Collection<File>, File>>()

        override fun add(sourceFiles: Collection<File>, outputFile: File) {
            addedOutputItems.add(sourceFiles to outputFile)
        }
    }

    @Test
    fun testSuccessfulTransaction() {
        val mockCollector = MockOutputItemsCollector()
        val srcFile = workingDir.resolve("a.kt")
        val outputFile = workingDir.resolve("AKt.class")
        RecoverableCompilationTransaction(DoNothingBuildReporter, stashDir).use {
            val registrar = TransactionOutputsRegistrar(it, mockCollector)
            registrar.add(listOf(srcFile.toFile()), outputFile.toFile())
            Files.write(outputFile, "blah-blah".toByteArray())
            it.markAsSuccessful()
        }
        assertIterableEquals(
            setOf(listOf(srcFile.toFile()) to outputFile.toFile()),
            mockCollector.addedOutputItems,
            "TransactionOutputsRegistrar should call the origin"
        )
        assertEquals("blah-blah", String(Files.readAllBytes(outputFile)))
    }

    @Test
    fun testFailedTransaction() {
        val mockCollector = MockOutputItemsCollector()
        val srcFile = workingDir.resolve("a.kt")
        val outputFile = workingDir.resolve("AKt.class")
        RecoverableCompilationTransaction(DoNothingBuildReporter, stashDir).use {
            val registrar = TransactionOutputsRegistrar(it, mockCollector)
            registrar.add(listOf(srcFile.toFile()), outputFile.toFile())
            Files.write(outputFile, "blah-blah".toByteArray())
        }
        assertIterableEquals(
            setOf(listOf(srcFile.toFile()) to outputFile.toFile()),
            mockCollector.addedOutputItems,
            "TransactionOutputsRegistrar should call the origin"
        )
        assertFalse(Files.exists(outputFile), "Output file wasn't reverted")
    }
}