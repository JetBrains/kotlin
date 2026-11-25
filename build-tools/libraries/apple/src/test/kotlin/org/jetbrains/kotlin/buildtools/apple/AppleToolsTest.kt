/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppleToolsTest {
    private val runner = RecordingAppleToolRunner()

    @Test
    fun `toolchain tools run through xcrun with DEVELOPER_DIR`() {
        val invocation = AppleTools(fakeXcode, runner).lipo.createInvocation(listOf(path("/a.a")), path("/out.a"))

        assertEquals(path(XCRUN), invocation.executable)
        assertEquals("lipo", invocation.arguments.first())
        assertEquals(mapOf("DEVELOPER_DIR" to "/Applications/Xcode 16.app/Contents/Developer"), invocation.environment)
    }

    @Test
    fun `OS tools run by absolute path with DEVELOPER_DIR`() {
        val invocation = AppleTools(fakeXcode, runner).plutil.convertToJsonInvocation(path("/p.plist"))

        assertEquals(path("/usr/bin/plutil"), invocation.executable)
        assertEquals(mapOf("DEVELOPER_DIR" to "/Applications/Xcode 16.app/Contents/Developer"), invocation.environment)
    }

    @Test
    fun `a toolchain is passed as TOOLCHAINS`() {
        val invocation = AppleTools(fakeXcode, runner, toolchain = "swift").swiftPackage.describeInvocation(path("/pkg"))

        assertEquals(
            mapOf("DEVELOPER_DIR" to "/Applications/Xcode 16.app/Contents/Developer", "TOOLCHAINS" to "swift"),
            invocation.environment,
        )
    }

    @Test
    fun `an inherited TOOLCHAINS is removed unless a toolchain is given`() {
        val default = AppleTools(fakeXcode, runner).lipo.createInvocation(listOf(path("/a.a")), path("/out.a"))
        val explicit = AppleTools(fakeXcode, runner, toolchain = "swift").lipo.createInvocation(listOf(path("/a.a")), path("/out.a"))

        assertEquals(EnvironmentCleanup(names = setOf("TOOLCHAINS")), default.environmentCleanup)
        assertEquals(EnvironmentCleanup.NONE, explicit.environmentCleanup)
    }

    @Test
    fun `tools that need Xcode app fail before running with Command Line Tools`() {
        val tools = AppleTools(fakeCommandLineTools, runner)

        val xcodebuild = assertFailsWith<AppleToolUnavailableException> { tools.xcodebuild.versionInvocation() }
        assertFailsWith<AppleToolUnavailableException> { tools.simctl.listDevices() }

        assertEquals(
            "'xcodebuild' needs Xcode.app, but the selected developer dir is Command Line Tools at /Library/Developer/CommandLineTools. " +
                    "Install Xcode and select it with 'sudo xcode-select --switch <path to Xcode.app>' or DEVELOPER_DIR.",
            xcodebuild.message,
        )
        assertEquals(emptyList(), runner.invocations)
    }

    @Test
    fun `tools that work with Command Line Tools still run`() {
        val tools = AppleTools(fakeCommandLineTools, runner)

        tools.lipo.create(listOf(path("/a.a")), path("/out.a"))

        assertEquals(mapOf("DEVELOPER_DIR" to "/Library/Developer/CommandLineTools"), runner.single.environment)
    }
}
