/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SimctlTest {
    private val runner = RecordingAppleToolRunner { result(stdout = "== Devices ==\n") }
    private val simctl = AppleTools(fakeXcode, runner).simctl

    @Test
    fun `listDevices lists available devices as text by default`() {
        assertEquals("== Devices ==\n", simctl.listDevices())
        assertEquals(listOf(XCRUN, "simctl", "list", "devices", "available"), runner.single.commandLine())
    }

    @Test
    fun `listDevices can ask for JSON and every device`() {
        assertEquals(
            listOf(XCRUN, "simctl", "list", "-j", "devices"),
            simctl.listDevicesInvocation(availableOnly = false, json = true).commandLine(),
        )
    }

    @Test
    fun `listRuntimes asks for JSON`() {
        assertEquals(listOf(XCRUN, "simctl", "list", "-j", "runtimes"), simctl.listRuntimesInvocation().commandLine())
    }

    @Test
    fun `spawn puts flags before the device and passes arguments through`() {
        val invocation = simctl.spawnInvocation(
            device = "iPhone 16 Pro",
            executable = path("/build/test kexe/test.kexe"),
            arguments = listOf("--", "--ktest_logger=TEAMCITY"),
            waitForDebugger = true,
            standalone = true,
        )

        assertEquals(
            listOf(
                XCRUN, "simctl", "spawn", "--wait-for-debugger", "--standalone",
                "iPhone 16 Pro", "/build/test kexe/test.kexe", "--", "--ktest_logger=TEAMCITY",
            ),
            invocation.commandLine(),
        )
    }

    @Test
    fun `spawn needs a device`() {
        assertFailsWith<IllegalArgumentException> { simctl.spawnInvocation(" ", path("/test.kexe")) }
    }
}
