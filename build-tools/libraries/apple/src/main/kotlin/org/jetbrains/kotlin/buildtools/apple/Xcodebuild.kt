/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/** `xcodebuild`. Every operation needs Xcode.app and throws [AppleToolUnavailableException] for Command Line Tools. */
public class Xcodebuild internal constructor(private val tools: AppleTools) {

    /** `xcodebuild -create-xcframework` with one `-framework` or `-library` per slice, each followed by its debug symbols. */
    public fun createXCFrameworkInvocation(slices: List<XCFrameworkSlice>, output: Path): AppleToolInvocation {
        require(slices.isNotEmpty()) { "An XCFramework needs at least one slice" }
        val arguments = buildList {
            add("-create-xcframework")
            slices.forEach { slice ->
                when (slice) {
                    is XCFrameworkSlice.Framework -> {
                        add("-framework"); add(slice.path.toString())
                    }
                    is XCFrameworkSlice.Library -> {
                        add("-library"); add(slice.path.toString())
                        slice.headers?.let { add("-headers"); add(it.toString()) }
                    }
                }
                // xcodebuild rejects a relative path here.
                slice.debugSymbols.forEach { add("-debug-symbols"); add(it.toAbsolutePath().toString()) }
            }
            add("-output"); add(output.toString())
        }
        return invocation(arguments)
    }

    /** Runs [createXCFrameworkInvocation]. `xcodebuild` fails when [output] already exists. */
    public fun createXCFramework(slices: List<XCFrameworkSlice>, output: Path) {
        tools.runner.runChecked(createXCFrameworkInvocation(slices, output))
    }

    /**
     * `xcodebuild [actions] -scheme <scheme> -destination <destination> ...` in [workingDirectory].
     *
     * @param actions build actions such as `build`; empty runs the default action.
     * @param project the `.xcodeproj` to use, or `null` to let `xcodebuild` pick the project or package in [workingDirectory].
     * @param buildSettings appended as `KEY=VALUE` arguments.
     * @param additionalArguments appended after everything else.
     * @param quiet adds `-quiet`, so only warnings and errors are printed.
     * @param environment variables to set for the build, for example for compiler wrapper scripts.
     * @param cleanup inherited variables to remove. The default drops the settings of an app whose build phase started
     * this build.
     */
    public fun buildInvocation(
        workingDirectory: Path,
        scheme: String,
        destination: String,
        actions: List<String> = emptyList(),
        project: Path? = null,
        configuration: String? = null,
        derivedDataPath: Path? = null,
        buildSettings: Map<String, String> = emptyMap(),
        additionalArguments: List<String> = emptyList(),
        quiet: Boolean = false,
        environment: Map<String, String> = emptyMap(),
        cleanup: EnvironmentCleanup = EnvironmentCleanup.APP_BUILD_SETTINGS,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation {
        val arguments = buildList {
            addAll(actions)
            project?.let { add("-project"); add(it.toString()) }
            add("-scheme"); add(scheme)
            add("-destination"); add(destination)
            configuration?.let { add("-configuration"); add(it) }
            derivedDataPath?.let { add("-derivedDataPath"); add(it.toString()) }
            if (quiet) add("-quiet")
            buildSettings.forEach { (key, value) -> add("$key=$value") }
            addAll(additionalArguments)
        }
        return invocation(arguments, workingDirectory, cleanup, environment, output)
    }

    /**
     * Runs [buildInvocation]. When the destination platform is not installed, the [AppleToolException] says how to
     * install it.
     */
    public fun build(
        workingDirectory: Path,
        scheme: String,
        destination: String,
        actions: List<String> = emptyList(),
        project: Path? = null,
        configuration: String? = null,
        derivedDataPath: Path? = null,
        buildSettings: Map<String, String> = emptyMap(),
        additionalArguments: List<String> = emptyList(),
        quiet: Boolean = false,
        environment: Map<String, String> = emptyMap(),
        cleanup: EnvironmentCleanup = EnvironmentCleanup.APP_BUILD_SETTINGS,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolResult {
        val invocation = buildInvocation(
            workingDirectory, scheme, destination, actions, project, configuration, derivedDataPath, buildSettings,
            additionalArguments, quiet, environment, cleanup, output,
        )
        return tools.runner.runChecked(invocation) { result -> missingPlatformHint(result, destination) }
    }

    /** `xcodebuild -showBuildSettings` in [workingDirectory]. [cleanup] names inherited variables to remove. */
    public fun showBuildSettingsInvocation(
        workingDirectory: Path,
        project: Path? = null,
        scheme: String? = null,
        sdk: ApplePlatform? = null,
        configuration: String? = null,
        cleanup: EnvironmentCleanup = EnvironmentCleanup.NONE,
    ): AppleToolInvocation {
        val arguments = buildList {
            add("-showBuildSettings")
            project?.let { add("-project"); add(it.toString()) }
            scheme?.let { add("-scheme"); add(it) }
            sdk?.let { add("-sdk"); add(it.sdkName) }
            configuration?.let { add("-configuration"); add(it) }
        }
        return invocation(arguments, workingDirectory, cleanup)
    }

    /** Runs [showBuildSettingsInvocation] and returns its output. */
    public fun showBuildSettings(
        workingDirectory: Path,
        project: Path? = null,
        scheme: String? = null,
        sdk: ApplePlatform? = null,
        configuration: String? = null,
        cleanup: EnvironmentCleanup = EnvironmentCleanup.NONE,
    ): String =
        tools.runner.runChecked(showBuildSettingsInvocation(workingDirectory, project, scheme, sdk, configuration, cleanup)).stdout

    /** `xcodebuild -version`. */
    public fun versionInvocation(): AppleToolInvocation = invocation(listOf("-version"))

    /** The version `xcodebuild -version` prints, or `null` when the output cannot be parsed. */
    public fun version(): XcodeVersion? {
        val firstLine = tools.runner.runChecked(versionInvocation()).stdout.lineSequence().firstOrNull() ?: return null
        return XcodeVersion.parse(firstLine.trim().removePrefix("Xcode").trim())
    }

    private fun invocation(
        arguments: List<String>,
        workingDirectory: Path? = null,
        cleanup: EnvironmentCleanup = EnvironmentCleanup.NONE,
        environment: Map<String, String> = emptyMap(),
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation {
        tools.requireXcodeApp(TOOL)
        return tools.toolchainInvocation(TOOL, arguments, workingDirectory, cleanup, environment, output = output)
    }

    private fun missingPlatformHint(result: AppleToolResult, destination: String): String? {
        val output = result.stderr + "\n" + result.stdout
        if (!(output.contains("is not installed") && output.contains("platform", ignoreCase = true))) return null
        return "The platform for destination '$destination' is not installed. Install it in Xcode > Settings > Components, " +
                "or with 'xcodebuild -downloadPlatform <platform>'."
    }

    private companion object {
        const val TOOL = "xcodebuild"
    }
}

/** One slice of an XCFramework passed to [Xcodebuild.createXCFramework]. */
public sealed interface XCFrameworkSlice {
    /** The framework or library to add. */
    public val path: Path

    /** `.dSYM` bundles or `.bcsymbolmap` files to copy next to the binary. */
    public val debugSymbols: List<Path>

    /** A `.framework` bundle. */
    public data class Framework(
        override val path: Path,
        override val debugSymbols: List<Path> = emptyList(),
    ) : XCFrameworkSlice

    /** A static or dynamic library, with its public headers when [headers] is set. */
    public data class Library(
        override val path: Path,
        public val headers: Path? = null,
        override val debugSymbols: List<Path> = emptyList(),
    ) : XCFrameworkSlice
}
