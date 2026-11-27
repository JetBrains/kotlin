/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.filters.splitTestDataPaths
import java.nio.file.Path

/**
 * What a run does, as [planRun] decides before the tests are built.
 *
 * @property modules the project paths of the modules whose task runs; none if the run can't start
 * @property skipped the modules left out for having no test data in or under the paths
 * @property testDataPaths the `testDataPath` entries the runner gets
 * @property errors why the run fails whatever the tests do
 */
internal class RunPlan(
    val modules: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
    val testDataPaths: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
)

internal val EMPTY_PATH_ERROR = "${Option.Path.longName} is empty: give a test data file or directory"

/** The error for the `testDataPath` entry [entry] outside the test data of every managed module. */
internal fun unmatchedEntryError(entry: String): String =
    "testDataPath '$entry' is outside the test data of every module with managed test data: its files aren't managed, " +
            "or a module reads them without declaring them in `projectTests { testData(...) }`"

/**
 * Plans a run in the modules given as `--module`, [requestedModules], or else in the managed modules, with the
 * `--path` values [paths] or, for `--rerun`, with the test data paths of the tests that [failed] in that run. The
 * relative paths in the suggestions of the errors are relative to [cwd].
 *
 * The requested modules run as given, with the paths as they are: the runner of each validates them. Otherwise, only
 * the managed modules that may have tests of the paths run ([ManagedModule.mayHaveTestsOf]). A path that doesn't exist
 * fails the whole run, as it would fail every module; a path outside the test data of every module fails the run, but
 * the modules of the other paths still run, without it: their runners would report it once more, as matching none of
 * their tests.
 */
internal fun Repository.planRun(requestedModules: List<String>, paths: List<String>, failed: FailedTests?, cwd: Path): RunPlan {
    // The same module given twice would run once but be reported twice. Sorted as the managed modules, so the same modules
    // make the same build, which reuses its configuration cache entry.
    val requested = requestedModules.map(::requestedProject).distinct().sorted()
    val candidates = requested.ifEmpty { modules.map { it.project } }
    val errors = mutableListOf<String>()
    if (candidates.isEmpty()) errors += "No module applies the test-data-manager plugin in $root"
    if (failed != null) {
        errors += failed.errors
        if (failed.testDataPaths.isEmpty()) return RunPlan(errors = errors)
    }

    val givenPaths = failed?.testDataPaths ?: paths
    if (givenPaths.isEmpty()) return RunPlan(modules = candidates, errors = errors)
    // The errors of the paths of the requested modules come from their runners, each suggesting its own module
    val suggestionBases = if (requested.isEmpty()) listOf(cwd) + modules.map { root.resolve(it.directory) } else listOf(cwd)
    val entries = givenPaths.flatMap(::splitTestDataPaths).map { testDataEntry(it, suggestionBases) }
    if (entries.isEmpty()) return RunPlan(errors = errors + EMPTY_PATH_ERROR)
    if (requested.isNotEmpty()) return RunPlan(modules = requested, testDataPaths = entries.map { it.forRunner }, errors = errors)

    val invalid = entries.filterIsInstance<TestDataEntry.Invalid>()
    if (invalid.isNotEmpty()) return RunPlan(errors = errors + invalid.map { it.error })

    val [matched, unmatched] = entries.filterIsInstance<TestDataEntry.Resolved>().partition { entry ->
        modules.any { it.mayHaveTestsOf(entry.path, entry.directoryOnly) }
    }

    val selected = modules
        .filter { module -> matched.any { module.mayHaveTestsOf(it.path, it.directoryOnly) } }
        .map { it.project }

    return RunPlan(
        modules = selected,
        // Nothing is selected when no path is in the test data of a module: the modules aren't skipped for having no tests
        skipped = if (selected.isEmpty()) emptyList() else candidates - selected.toSet(),
        testDataPaths = matched.map { it.forRunner },
        errors = errors + unmatched.map { unmatchedEntryError(it.forRunner) },
    )
}
