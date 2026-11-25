/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path
import java.nio.file.Paths

/** Records invocations and answers them with [respond]; by default every command succeeds with empty output. */
class RecordingAppleToolRunner(
    private val respond: (AppleToolInvocation) -> AppleToolResult = { AppleToolResult(0, "", "") },
) : AppleToolRunner {
    val invocations = mutableListOf<AppleToolInvocation>()

    override fun run(invocation: AppleToolInvocation): AppleToolResult {
        invocations += invocation
        return respond(invocation)
    }

    val single: AppleToolInvocation get() = invocations.single()
}

fun result(stdout: String = "", stderr: String = "", exitCode: Int = 0): AppleToolResult = AppleToolResult(exitCode, stdout, stderr)

fun path(first: String, vararg more: String): Path = Paths.get(first, *more)

const val XCRUN = "/usr/bin/xcrun"
