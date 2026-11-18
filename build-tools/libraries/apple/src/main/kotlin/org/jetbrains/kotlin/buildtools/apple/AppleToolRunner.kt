/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect
import java.util.concurrent.atomic.AtomicReference

/**
 * Starts an [AppleToolInvocation] and waits for it.
 *
 * Implementations return the result whatever the exit code; the tools compare it with
 * [AppleToolInvocation.acceptedExitCodes]. An implementation throws [AppleToolUnavailableException] when the process
 * cannot be started at all.
 *
 * A build tool integration supplies its own runner when it must start processes through the build tool's API.
 */
public fun interface AppleToolRunner {
    /** Runs [invocation] to completion. */
    public fun run(invocation: AppleToolInvocation): AppleToolResult
}

/** Exit code and output of a finished process. */
public class AppleToolResult(
    /** The process exit code. */
    public val exitCode: Int,
    /** Standard output, empty unless the invocation used [OutputMode.Capture]. */
    public val stdout: String,
    /** Standard error. */
    public val stderr: String,
) {
    override fun toString(): String = "AppleToolResult(exitCode=$exitCode)"
}

/**
 * An [AppleToolRunner] built on [ProcessBuilder]. The process inherits the environment of the current JVM.
 *
 * The process is killed when the calling thread is interrupted or an [OutputMode.Forward] consumer throws. Processes it
 * has started itself are not tracked and may outlive it.
 */
public class ProcessAppleToolRunner : AppleToolRunner {
    override fun run(invocation: AppleToolInvocation): AppleToolResult {
        val builder = ProcessBuilder(invocation.commandLine())
        invocation.workingDirectory?.let { builder.directory(it.toFile()) }
        invocation.applyEnvironment(builder.environment())
        if (invocation.input == null) builder.redirectInput(Redirect.from(nullDevice))
        if (invocation.output == OutputMode.Discard) builder.redirectOutput(Redirect.to(nullDevice))

        val process = try {
            builder.start()
        } catch (e: IOException) {
            throw AppleToolUnavailableException("Cannot start '$invocation': ${e.message}", e)
        }

        try {
            val toolName = invocation.executable.fileName
            invocation.input?.let { input ->
                daemon("stdin writer for $toolName") {
                    try {
                        process.outputStream.use { it.write(input.toByteArray()) }
                    } catch (_: IOException) {
                        // The process exited without reading all of its input.
                    }
                }
            }

            // Output is read on separate threads: a blocked pipe read cannot be interrupted, waitFor can.
            val failure = AtomicReference<Throwable>()
            fun reader(stream: String, body: () -> Unit): Thread = daemon("$stream reader for $toolName") {
                try {
                    body()
                } catch (e: Throwable) {
                    // Nobody reads this pipe anymore, so the process would block once it is full.
                    failure.compareAndSet(null, e)
                    process.destroyForcibly()
                }
            }

            val stdout = StringBuilder()
            val stdoutReader = when (val output = invocation.output) {
                OutputMode.Capture -> reader("stdout") { stdout.append(process.inputStream.bufferedReader().readText()) }
                is OutputMode.Forward -> reader("stdout") { process.inputStream.bufferedReader().forEachLine(output.consumer) }
                OutputMode.Discard -> null
            }
            val stderr = StringBuilder()
            val errorConsumer = (invocation.output as? OutputMode.Forward)?.errorConsumer
            val stderrReader = reader("stderr") {
                process.errorStream.bufferedReader().forEachLine { line ->
                    stderr.append(line).append('\n')
                    errorConsumer?.invoke(line)
                }
            }

            val exitCode = process.waitFor()
            stdoutReader?.join()
            stderrReader.join()
            failure.get()?.let { throw it }
            return AppleToolResult(exitCode, stdout.toString(), stderr.toString())
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun daemon(name: String, body: () -> Unit): Thread = Thread(body, name).apply {
        isDaemon = true
        start()
    }

    private companion object {
        val nullDevice = File("/dev/null")
    }
}

/** Runs [invocation] and throws [AppleToolException] when its exit code is not accepted. */
internal fun AppleToolRunner.runChecked(
    invocation: AppleToolInvocation,
    hint: (AppleToolResult) -> String? = { null },
): AppleToolResult {
    val result = run(invocation)
    if (result.exitCode !in invocation.acceptedExitCodes) {
        throw AppleToolException(invocation, result, hint(result))
    }
    return result
}
