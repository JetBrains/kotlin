/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.report.ModuleReport
import org.jetbrains.kotlin.analysis.test.data.manager.report.TEST_DATA_MANAGER_JSON
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.readText

/** The module report in [file]. */
internal class LoadedReport(val file: String, val content: ModuleReport)

/** The entries of the module reports, in the order of the modules. */
internal class MergedEntries(
    val problems: List<ModuleReport.Problem>,
    val testErrors: List<ModuleReport.TestError>,
    val recoveredTests: List<ModuleReport.RecoveredTest>,
    val fileActions: List<ModuleReport.FileAction>,
)

private val REPORT_LINE = Regex("""^${Regex.escape(ModuleReport.PATH_PREFIX)}(.+\.json)$""", RegexOption.MULTILINE)

/** The module report in [file]; `null` if it can't be read. */
internal fun readReport(file: String): ModuleReport? = try {
    TEST_DATA_MANAGER_JSON.decodeFromString<ModuleReport>(Path(file).readText())
} catch (_: IOException) {
    null
} catch (_: IllegalArgumentException) {
    // Not JSON, or not a report
    null
}

/** The reports of the run [runId] printed in [gradleOutput], by project; an unreadable one counts as missing. */
internal fun loadReports(gradleOutput: CharSequence, runId: String): Map<String, LoadedReport> {
    val reports = mutableMapOf<String, LoadedReport>()
    for (match in REPORT_LINE.findAll(gradleOutput)) {
        val path = match.groupValues[1]
        val report = readReport(path) ?: continue
        val project = report.project ?: continue
        if (report.runId == runId) reports[project] = LoadedReport(path, report)
    }

    return reports
}

/** The entries of the [reports] of [modules], in their order. */
internal fun merge(modules: List<String>, reports: Map<String, LoadedReport>): MergedEntries {
    val contents = modules.mapNotNull { reports[it]?.content }
    return MergedEntries(
        problems = contents.flatMap { it.problems },
        testErrors = contents.flatMap { it.testErrors },
        recoveredTests = contents.flatMap { it.recoveredTests },
        fileActions = contents.flatMap { it.fileActions },
    )
}
