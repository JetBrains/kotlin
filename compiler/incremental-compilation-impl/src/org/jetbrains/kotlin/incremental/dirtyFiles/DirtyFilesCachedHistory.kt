/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.incremental.CompilationTransaction
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.Companion.DIRTY_SOURCES_FILE_NAME
import org.jetbrains.kotlin.incremental.writeText
import java.io.File

/**
 * Used in old JPS implementation
 *
 * Not needed for Gradle? (we fall back to rebuild anyway)
 * Not needed for dep-graph? (it manages successful and unsuccessful compilations on its own)
 * Might be used in maven?
 */
internal class DirtyFilesCachedHistory(workingDir: File) {
    private val dirtySourcesSinceLastTimeFile = File(workingDir, DIRTY_SOURCES_FILE_NAME)

    fun store(withTransaction: CompilationTransaction, allDirtySources: Collection<File>) {
        val text = allDirtySources.joinToString(separator = System.lineSeparator()) { it.normalize().absolutePath }
        withTransaction.writeText(dirtySourcesSinceLastTimeFile.toPath(), text)
    }

    fun read(): List<File> {
        if (dirtySourcesSinceLastTimeFile.exists()) {
            return dirtySourcesSinceLastTimeFile.readLines().map(::File)
        }
        return emptyList()
    }

    fun clear(withTransaction: CompilationTransaction) {
        withTransaction.deleteFile(dirtySourcesSinceLastTimeFile.toPath())
    }
}
