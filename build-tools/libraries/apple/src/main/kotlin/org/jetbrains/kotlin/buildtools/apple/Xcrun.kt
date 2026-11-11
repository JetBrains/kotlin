/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path
import java.nio.file.Paths

/** `xcrun`: lookups of tool and SDK paths inside the selected installation, and tools without a wrapper of their own. */
public class Xcrun internal constructor(private val tools: AppleTools) {

    /** `xcrun <tool> <arguments>`, for toolchain tools this library has no wrapper for, such as `xcprojformatter`. */
    public fun toolInvocation(
        tool: String,
        arguments: List<String> = emptyList(),
        workingDirectory: Path? = null,
        input: String? = null,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation {
        require(tool.isNotBlank()) { "The tool name must not be blank" }
        return tools.toolchainInvocation(tool, arguments, workingDirectory, input = input, output = output)
    }

    /** Runs [toolInvocation]. */
    public fun runTool(
        tool: String,
        arguments: List<String> = emptyList(),
        workingDirectory: Path? = null,
        input: String? = null,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolResult = tools.runner.runChecked(toolInvocation(tool, arguments, workingDirectory, input, output))

    /** `xcrun --find <tool>`. */
    public fun findInvocation(tool: String): AppleToolInvocation {
        require(tool.isNotBlank()) { "The tool name must not be blank" }
        return tools.xcrunInvocation(listOf("--find", tool))
    }

    /** The absolute path of [tool], for callers that must pass a path, such as `ALTERNATE_LINKER=`. */
    public fun find(tool: String): Path = tools.runner.runForPath(findInvocation(tool))

    /** `xcrun --sdk <sdk> --show-sdk-path`. */
    public fun showSdkPathInvocation(platform: ApplePlatform): AppleToolInvocation =
        tools.xcrunInvocation(listOf("--sdk", platform.sdkName, "--show-sdk-path"))

    /** The SDK directory for [platform]. */
    public fun showSdkPath(platform: ApplePlatform): Path = tools.runner.runForPath(showSdkPathInvocation(platform))
}

/** Runs [invocation] and returns the path on the first non-blank line of its output. */
internal fun AppleToolRunner.runForPath(invocation: AppleToolInvocation): Path {
    val line = runChecked(invocation).stdout.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        ?: throw IllegalStateException("Expected a path, but there was no output from: $invocation")
    return Paths.get(line)
}
