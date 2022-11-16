/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.warn
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.konan.file.use
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

interface CompilationTransaction : Closeable {
    fun registerAddedOrChangedFile(outputFile: Path)

    fun deleteFile(outputFile: Path)

    fun markAsSuccessful()
}

class DummyCompilationTransaction : CompilationTransaction {
    override fun registerAddedOrChangedFile(outputFile: Path) {
        // do nothing
    }

    override fun deleteFile(outputFile: Path) {
        Files.delete(outputFile)
    }

    override fun markAsSuccessful() {
        // do nothing
    }

    override fun close() {
        // do nothing
    }

}

class RecoverableCompilationTransaction(
    private val reporter: BuildReporter,
    private val stashDir: Path,
) : CompilationTransaction {
    private var successful = false

    override fun registerAddedOrChangedFile(outputFile: Path) {
        reporter.warn { "$outputFile is being added or changed" }
    }

    override fun deleteFile(outputFile: Path) {
        reporter.warn { "Deleting $outputFile" }
        Files.delete(outputFile)
    }

    private fun revertChanges() {
        reporter.warn { "Reverting changes" }
    }

    private fun cleanupStash() {
        reporter.warn { "Cleaning up stash" }
        Files.walk(stashDir).use {
            it.sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
    }

    override fun markAsSuccessful() {
        successful = true
    }

    override fun close() {
        if (successful) {
            cleanupStash()
        } else {
            revertChanges()
        }
    }
}

class TransactionOutputsRegistrar(
    private val transaction: CompilationTransaction,
    private val origin: OutputItemsCollector
) : OutputItemsCollector {
    override fun add(sourceFiles: MutableCollection<File>, outputFile: File) {
        transaction.registerAddedOrChangedFile(outputFile.toPath())
        origin.add(sourceFiles, outputFile)
    }
}