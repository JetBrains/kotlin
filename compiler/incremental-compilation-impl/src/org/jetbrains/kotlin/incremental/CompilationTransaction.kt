/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.konan.file.use
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

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
        if (Files.exists(outputFile)) {
            Files.delete(outputFile)
        }
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
    private val fileRelocationRegistry = hashMapOf<Path, Path?>()
    private var filesCounter = 0
    private var successful = false

    override fun registerAddedOrChangedFile(outputFile: Path) {
        if (fileRelocationIsAlreadyRegisteredFor(outputFile)) return
        reporter.measure(BuildTime.BACKUP_OUTPUT) {
            if (Files.exists(outputFile)) {
                val relocatedFilePath = getNextRelocatedFilePath()
                reporter.debug { "Moving the $outputFile file to the stash as $relocatedFilePath" }
                fileRelocationRegistry[outputFile] = relocatedFilePath
                Files.move(outputFile, relocatedFilePath)
            } else {
                reporter.debug { "Marking the $outputFile file as newly added" }
                fileRelocationRegistry[outputFile] = null
            }
        }
    }

    override fun deleteFile(outputFile: Path) {
        if (fileRelocationIsAlreadyRegisteredFor(outputFile)) {
            if (Files.exists(outputFile)) {
                reporter.debug { "Deleting $outputFile" }
                Files.delete(outputFile)
            }
            return
        }
        reporter.measure(BuildTime.BACKUP_OUTPUT) {
            val relocatedFilePath = getNextRelocatedFilePath()
            reporter.debug { "Moving $outputFile to the stash as $relocatedFilePath" }
            fileRelocationRegistry[outputFile] = relocatedFilePath
            Files.move(outputFile, relocatedFilePath)
        }
    }

    private fun getNextRelocatedFilePath(): Path = stashDir.resolve("$filesCounter.backup").also { filesCounter++ }

    private fun fileRelocationIsAlreadyRegisteredFor(outputFile: Path) = outputFile in fileRelocationRegistry

    private fun revertChanges() {
        reporter.debug { "Reverting changes" }
        reporter.measure(BuildTime.RESTORE_OUTPUT_FROM_BACKUP) {
            for ((originPath, relocatedPath) in fileRelocationRegistry) {
                if (relocatedPath == null) {
                    if (Files.exists(originPath)) {
                        Files.delete(originPath)
                    }
                    continue
                }
                Files.move(relocatedPath, originPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun cleanupStash() {
        reporter.debug { "Cleaning up stash" }
        reporter.measure(BuildTime.CLEAN_BACKUP_STASH) {
            Files.walk(stashDir).use {
                it.sorted(Comparator.reverseOrder())
                    .forEach(Files::delete)
            }
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
            cleanupStash()
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