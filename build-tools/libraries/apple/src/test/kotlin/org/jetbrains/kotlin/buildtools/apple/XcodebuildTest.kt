/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class XcodebuildTest {
    private var response = result()
    private val runner = RecordingAppleToolRunner { response }
    private val xcodebuild = AppleTools(fakeXcode, runner).xcodebuild

    @Test
    fun `createXCFramework adds frameworks with debug symbols and libraries with headers`() {
        val invocation = xcodebuild.createXCFrameworkInvocation(
            listOf(
                XCFrameworkSlice.Framework(path("/out/ios arm64/Shared.framework"), listOf(path("/out/ios arm64/Shared.framework.dSYM"))),
                XCFrameworkSlice.Framework(path("/out/sim/Shared.framework")),
                XCFrameworkSlice.Library(
                    path("/out/mac/libShared.a"), headers = path("/out/mac/Headers"),
                    debugSymbols = listOf(path("/out/mac/a.dSYM"), path("/out/mac/b.bcsymbolmap")),
                ),
            ),
            path("/out/Shared ü.xcframework"),
        )

        assertEquals(
            listOf(
                XCRUN, "xcodebuild", "-create-xcframework",
                "-framework", "/out/ios arm64/Shared.framework", "-debug-symbols", "/out/ios arm64/Shared.framework.dSYM",
                "-framework", "/out/sim/Shared.framework",
                "-library", "/out/mac/libShared.a", "-headers", "/out/mac/Headers",
                "-debug-symbols", "/out/mac/a.dSYM", "-debug-symbols", "/out/mac/b.bcsymbolmap",
                "-output", "/out/Shared ü.xcframework",
            ),
            invocation.commandLine(),
        )
    }

    @Test
    fun `createXCFramework makes debug symbol paths absolute`() {
        val invocation = xcodebuild.createXCFrameworkInvocation(
            listOf(XCFrameworkSlice.Framework(path("Shared.framework"), listOf(path("Shared.framework.dSYM")))),
            path("Shared.xcframework"),
        )

        assertEquals(
            listOf(
                XCRUN, "xcodebuild", "-create-xcframework", "-framework", "Shared.framework",
                "-debug-symbols", path("Shared.framework.dSYM").toAbsolutePath().toString(), "-output", "Shared.xcframework",
            ),
            invocation.commandLine(),
        )
    }

    @Test
    fun `createXCFramework needs a slice`() {
        val exception = assertFailsWith<IllegalArgumentException> { xcodebuild.createXCFramework(emptyList(), path("/out.xcframework")) }
        assertEquals("An XCFramework needs at least one slice", exception.message)
    }

    @Test
    fun `build passes options in a fixed order and cleans the Xcode environment`() {
        val invocation = xcodebuild.buildInvocation(
            workingDirectory = path("/pkg dir"),
            scheme = "Synthetic",
            destination = ApplePlatform.IOS_SIMULATOR.genericDestination,
            actions = listOf("build"),
            project = path("Pods/Pods.xcodeproj"),
            configuration = "Release",
            derivedDataPath = path("dd"),
            buildSettings = linkedMapOf("ARCHS" to "arm64 x86_64", "CODE_SIGN_IDENTITY" to ""),
            additionalArguments = listOf("-clonedSourcePackagesDirPath", "/checkouts"),
            quiet = true,
        )

        assertEquals(
            listOf(
                XCRUN, "xcodebuild", "build",
                "-project", "Pods/Pods.xcodeproj",
                "-scheme", "Synthetic",
                "-destination", "generic/platform=iOS Simulator",
                "-configuration", "Release",
                "-derivedDataPath", "dd",
                "-quiet",
                "ARCHS=arm64 x86_64", "CODE_SIGN_IDENTITY=",
                "-clonedSourcePackagesDirPath", "/checkouts",
            ),
            invocation.commandLine(),
        )
        assertEquals(path("/pkg dir"), invocation.workingDirectory)
        assertEquals(
            EnvironmentCleanup(
                names = setOf(
                    "EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES", "ENABLE_DEBUG_DYLIB", "EXECUTABLE_BLANK_INJECTION_DYLIB_PATH",
                    "EXECUTABLE_DEBUG_DYLIB_INSTALL_NAME", "EXECUTABLE_DEBUG_DYLIB_PATH", "TOOLCHAINS",
                ),
                prefixes = setOf("OTHER_", "ASSETCATALOG_"),
            ),
            invocation.environmentCleanup,
        )
    }

    @Test
    fun `build takes its own cleanup and extra environment`() {
        val invocation = xcodebuild.buildInvocation(
            path("/pkg"), "Lib", "platform=macOS",
            environment = mapOf("KOTLIN_CLANG_ARGS_DUMP_FILE" to "/dump/clang args", "DEVELOPER_DIR" to "/ignored"),
            cleanup = EnvironmentCleanup.DEBUG_DYLIB,
        )

        assertEquals(
            mapOf("KOTLIN_CLANG_ARGS_DUMP_FILE" to "/dump/clang args", "DEVELOPER_DIR" to "/Applications/Xcode 16.app/Contents/Developer"),
            invocation.environment,
        )
        assertEquals(EnvironmentCleanup.DEBUG_DYLIB + EnvironmentCleanup(names = setOf("TOOLCHAINS")), invocation.environmentCleanup)
    }

    @Test
    fun `only build removes variables by default`() {
        val others = listOf(
            xcodebuild.showBuildSettingsInvocation(path("/Pods")),
            xcodebuild.versionInvocation(),
            xcodebuild.createXCFrameworkInvocation(listOf(XCFrameworkSlice.Library(path("/lib.a"))), path("/out.xcframework")),
        )

        others.forEach {
            assertEquals(EnvironmentCleanup(names = setOf("TOOLCHAINS")), it.environmentCleanup, it.toString())
        }
        assertEquals(
            EnvironmentCleanup.APP_BUILD_SETTINGS + EnvironmentCleanup(names = setOf("TOOLCHAINS")),
            xcodebuild.showBuildSettingsInvocation(path("/Pods"), cleanup = EnvironmentCleanup.APP_BUILD_SETTINGS).environmentCleanup,
        )
    }

    @Test
    fun `build with defaults has only scheme and destination`() {
        val invocation = xcodebuild.buildInvocation(path("/pkg"), "Lib", "platform=macOS")

        assertEquals(listOf(XCRUN, "xcodebuild", "-scheme", "Lib", "-destination", "platform=macOS"), invocation.commandLine())
    }

    @Test
    fun `build forwards output when asked`() {
        val forward = OutputMode.Forward { }

        xcodebuild.build(path("/pkg"), "Lib", "platform=macOS", output = forward)

        assertSame(forward, runner.single.output)
    }

    @Test
    fun `build failure for a missing platform carries a hint`() {
        response = result(stderr = "xcodebuild: error: iOS 18.0 is not installed. To use with Xcode, first download and install the platform", exitCode = 70)

        val exception = assertFailsWith<AppleToolException> { xcodebuild.build(path("/pkg"), "Lib", "generic/platform=iOS") }

        assertEquals(
            "The platform for destination 'generic/platform=iOS' is not installed. Install it in Xcode > Settings > Components, " +
                    "or with 'xcodebuild -downloadPlatform <platform>'.",
            exception.hint,
        )
    }

    @Test
    fun `other build failures have no hint`() {
        response = result(stderr = "error: compile failed", exitCode = 65)

        val exception = assertFailsWith<AppleToolException> { xcodebuild.build(path("/pkg"), "Lib", "generic/platform=iOS") }

        assertNull(exception.hint)
    }

    @Test
    fun `showBuildSettings passes project, scheme, sdk and configuration`() {
        response = result(stdout = "Build settings for action build and target Pod:\n    ARCHS = arm64\n")

        val output = xcodebuild.showBuildSettings(path("/Pods dir"), path("Pods.xcodeproj"), "Pod", ApplePlatform.IOS, "Debug")

        assertEquals("Build settings for action build and target Pod:\n    ARCHS = arm64\n", output)
        assertEquals(
            listOf(XCRUN, "xcodebuild", "-showBuildSettings", "-project", "Pods.xcodeproj", "-scheme", "Pod", "-sdk", "iphoneos", "-configuration", "Debug"),
            runner.single.commandLine(),
        )
        assertEquals(path("/Pods dir"), runner.single.workingDirectory)
    }

    @Test
    fun `version parses the first line`() {
        response = result(stdout = "Xcode 16.4\nBuild version 16F6\n")
        assertEquals(XcodeVersion(16, 4), xcodebuild.version())
        assertEquals(listOf(XCRUN, "xcodebuild", "-version"), runner.single.commandLine())
    }

    @Test
    fun `version is null for unexpected output`() {
        response = result(stdout = "something else\n")
        assertNull(xcodebuild.version())
    }
}
