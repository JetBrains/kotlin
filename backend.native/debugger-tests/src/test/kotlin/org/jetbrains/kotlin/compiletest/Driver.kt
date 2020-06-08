/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.compiletest

import org.jetbrains.kotlin.cli.bc.K2Native
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ToolDriver(
        private val konancDriver: Path,
        private val lldb: Path,
        private val lldbPrettyPrinters: Path,
        private val useInProcessCompiler: Boolean = false
) {
    fun compile(source: Path, output: Path, vararg args: String) {
        check(!Files.exists(output))
        val allArgs = listOf("-output", output.toString(), source.toString(), *args).toTypedArray()

        if (useInProcessCompiler) {
            K2Native.main(allArgs)
        } else {
            subprocess(konancDriver, *allArgs).thrownIfFailed()

        }
        check(Files.exists(output)) {
            "Compiler has not produced an output at $output"
        }
    }

    fun runLldb(program: Path, commands: List<String>): String {
        val args = listOf("-o", "command script import \"$lldbPrettyPrinters\"") +
                commands.flatMap { listOf("-o", it) }
        return subprocess(lldb, program.toString(), "-b", *args.toTypedArray())
                .thrownIfFailed()
                .stdout
    }
}

data class ProcessOutput(
        val program: Path,
        val process: Process,
        val stdout: String,
        val stderr: String,
        val durationMs: Long
) {
    fun thrownIfFailed(): ProcessOutput {
        fun renderStdStream(name: String, text: String): String =
                if (text.isBlank()) "$name is empty" else "$name:\n$text"

        check(process.exitValue() == 0) {
            """$program exited with non-zero value: ${process.exitValue()}
              |${renderStdStream("stdout", stdout)}
              |${renderStdStream("stderr", stderr)}
            """.trimMargin()
        }
        return this
    }
}

fun subprocess(program: Path, vararg args: String): ProcessOutput {
    val start = System.currentTimeMillis()
    val process = ProcessBuilder(program.toString(), *args).start()
    val outReader = process.inputStream.bufferedReader()
    val errReader = process.errorStream.bufferedReader()

    val timeout = 5L
    if (!process.waitFor(timeout, TimeUnit.MINUTES)) {
        process.destroy()
        error("$program is running for more then $timeout minutes")
    }
    val stdout = outReader.readText()
    val stderr = errReader.readText()
    return ProcessOutput(program, process, stdout, stderr, System.currentTimeMillis() - start)
}