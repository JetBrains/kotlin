/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.TestDataManagerMode
import java.nio.file.Path

/** The task of the `test-data-manager` plugin that runs the tests in [this] mode. */
internal val TestDataManagerMode.taskName: String
    get() = when (this) {
        TestDataManagerMode.CHECK -> "checkTestData"
        TestDataManagerMode.UPDATE -> "updateTestData"
    }

/**
 * The command line of the build that lists the managed modules, [MANAGED_MODULE_TASK]. The extra arguments may change
 * what the build configures, like the projects, so it gets them too.
 */
internal fun Arguments.listingBuild(): List<String> = listOf("-q", MANAGED_MODULE_TASK) + values(Option.GradleArg)

/**
 * The command line of the build of the tests: the options, the tasks of [modules], the runner options and the extra
 * arguments. The runner gets [testDataPaths] and [runId] as resolved, not as given.
 */
internal fun Arguments.testsBuild(modules: List<String>, testDataPaths: List<String>, runId: String): List<String> {
    val runnerOptions = Option.entries.mapNotNull { option ->
        val property = option.runnerProperty ?: return@mapNotNull null
        val value = when (option) {
            Option.Path -> testDataPaths.joinToString(",").ifEmpty { null }
            Option.RunId -> runId
            is ValueOption -> this[option]
            is Flag -> option.runnerValue.takeIf { option in this }
        }

        value?.let { "-P$property=$it" }
    }

    return buildList {
        add("-q")
        add("--continue")
        modules.mapTo(this) { "$it:${mode.taskName}" }
        addAll(runnerOptions)
        addAll(values(Option.GradleArg))
    }
}

/**
 * The line in the log before the output of the build with [arguments]: the command equivalent to it, with `gradlew`
 * of the repository at [root], to repeat it by hand.
 */
internal fun commandLine(root: Path, arguments: List<String>): String =
    "$ " + (listOf(root.resolve("gradlew").toString()) + arguments).joinToString(" ", transform = ::shellQuote)

/** [argument] quoted for a POSIX shell, unless it needs no quotes. */
internal fun shellQuote(argument: String): String =
    if (argument.isNotEmpty() && argument.all { it.isLetterOrDigit() || it in "_-./:=,@%+" }) argument
    else "'" + argument.replace("'", "'\\''") + "'"
