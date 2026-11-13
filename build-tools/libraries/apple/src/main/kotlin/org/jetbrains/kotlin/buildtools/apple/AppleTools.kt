/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Apple developer tools bound to one [xcode] installation and one [runner].
 *
 * Every invocation sets `DEVELOPER_DIR` to [XcodeInstallation.developerDir]. Tools from the Xcode toolchain run through
 * `/usr/bin/xcrun`, tools that ship with macOS by absolute path, so `PATH` never decides which binary runs.
 *
 * @param xcode the installation to run tools from, usually from [XcodeLocator.resolve].
 * @param runner starts the processes.
 * @param toolchain a toolchain identifier passed as `TOOLCHAINS`, such as `swift` for a swift.org toolchain, or `null`
 * for the toolchain inside [xcode]. With `null`, an inherited `TOOLCHAINS` variable is removed.
 */
public class AppleTools(
    public val xcode: XcodeInstallation,
    public val runner: AppleToolRunner,
    public val toolchain: String? = null,
) {
    /** `xcodebuild`. */
    public val xcodebuild: Xcodebuild = Xcodebuild(this)

    /** `xcrun` queries. */
    public val xcrun: Xcrun = Xcrun(this)

    /** `lipo`. */
    public val lipo: Lipo = Lipo(this)

    /** `libtool`. */
    public val libtool: Libtool = Libtool(this)

    /** `install_name_tool`. */
    public val installNameTool: InstallNameTool = InstallNameTool(this)

    /** `codesign`. */
    public val codesign: Codesign = Codesign(this)

    /** `PlistBuddy`. */
    public val plistBuddy: PlistBuddy = PlistBuddy(this)

    /** `plutil`. */
    public val plutil: Plutil = Plutil(this)

    /** `simctl`. */
    public val simctl: Simctl = Simctl(this)

    /** `swift package` and `swift build`. */
    public val swiftPackage: SwiftPackage = SwiftPackage(this)

    /** `xattr`. */
    public val xattr: Xattr = Xattr(this)

    private val baseEnvironment: Map<String, String> = buildMap {
        put("DEVELOPER_DIR", xcode.developerDir.toString())
        toolchain?.let { put(TOOLCHAINS, it) }
    }

    // Without an explicit toolchain, an inherited TOOLCHAINS would still redirect xcrun away from [xcode].
    private val inheritedToolchain: EnvironmentCleanup =
        if (toolchain == null) EnvironmentCleanup(names = setOf(TOOLCHAINS)) else EnvironmentCleanup.NONE

    internal fun toolchainInvocation(
        tool: String,
        arguments: List<String>,
        workingDirectory: Path? = null,
        cleanup: EnvironmentCleanup = EnvironmentCleanup.NONE,
        environment: Map<String, String> = emptyMap(),
        input: String? = null,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation = xcrunInvocation(listOf(tool) + arguments, workingDirectory, cleanup, environment, input, output)

    internal fun xcrunInvocation(
        arguments: List<String>,
        workingDirectory: Path? = null,
        cleanup: EnvironmentCleanup = EnvironmentCleanup.NONE,
        environment: Map<String, String> = emptyMap(),
        input: String? = null,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation = AppleToolInvocation(
        executable = XCRUN,
        arguments = arguments,
        workingDirectory = workingDirectory,
        environment = environment + baseEnvironment,
        environmentCleanup = cleanup + inheritedToolchain,
        output = output,
        input = input,
    )

    internal fun systemInvocation(
        executable: Path,
        arguments: List<String>,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation = AppleToolInvocation(
        executable = executable,
        arguments = arguments,
        environment = baseEnvironment,
        output = output,
    )

    internal fun requireXcodeApp(tool: String) {
        if (xcode.kind != XcodeKind.XCODE) {
            throw AppleToolUnavailableException(
                "'$tool' needs Xcode.app, but the selected developer dir is $xcode. " +
                        "Install Xcode and select it with 'sudo xcode-select --switch <path to Xcode.app>' or DEVELOPER_DIR."
            )
        }
    }

    internal companion object {
        val XCRUN: Path = Paths.get("/usr/bin/xcrun")
        const val TOOLCHAINS = "TOOLCHAINS"
    }
}
