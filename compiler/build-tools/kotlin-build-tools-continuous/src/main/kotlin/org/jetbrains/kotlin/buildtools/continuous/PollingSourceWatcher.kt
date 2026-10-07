/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.continuous

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.fileSize

/**
 * Detects changes of source files by periodically comparing the modification time and the size of all the files under [roots].
 */
internal class PollingSourceWatcher(
    private val roots: List<Path>,
    private val extensions: Set<String>,
    private val pollIntervalMillis: Long,
    private val onChanges: (modified: List<Path>, removed: List<Path>) -> Unit,
) : AutoCloseable {
    private data class FileState(val lastModified: Long, val size: Long)

    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Kotlin continuous compilation file watcher").apply { isDaemon = true }
    }

    private var knownFiles: Map<Path, FileState> = emptyMap()

    fun start() {
        knownFiles = scan()
        executor.scheduleWithFixedDelay(::poll, pollIntervalMillis, pollIntervalMillis, TimeUnit.MILLISECONDS)
    }

    private fun poll() {
        val currentFiles = try {
            scan()
        } catch (_: Exception) {
            // Files might be changed during the scan, the next poll will pick up the changes
            return
        }
        val modified = currentFiles.filter { (path, state) -> knownFiles[path] != state }.keys.toList()
        val removed = knownFiles.keys.filter { it !in currentFiles }
        knownFiles = currentFiles
        if (modified.isNotEmpty() || removed.isNotEmpty()) {
            onChanges(modified, removed)
        }
    }

    private fun scan(): Map<Path, FileState> {
        val result = LinkedHashMap<Path, FileState>()
        for (root in roots) {
            if (!root.isDirectory()) continue
            Files.walk(root).use { paths ->
                paths.filter { it.isRegularFile() && it.extension in extensions }.forEach { path ->
                    result[path.toAbsolutePath().normalize()] = FileState(path.getLastModifiedTime().toMillis(), path.fileSize())
                }
            }
        }
        return result
    }

    override fun close() {
        executor.shutdownNow()
    }
}
