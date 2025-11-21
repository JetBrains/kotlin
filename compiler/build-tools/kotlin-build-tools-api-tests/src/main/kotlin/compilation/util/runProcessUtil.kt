/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.util

import java.nio.file.Path
import kotlin.io.path.absolutePathString

class ProcessRunResult(
    private val cmd: List<String>,
    private val workingDir: Path,
    val exitCode: Int,
    val output: String,
) {
    val isSuccessful: Boolean
        get() = exitCode == 0

    override fun toString(): String =
        """
        |Executing process was ${if (isSuccessful) "successful" else "unsuccessful"}
        |    Command: ${cmd.joinToString()}
        |    Working directory: ${workingDir.absolutePathString()}
        |    Exit code: $exitCode
        """.trimMargin()
}

fun runProcess(
    cmd: List<String>,
    workingDir: Path,
): ProcessRunResult {
    val process = ProcessBuilder(cmd).apply {
        directory(workingDir.toFile())
        redirectErrorStream(true)
    }.start()

    val output = StringBuilder()
    process.inputStream!!.bufferedReader().forEachLine {
        output.append(it).append(System.lineSeparator())
    }
    val exitCode = process.waitFor()

    return ProcessRunResult(cmd, workingDir, exitCode, output.toString())
}