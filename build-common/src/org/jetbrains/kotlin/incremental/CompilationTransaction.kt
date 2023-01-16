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

/**
 * A compilation transaction that is able to track compilation result files changes and maybe to revert them back.
 */
interface CompilationTransaction : Closeable {
    /**
     * This method should be called before creating a new file or changing an existing file.
     */
    fun registerAddedOrChangedFile(outputFile: Path)

    /**
     * This method should be used to perform a file removal.
     */
    fun deleteFile(outputFile: Path)

    /**
     * Marks the transaction as successful, so it should not revert changes if it is able to perform revert.
     */
    fun markAsSuccessful()
}

fun CompilationTransaction.write(file: Path, writeAction: () -> Unit) {
    registerAddedOrChangedFile(file)
    writeAction()
}

fun CompilationTransaction.writeText(file: Path, text: String) {
    writeBytes(file, text.toByteArray())
}

fun CompilationTransaction.writeBytes(file: Path, array: ByteArray) {
    write(file) {
        if (!Files.exists(file.parent)) {
            Files.createDirectories(file.parent)
        }
        Files.write(file, array)
    }
}

/**
 * A dummy implementation of compilation transaction
 */
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

/**
 * A recoverable implementation of compilation transaction.
 * Tracks all files changes during a compilation and reverts them back if [markAsSuccessful] isn't called
 * In the case of a successful compilation [stashDir] is removed after is compilation.
 * In the case of an unsuccessful compilation [stashDir] is also removed, but the backed-up files restored to their origin location.
 */
class RecoverableCompilationTransaction(
    private val reporter: BuildReporter,
    private val stashDir: Path,
) : CompilationTransaction {
    private val fileRelocationRegistry = hashMapOf<Path, Path?>()
    private var filesCounter = 0
    private var successful = false

    /**
     * Moves the original [outputFile] before change to the [stashDir].
     * If the [outputFile] doesn't exist then it's marked to be removed if the transaction is unsuccessful.
     */
    override fun registerAddedOrChangedFile(outputFile: Path) {
        if (isFileRelocationIsAlreadyRegisteredFor(outputFile)) return
        reporter.measure(BuildTime.PRECISE_BACKUP_OUTPUT) {
            if (Files.exists(outputFile)) {
                stashFile(outputFile)
            } else {
                reporter.debug { "Marking the $outputFile file as newly added" }
                fileRelocationRegistry[outputFile] = null
            }
        }
    }

    /**
     * Moves the original [outputFile] to the [stashDir] instead of deleting.
     */
    override fun deleteFile(outputFile: Path) {
        if (!Files.exists(outputFile)) {
            return
        }
        if (isFileRelocationIsAlreadyRegisteredFor(outputFile)) {
            reporter.debug { "Deleting $outputFile" }
            Files.delete(outputFile)
            return
        }
        reporter.measure(BuildTime.PRECISE_BACKUP_OUTPUT) {
            stashFile(outputFile)
        }
    }

    private fun stashFile(outputFile: Path) {
        val relocatedFilePath = getNextRelocatedFilePath()
        reporter.debug { "Moving $outputFile to the stash as $relocatedFilePath" }
        fileRelocationRegistry[outputFile] = relocatedFilePath
        Files.move(outputFile, relocatedFilePath)
    }

    private fun getNextRelocatedFilePath(): Path = stashDir.resolve("$filesCounter.backup").also { filesCounter++ }

    private fun isFileRelocationIsAlreadyRegisteredFor(outputFile: Path) = outputFile in fileRelocationRegistry

    /**
     * Reverts all the file changes registered in this transaction.
     * If the value for a key is null, then it's the file that was created during the transaction, so the file will be just removed.
     */
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

    /**
     * Deletes the [stashDir].
     */
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

/**
 * A delegating [OutputItemsCollector] implementation that registers compiler output changes in the [transaction]
 */
class TransactionOutputsRegistrar(
    private val transaction: CompilationTransaction,
    private val origin: OutputItemsCollector
) : OutputItemsCollector {
    override fun add(sourceFiles: Collection<File>, outputFile: File) {
        transaction.registerAddedOrChangedFile(outputFile.toPath())
        origin.add(sourceFiles, outputFile)
    }
}