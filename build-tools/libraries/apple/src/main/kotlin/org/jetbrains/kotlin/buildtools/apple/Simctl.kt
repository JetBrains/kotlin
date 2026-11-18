/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path

/**
 * `xcrun simctl`. Every operation needs Xcode.app and throws [AppleToolUnavailableException] for Command Line Tools.
 *
 * Lists come back as text; parsing the JSON form is left to the caller.
 */
public class Simctl internal constructor(private val tools: AppleTools) {

    /** `simctl list [-j] devices [available]`. */
    public fun listDevicesInvocation(availableOnly: Boolean = true, json: Boolean = false): AppleToolInvocation =
        invocation(buildList {
            add("list")
            if (json) add("-j")
            add("devices")
            if (availableOnly) add("available")
        })

    /** Runs [listDevicesInvocation] and returns its output. */
    public fun listDevices(availableOnly: Boolean = true, json: Boolean = false): String =
        tools.runner.runChecked(listDevicesInvocation(availableOnly, json)).stdout

    /** `simctl list -j runtimes`. */
    public fun listRuntimesInvocation(): AppleToolInvocation = invocation(listOf("list", "-j", "runtimes"))

    /** The installed simulator runtimes as `simctl` JSON. */
    public fun listRuntimes(): String = tools.runner.runChecked(listRuntimesInvocation()).stdout

    /**
     * `simctl spawn [--wait-for-debugger] [--standalone] <device> <executable> <arguments>`.
     *
     * Test executors start this command themselves, so there is no method that runs it.
     *
     * @param device a device UDID or name, or `booted`.
     * @param standalone let `simctl` boot and shut down the device around the process.
     */
    public fun spawnInvocation(
        device: String,
        executable: Path,
        arguments: List<String> = emptyList(),
        waitForDebugger: Boolean = false,
        standalone: Boolean = false,
        output: OutputMode = OutputMode.Capture,
    ): AppleToolInvocation {
        require(device.isNotBlank()) { "The device must not be blank" }
        return invocation(
            buildList {
                add("spawn")
                if (waitForDebugger) add("--wait-for-debugger")
                if (standalone) add("--standalone")
                add(device)
                add(executable.toString())
                addAll(arguments)
            },
            output,
        )
    }

    private fun invocation(arguments: List<String>, output: OutputMode = OutputMode.Capture): AppleToolInvocation {
        tools.requireXcodeApp("simctl")
        return tools.toolchainInvocation("simctl", arguments, output = output)
    }
}
