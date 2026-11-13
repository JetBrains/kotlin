/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/**
 * One fully resolved command: what to start, with which arguments and environment, and how to treat its output.
 *
 * Tools build invocations; an [AppleToolRunner] runs them. Callers that start processes themselves, for example a test
 * executor, take [commandLine], [workingDirectory] and [applyEnvironment] from here.
 *
 * [toString] is the command as a shell would take it, with [environment] in front: enough to repeat it in a terminal.
 *
 * @property executable absolute path of the program to start.
 * @property arguments arguments passed to [executable], without shell quoting.
 * @property workingDirectory the directory to start in, or `null` for the current one.
 * @property environment variables to set, applied after [environmentCleanup].
 * @property environmentCleanup variables to remove from the inherited environment.
 * @property output what to do with standard output.
 * @property acceptedExitCodes exit codes that count as success.
 * @property input text written to standard input as UTF-8, or `null` for an empty standard input.
 */
public class AppleToolInvocation(
    public val executable: Path,
    public val arguments: List<String>,
    public val workingDirectory: Path? = null,
    public val environment: Map<String, String> = emptyMap(),
    public val environmentCleanup: EnvironmentCleanup = EnvironmentCleanup.NONE,
    public val output: OutputMode = OutputMode.Capture,
    public val acceptedExitCodes: Set<Int> = setOf(0),
    public val input: String? = null,
) {
    init {
        require(executable.isAbsolute) { "The executable must be an absolute path, got '$executable'" }
        require(acceptedExitCodes.isNotEmpty()) { "At least one exit code must be accepted" }
    }

    /** The executable followed by the arguments. */
    public fun commandLine(): List<String> = listOf(executable.toString()) + arguments

    /**
     * Applies [environmentCleanup] to [inherited], then sets [environment].
     *
     * Accepts any map that can hold strings, so process APIs with `Map<String, Any>` environments work too.
     */
    public fun applyEnvironment(inherited: MutableMap<String, in String>) {
        inherited.keys.removeAll { environmentCleanup.removes(it) }
        inherited.putAll(environment)
    }

    internal fun copy(
        arguments: List<String> = this.arguments,
        output: OutputMode = this.output,
        acceptedExitCodes: Set<Int> = this.acceptedExitCodes,
    ): AppleToolInvocation = AppleToolInvocation(
        executable, arguments, workingDirectory, environment, environmentCleanup, output, acceptedExitCodes, input,
    )

    override fun toString(): String {
        val variables = environment.toSortedMap().map { (name, value) -> "$name=${value.shellQuoted()}" }
        return (variables + commandLine().map { it.shellQuoted() }).joinToString(" ")
    }
}

/**
 * What an [AppleToolRunner] does with standard output.
 *
 * Standard error is always kept in [AppleToolResult.stderr] for error messages; [Forward] passes it on as well.
 */
public sealed interface OutputMode {
    /** Keep standard output in [AppleToolResult.stdout]. */
    public data object Capture : OutputMode

    /** Drop standard output. */
    public data object Discard : OutputMode

    /**
     * Pass each line of standard output to [consumer] and each line of standard error to [errorConsumer] while the
     * process runs. [AppleToolResult.stdout] stays empty.
     *
     * The two consumers are called from different threads. An exception from either stops the process.
     */
    public class Forward(
        public val consumer: (String) -> Unit,
        public val errorConsumer: (String) -> Unit,
    ) : OutputMode {
        /** Passes lines of both streams to [consumer]. */
        public constructor(consumer: (String) -> Unit) : this(consumer, consumer)
    }
}

private val safeShellCharacters = Regex("[A-Za-z0-9_@%+=:,./-]+")

internal fun String.shellQuoted(): String = when {
    isEmpty() -> "''"
    safeShellCharacters.matches(this) -> this
    else -> "'" + replace("'", "'\\''") + "'"
}
