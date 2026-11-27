/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

/** A failure with its causes, as a Gradle failure or an exception has them. */
internal class FailureNode(val message: String?, val causes: List<FailureNode>)

/** The most lines of each Gradle failure in the summary; the rest is in the Gradle log. */
internal const val MAX_FAILURE_LINES = 20

/** The location of a script as Gradle puts it before a failure: `Build file '/repo/build.gradle.kts' line: 12`. */
private val SCRIPT_LOCATION = Regex("""(?:Build file|Settings file|Initialization script|Script) '.+'(?: line: \d+)?""")

private val LINE_BREAK = Regex("""\r?\n""")

/**
 * [failure] as the console shows it: its message and, below it, the messages of its causes, each behind `> ` indented by
 * its depth. A cause whose message is already part of the message above it is left out: Gradle wraps an unknown task in
 * a failure with the same message, and the Tooling API puts the messages of the causes into its own. Only the first
 * [MAX_FAILURE_LINES] lines are kept.
 */
internal fun renderFailure(failure: FailureNode): String {
    val lines = mutableListOf<String>()
    lines += failure.message?.lines() ?: listOf("Unknown failure")

    fun addCauses(node: FailureNode, depth: Int, shown: String?) {
        for (cause in node.causes) {
            val message = cause.message
            if (message == null || shown?.contains(message) == true) {
                addCauses(cause, depth, shown)
                continue
            }

            val prefix = "   ".repeat(depth) + "> "
            for ([index, line] in message.lines().withIndex()) {
                lines += (if (index == 0) prefix else " ".repeat(prefix.length)) + line
            }

            addCauses(cause, depth + 1, message)
        }
    }

    addCauses(failure, depth = 0, failure.message)
    if (lines.size <= MAX_FAILURE_LINES) return lines.joinToString("\n")
    return (lines.take(MAX_FAILURE_LINES) + "... ${lines.size - MAX_FAILURE_LINES} more lines").joinToString("\n")
}

/**
 * The failures of a build: those of the tasks ([taskFailures], by task path), then those of the build ([buildFailures])
 * that aren't failures of a task, and the [launchFailure], if the launch or the connection failed besides.
 *
 * With several failures, Gradle reports one failure of the build, `Build completed with N failures`, whose causes are
 * the failures themselves: such a failure is replaced by those of its causes that aren't failures of a task. A failure
 * of a task as a failure of the build may have the location of the failing script before its message (`Build file '...'
 * line: 12`); then it is shown instead of the failure of the task, for the location.
 */
internal fun collectFailures(
    taskFailures: List<Pair<String, FailureNode>>,
    buildFailures: List<FailureNode>,
    launchFailure: FailureNode?,
): List<GradleFailure> {
    val taskMessages = taskFailures.mapNotNullTo(mutableSetOf()) { it.second.message }
    // The failures of tasks with a location, by the message of the task failure
    val located = mutableMapOf<String, FailureNode>()

    /** The message of the task failure [failure] is, with or without a location, or `null`. */
    fun taskMessage(failure: FailureNode): String? {
        val message = failure.message ?: return null
        if (message in taskMessages) return message
        // The location ends with the line separator of the platform
        val [location, rest] = message.split(LINE_BREAK, limit = 2).takeIf { it.size == 2 } ?: return null
        return rest.takeIf { it in taskMessages && SCRIPT_LOCATION.matches(location) }?.also { located[it] = failure }
    }

    fun ownFailures(failure: FailureNode): List<FailureNode> = when {
        taskMessage(failure) != null -> emptyList()
        failure.causes.any { taskMessage(it) != null } -> failure.causes.flatMap(::ownFailures)
        else -> listOf(failure)
    }

    val buildLevel = buildFailures.flatMap(::ownFailures) + listOfNotNull(launchFailure)
    val ofTasks = taskFailures.map { [taskPath, failure] -> GradleFailure(taskPath, renderFailure(located[failure.message] ?: failure)) }
    return ofTasks + buildLevel.map { GradleFailure(taskPath = null, renderFailure(it)) }
}
