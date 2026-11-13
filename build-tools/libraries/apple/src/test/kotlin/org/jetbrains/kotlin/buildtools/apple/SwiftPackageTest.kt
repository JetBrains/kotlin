/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SwiftPackageTest {
    private var response = result()
    private val runner = RecordingAppleToolRunner { response }
    private val swiftPackage = AppleTools(fakeXcode, runner).swiftPackage
    private val packageDirectory = path("/project/Synthetic Package")

    @Test
    fun `resolve runs in the package directory without SDKROOT`() {
        swiftPackage.resolve(packageDirectory, scratchPath = path("/project/.build checkouts"), additionalArguments = listOf("--disable-scm-to-registry-transformation"))

        val invocation = runner.single
        assertEquals(
            listOf(XCRUN, "swift", "package", "--scratch-path", "/project/.build checkouts", "resolve", "--disable-scm-to-registry-transformation"),
            invocation.commandLine(),
        )
        assertEquals(packageDirectory, invocation.workingDirectory)
        assertEquals(EnvironmentCleanup(names = setOf("SDKROOT", "TOOLCHAINS")), invocation.environmentCleanup)
    }

    @Test
    fun `every operation removes SDKROOT by default and takes another cleanup`() {
        val defaults = listOf(
            swiftPackage.resolveInvocation(packageDirectory),
            swiftPackage.describeInvocation(packageDirectory),
            swiftPackage.buildInvocation(packageDirectory),
            swiftPackage.showBinPathInvocation(packageDirectory),
        )
        defaults.forEach { assertEquals(EnvironmentCleanup(names = setOf("SDKROOT", "TOOLCHAINS")), it.environmentCleanup, it.toString()) }

        val describe = swiftPackage.describeInvocation(packageDirectory, EnvironmentCleanup.SDK_VARIABLES)
        assertEquals(EnvironmentCleanup(names = setOf("TOOLCHAINS"), prefixes = setOf("SDK")), describe.environmentCleanup)
    }

    @Test
    fun `resolve and build return the result with stderr`() {
        response = result(stderr = "warning: dependency 'x' is not used by any target\n")

        assertEquals("warning: dependency 'x' is not used by any target\n", swiftPackage.resolve(packageDirectory).stderr)
        assertEquals("warning: dependency 'x' is not used by any target\n", swiftPackage.build(packageDirectory).stderr)
    }

    @Test
    fun `resolve without a scratch path`() {
        assertEquals(listOf(XCRUN, "swift", "package", "resolve"), swiftPackage.resolveInvocation(packageDirectory).commandLine())
    }

    @Test
    fun `describe returns JSON`() {
        response = result(stdout = """{"name":"Synthetic"}""")

        assertEquals("""{"name":"Synthetic"}""", swiftPackage.describe(packageDirectory))
        assertEquals(listOf(XCRUN, "swift", "package", "describe", "--type", "json"), runner.single.commandLine())
    }

    @Test
    fun `build passes arguments through`() {
        swiftPackage.build(packageDirectory, listOf("-c", "release"))

        assertEquals(listOf(XCRUN, "swift", "build", "-c", "release"), runner.single.commandLine())
    }

    @Test
    fun `showBinPath returns the first line as a path`() {
        response = result(stdout = "/project/Synthetic Package/.build/arm64-apple-macosx/debug\n")

        assertEquals(path("/project/Synthetic Package/.build/arm64-apple-macosx/debug"), swiftPackage.showBinPath(packageDirectory))
        assertEquals(listOf(XCRUN, "swift", "build", "--show-bin-path"), runner.single.commandLine())
    }

    @Test
    fun `showBinPath with empty output is an error`() {
        response = result(stdout = "\n")

        val exception = assertFailsWith<IllegalStateException> { swiftPackage.showBinPath(packageDirectory) }
        assertEquals(
            "Expected a path, but there was no output from: DEVELOPER_DIR='/Applications/Xcode 16.app/Contents/Developer' " +
                    "/usr/bin/xcrun swift build --show-bin-path",
            exception.message,
        )
    }
}
