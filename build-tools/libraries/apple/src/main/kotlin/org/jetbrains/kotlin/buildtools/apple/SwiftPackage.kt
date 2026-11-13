/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/**
 * `swift package` and `swift build` for the Swift package in a directory.
 *
 * `swift` runs through `xcrun`, so a swiftly or Homebrew `swift` on `PATH` is not used; pass a toolchain to
 * [AppleTools] to choose one on purpose.
 *
 * Every operation takes a `cleanup` of inherited variables to remove. The default is [EnvironmentCleanup.SDKROOT],
 * which would otherwise point SwiftPM at the SDK of the Xcode target that started the build.
 */
public class SwiftPackage internal constructor(private val tools: AppleTools) {

    /** `swift package [--scratch-path <scratchPath>] resolve <additionalArguments>` in [packageDirectory]. */
    public fun resolveInvocation(
        packageDirectory: Path,
        scratchPath: Path? = null,
        additionalArguments: List<String> = emptyList(),
        cleanup: EnvironmentCleanup = EnvironmentCleanup.SDKROOT,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation = invocation(
        packageDirectory,
        buildList {
            add("package")
            scratchPath?.let { add("--scratch-path"); add(it.toString()) }
            add("resolve")
            addAll(additionalArguments)
        },
        cleanup,
        output,
    )

    /** Runs [resolveInvocation]. The result has the warnings SwiftPM printed to standard error. */
    public fun resolve(
        packageDirectory: Path,
        scratchPath: Path? = null,
        additionalArguments: List<String> = emptyList(),
        cleanup: EnvironmentCleanup = EnvironmentCleanup.SDKROOT,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolResult =
        tools.runner.runChecked(resolveInvocation(packageDirectory, scratchPath, additionalArguments, cleanup, output))

    /** `swift package describe --type json` in [packageDirectory]. */
    public fun describeInvocation(
        packageDirectory: Path,
        cleanup: EnvironmentCleanup = EnvironmentCleanup.SDKROOT,
    ): AppleToolInvocation = invocation(packageDirectory, listOf("package", "describe", "--type", "json"), cleanup)

    /** The package description as JSON. */
    public fun describe(packageDirectory: Path, cleanup: EnvironmentCleanup = EnvironmentCleanup.SDKROOT): String =
        tools.runner.runChecked(describeInvocation(packageDirectory, cleanup)).stdout

    /** `swift build <additionalArguments>` in [packageDirectory]. */
    public fun buildInvocation(
        packageDirectory: Path,
        additionalArguments: List<String> = emptyList(),
        cleanup: EnvironmentCleanup = EnvironmentCleanup.SDKROOT,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation = invocation(packageDirectory, listOf("build") + additionalArguments, cleanup, output)

    /** Runs [buildInvocation]. The result has the warnings SwiftPM printed to standard error. */
    public fun build(
        packageDirectory: Path,
        additionalArguments: List<String> = emptyList(),
        cleanup: EnvironmentCleanup = EnvironmentCleanup.SDKROOT,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolResult = tools.runner.runChecked(buildInvocation(packageDirectory, additionalArguments, cleanup, output))

    /** `swift build --show-bin-path <additionalArguments>` in [packageDirectory]. */
    public fun showBinPathInvocation(
        packageDirectory: Path,
        additionalArguments: List<String> = emptyList(),
        cleanup: EnvironmentCleanup = EnvironmentCleanup.SDKROOT,
    ): AppleToolInvocation = invocation(packageDirectory, listOf("build", "--show-bin-path") + additionalArguments, cleanup)

    /** The directory `swift build` writes products to. */
    public fun showBinPath(
        packageDirectory: Path,
        additionalArguments: List<String> = emptyList(),
        cleanup: EnvironmentCleanup = EnvironmentCleanup.SDKROOT,
    ): Path = tools.runner.runForPath(showBinPathInvocation(packageDirectory, additionalArguments, cleanup))

    private fun invocation(
        packageDirectory: Path,
        arguments: List<String>,
        cleanup: EnvironmentCleanup,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation = tools.toolchainInvocation("swift", arguments, packageDirectory, cleanup, output = output)
}
