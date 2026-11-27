/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.report.TEST_DATA_MANAGER_JSON
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

/** How many runs [RunDirectory] keeps. */
internal const val KEPT_RUNS = 5

/** A run with a log but no summary may still be running; older than this, it was killed. */
private const val UNFINISHED_RUN_AGE_MILLIS = 24 * 60 * 60 * 1000L

private const val SUMMARY_SUFFIX = ".summary.json"
private const val GRADLE_LOG_SUFFIX = ".gradle.log"

/**
 * The [directory] of the files of the runs, `build/test-data-manager` of the repository: the summary and the Gradle log
 * of each run, named by its id.
 */
internal class RunDirectory(private val directory: Path) {
    fun summaryFile(runId: String): Path = directory.resolve(runId + SUMMARY_SUFFIX)

    fun gradleLog(runId: String): Path = directory.resolve(runId + GRADLE_LOG_SUFFIX)

    /** The summary of the run [runId]; `null` if there is none to read. */
    fun readSummary(runId: String): Summary? = try {
        TEST_DATA_MANAGER_JSON.decodeFromString<Summary>(summaryFile(runId).readText())
    } catch (_: Exception) {
        null
    }

    /**
     * Writes the summary of the run [runId], the JSON [text], so that the file exists only complete: whoever waits for it
     * never reads a part.
     */
    fun writeSummary(runId: String, text: String) {
        val file = summaryFile(runId)
        val temporary = file.resolveSibling(file.name + ".tmp")
        temporary.writeText(text)
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /**
     * Keeps the files of the [KEPT_RUNS] most recent runs, and of [keptRun]. A run with only a log is still running, or
     * was killed before its summary: it counts only when older than [UNFINISHED_RUN_AGE_MILLIS].
     */
    fun deleteOldRuns(keptRun: String?, now: Long = System.currentTimeMillis()) {
        val runs = mutableMapOf<String, Long>()
        for (summary in directory.listDirectoryEntries("*$SUMMARY_SUFFIX")) {
            runs[summary.name.removeSuffix(SUMMARY_SUFFIX)] = summary.getLastModifiedTime().toMillis()
        }

        for (log in directory.listDirectoryEntries("*$GRADLE_LOG_SUFFIX")) {
            val runId = log.name.removeSuffix(GRADLE_LOG_SUFFIX)
            val modified = log.getLastModifiedTime().toMillis()
            if (runId in runs || modified < now - UNFINISHED_RUN_AGE_MILLIS) {
                runs[runId] = maxOf(runs[runId] ?: 0L, modified)
            }
        }

        for (runId in runs.keys.sortedByDescending { runs.getValue(it) }.drop(KEPT_RUNS)) {
            if (runId == keptRun) continue
            summaryFile(runId).deleteIfExists()
            gradleLog(runId).deleteIfExists()
        }
    }
}
