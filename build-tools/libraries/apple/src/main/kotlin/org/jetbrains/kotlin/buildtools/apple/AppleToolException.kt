/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

/**
 * A tool finished with an exit code its invocation does not accept.
 *
 * The message has the command with the variables it sets, the exit code, a [hint] when the tool recognised the failure,
 * and the last lines of standard error, or of standard output when standard error is empty.
 */
public class AppleToolException(
    /** The failed invocation. */
    public val invocation: AppleToolInvocation,
    /** Exit code and output of the failed process. */
    public val result: AppleToolResult,
    /** What probably went wrong and how to fix it, if the tool recognised the failure. */
    public val hint: String? = null,
) : RuntimeException(failureMessage(invocation, result, hint))

/** A tool cannot run at all: its executable is missing, or it needs Xcode.app and only Command Line Tools are selected. */
public class AppleToolUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** No Xcode matches the request, or the requested path is not an Xcode installation. */
public class XcodeNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

private const val OUTPUT_TAIL_LINES = 50

private fun failureMessage(invocation: AppleToolInvocation, result: AppleToolResult, hint: String?): String = buildString {
    append("Command failed with exit code ${result.exitCode}: $invocation")
    if (hint != null) append("\n").append(hint)
    val output = result.stderr.ifBlank { result.stdout }.trimEnd()
    if (output.isNotEmpty()) {
        val lines = output.lines()
        append("\n")
        if (lines.size > OUTPUT_TAIL_LINES) append("[${lines.size - OUTPUT_TAIL_LINES} earlier lines omitted]\n")
        append(lines.takeLast(OUTPUT_TAIL_LINES).joinToString("\n"))
    }
}
