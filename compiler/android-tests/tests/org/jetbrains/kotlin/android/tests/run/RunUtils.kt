/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests.run

import com.intellij.execution.configurations.GeneralCommandLine
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ProcessResult(
    val command: GeneralCommandLine,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    override fun toString(): String =
        """$command exited with exit code $exitCode:
           STDOUT:
           $stdout
           STDERR:
           $stderr
        """.trimIndent()
}

class ProcessFailedException(
    val result: ProcessResult,
) : RuntimeException(
    "Process failed with exit code ${result.exitCode}: ${result.command.commandLineString}\n${result.stderr}"
)

suspend fun runProcessCancellable(
    command: GeneralCommandLine,
    stdin: String? = null,
    timeout: Duration? = null,
    checkExitCode: Boolean = true,
): ProcessResult = coroutineScope {
    withContext(Dispatchers.IO) {
        val process = runInterruptible {
            command.createProcess()
        }

        runInterruptible {
            process.outputStream.bufferedWriter().use { writer ->
                if (stdin != null) writer.write(stdin)
                writer.flush()
            }
        }

        val stdout = async(Dispatchers.IO) {
            process.inputStream.readTextInterruptibly(System.out, process)
        }

        val stderr = async(Dispatchers.IO) {
            process.errorStream.readTextInterruptibly(System.err, process)
        }

        try {
            val runLambda = suspend {
                runInterruptible {
                    process.waitFor()
                }
            }

            val exitCode =
                if (timeout != null) withTimeoutOrNull(timeout) { runLambda() }
                else runLambda()

            val result = ProcessResult(
                command = command,
                exitCode = exitCode ?: -1,
                stdout = stdout.await(),
                stderr = stderr.await(),
            )

            if (checkExitCode && exitCode != 0) {
                throw ProcessFailedException(result)
            }

            result
        } catch (e: CancellationException) {
            process.destroy()
            withContext(NonCancellable) {
                delay(500.milliseconds)
                process.destroyForcibly()
            }
            throw e
        }
    }
}

private suspend fun InputStream.readTextInterruptibly(
    out: PrintStream,
    process: Process,
): String {
    val buffer = StringBuilder()

    withContext(Dispatchers.IO) {
        bufferedReader().use { reader ->
            val chars = CharArray(256)

            while (true) {
                val read = try {
                    reader.read(chars)
                } catch (e: IOException) {
                    if (!currentCoroutineContext().isActive || !process.isAlive || e.message == "Stream closed") {
                        break
                    }
                    throw e
                }

                if (read < 0) break

                val chunk = String(chars, 0, read)
                buffer.append(chunk)
                out.print(chunk)
                out.flush()
            }
        }
    }

    return buffer.toString()
}
