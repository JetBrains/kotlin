/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.*

/**
 * An option of the command line, `--name`.
 *
 * @property runnerProperty the runner option the value is passed to the build as, `-P<runnerProperty>=<value>`; `null` for
 * an option of the wrapper itself
 */
internal sealed class Option(val name: String, val help: String, val runnerProperty: String?) {
    val longName: String get() = "--$name"

    data object Path : ValueOption(
        name = "path",
        valueName = "PATH[,PATH...]",
        help = "testDataPath: test data paths relative to the repository root or absolute, separated by commas as in the " +
                "task option (repeating --path adds more); only the modules with test data in or under them run",
        runnerProperty = TEST_DATA_MANAGER_OPTIONS_TEST_DATA_PATH,
        repeatable = true,
    )

    data object ClassPattern : ValueOption(
        name = "class-pattern",
        valueName = "REGEX",
        help = "testClassPattern: a regex the fully qualified name of a test class must match in full, like " +
                "'.*PsiParsingTestGenerated'",
        runnerProperty = TEST_DATA_MANAGER_OPTIONS_TEST_CLASS_PATTERN,
    )

    data object GoldenOnly : Flag(
        name = "golden-only",
        help = "goldenOnly",
        runnerProperty = TEST_DATA_MANAGER_OPTIONS_GOLDEN_ONLY,
    )

    data object Incremental : Flag(
        name = "incremental",
        help = "incremental",
        runnerProperty = TEST_DATA_MANAGER_OPTIONS_INCREMENTAL,
    )

    data object Module : ValueOption(
        name = "module",
        valueName = "MODULE",
        help = "project path or directory of a module to run (repeatable); default: the modules with managed test data, " +
                "with --path only those with test data in or under the paths",
        repeatable = true,
    )

    data object RunId : ValueOption(
        name = "run-id",
        valueName = "RUN_ID",
        help = "id of the run, naming the module reports and the Gradle log (default: a new UUID)",
        runnerProperty = TEST_DATA_MANAGER_OPTIONS_RUN_ID,
    )

    data object Rerun : ValueOption(
        name = "rerun",
        valueName = "RUN_ID",
        help = "instead of --path, the test data files of the tests that failed in the run RUN_ID, with a problem or a " +
                "test error; this run has its own id",
        runnerProperty = TEST_DATA_MANAGER_OPTIONS_RERUN_ID,
    )

    data object Verbose : Flag(
        name = "verbose",
        help = "verbose output of the modules, in the Gradle log",
        runnerProperty = TEST_DATA_MANAGER_OPTIONS_OUTPUT,
        runnerValue = "verbose",
    )

    data object Json : Flag(
        name = "json",
        help = "print the summary file instead of the text output",
    )

    data object GradleArg : ValueOption(
        name = "gradle-arg",
        valueName = "ARG",
        help = "extra Gradle argument (repeatable), like --gradle-arg --offline",
        repeatable = true,
    )

    companion object {
        /** All options, in the order of the usage. */
        val entries: List<Option> by lazy {
            listOf(Path, ClassPattern, GoldenOnly, Incremental, Module, RunId, Rerun, Verbose, Json, GradleArg)
        }
    }
}

/** An option with a value; the last one counts unless the option is [repeatable]. */
internal sealed class ValueOption(
    name: String,
    val valueName: String,
    help: String,
    runnerProperty: String? = null,
    val repeatable: Boolean = false,
) : Option(name, help, runnerProperty)

/** An option without a value, which passes [runnerValue] to the runner when given. */
internal sealed class Flag(
    name: String,
    help: String,
    runnerProperty: String? = null,
    val runnerValue: String = "true",
) : Option(name, help, runnerProperty)

/** The command line of the wrapper, see [USAGE]. The options are passed to the runner as they are, which validates them. */
internal class Arguments(
    val mode: TestDataManagerMode,
    private val values: Map<ValueOption, List<String>>,
    private val flags: Set<Flag>,
) {
    /** All values of [option], as given. */
    fun values(option: ValueOption): List<String> = values[option].orEmpty()

    /** The value of [option]: the last one, for a repeated option that isn't [ValueOption.repeatable]. */
    operator fun get(option: ValueOption): String? = values[option]?.last()

    operator fun contains(flag: Flag): Boolean = flag in flags
}

/** A command line that can't be run, with the error for the user. */
internal class UsageException(message: String) : Exception(message)

/** The command line asks for the help, [USAGE]. */
internal class HelpRequestedException : Exception()

/**
 * Parses [args]. An option takes its value from the next argument, even if that one starts with `-`, or after `=`
 * (`--gradle-arg=--offline`).
 *
 * @throws UsageException for a command line that can't be run
 * @throws HelpRequestedException for `-h` and `--help`
 */
internal fun parseArguments(args: List<String>): Arguments {
    val options = Option.entries.associateBy { it.longName }
    val values = mutableMapOf<ValueOption, MutableList<String>>()
    val flags = mutableSetOf<Flag>()
    val positional = mutableListOf<String>()

    var index = 0
    while (index < args.size) {
        val argument = args[index++]
        if (argument == "-h" || argument == "--help") throw HelpRequestedException()
        if (!argument.startsWith("--")) {
            positional += argument
            continue
        }

        val inlineValue = argument.substringAfter('=', missingDelimiterValue = "").takeIf { '=' in argument }
        when (val option = options[argument.substringBefore('=')]) {
            is ValueOption -> {
                val value = inlineValue
                    ?: args.getOrNull(index++)
                    ?: throw UsageException("argument ${option.longName}: expected one argument")
                values.getOrPut(option, ::mutableListOf) += value
            }

            is Flag -> {
                if (inlineValue != null) throw UsageException("argument ${option.longName}: takes no value, got '$inlineValue'")
                flags += option
            }

            null -> throw UsageException("unrecognized argument: $argument")
        }
    }

    val modeName = positional.firstOrNull() ?: throw UsageException("the mode is required: check or update")
    val mode = TestDataManagerMode.entries.find { it.name.lowercase() == modeName }
        ?: throw UsageException("invalid mode '$modeName': choose from 'check', 'update'")
    if (positional.size > 1) throw UsageException("unrecognized arguments: ${positional.drop(1).joinToString(" ")}")

    val arguments = Arguments(mode, values, flags)
    for (module in arguments.values(Option.Module)) {
        if (!isModulePath(module)) throw UsageException("argument ${Option.Module.longName}: '$module' is not a module path")
    }

    arguments.checkRunId(Option.Rerun)
    if (arguments[Option.Rerun] != null && arguments.values(Option.Path).isNotEmpty()) {
        throw UsageException(
            "${Option.Rerun.longName} takes the paths of the failed tests, so it can't be combined with ${Option.Path.longName}"
        )
    }

    arguments.checkRunId(Option.RunId)
    return arguments
}

private fun Arguments.checkRunId(option: ValueOption) {
    val problem = this[option]?.let(::runIdError) ?: return
    throw UsageException("argument ${option.longName}: $problem")
}
