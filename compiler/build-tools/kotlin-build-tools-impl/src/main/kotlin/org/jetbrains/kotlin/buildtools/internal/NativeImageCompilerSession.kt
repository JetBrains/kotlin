/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/** Owns one lazily started native compiler worker for a build session. */
internal class NativeImageCompilerSession(
    private val startProcess: () -> Process = {
        ProcessBuilder(nativeImageCompilerCommand())
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
    },
) : AutoCloseable {
    private val requestLock = ReentrantLock()
    private val lifecycleLock = Any()
    private var worker: Worker? = null
    private var activeRequest: Any? = null
    private var closed = false

    fun compile(
        arguments: List<String>,
        stdout: Path,
        stderr: Path,
        checkCanceled: () -> Unit,
        onCancel: (() -> Unit) -> Unit,
    ): Int {
        val request = Any()
        onCancel {
            // A late cancellation of an already completed request must not kill another request.
            synchronized(lifecycleLock) {
                if (activeRequest === request) worker?.process?.destroyForcibly()
            }
        }
        requestLock.lockInterruptibly()
        var current: Worker? = null
        try {
            checkCanceled()
            current = synchronized(lifecycleLock) {
                check(!closed) { "The native compiler build session is closed" }
                activeRequest = request
                worker ?: Worker(startProcess()).also { worker = it }
            }
            checkCanceled()
            // Pipe I/O runs on a worker thread so interrupting the caller also cancels a blocked request.
            val result = current.exchange(arguments, stdout, stderr)
            checkCanceled()
            return result
        } catch (e: Exception) {
            val failed = current
            if (failed != null) {
                synchronized(lifecycleLock) {
                    if (worker === failed) worker = null
                }
                failed.close(gracefully = false)
            }
            throw e
        } finally {
            synchronized(lifecycleLock) {
                if (activeRequest === request) activeRequest = null
            }
            requestLock.unlock()
        }
    }

    override fun close() {
        val state = synchronized(lifecycleLock) {
            closed = true
            val state = worker to (activeRequest == null)
            worker = null
            state
        }
        state.first?.close(gracefully = state.second)
    }

    private class Worker(val process: Process) {
        private val input = DataInputStream(process.inputStream.buffered())
        private val output = DataOutputStream(process.outputStream.buffered())
        private val io = Executors.newSingleThreadExecutor { action ->
            Thread(action, "kotlin-native-image-protocol").apply { isDaemon = true }
        }
        private var handshakeRead = false

        fun exchange(arguments: List<String>, stdout: Path, stderr: Path): Int {
            try {
                return io.submit<Int> {
                    if (!handshakeRead) {
                        if (input.readInt() != 0x4b4e4931) throw IOException("Unsupported native compiler worker protocol")
                        handshakeRead = true
                    }
                    output.writeInt(arguments.size)
                    output.writeString(stdout.toAbsolutePath().toString())
                    output.writeString(stderr.toAbsolutePath().toString())
                    arguments.forEach { output.writeString(it) }
                    output.flush()
                    input.readInt()
                }.get()
            } catch (e: ExecutionException) {
                throw IOException("Communication with the native compiler worker failed", e.cause)
            }
        }

        fun close(gracefully: Boolean) {
            try {
                if (gracefully && process.isAlive) {
                    io.submit {
                        output.writeInt(-1)
                        output.flush()
                    }.get(5, TimeUnit.SECONDS)
                    if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly()
                } else {
                    process.destroyForcibly()
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                process.destroyForcibly()
            } catch (_: Exception) {
                process.destroyForcibly()
            } finally {
                io.shutdownNow()
            }
        }

        private fun DataOutputStream.writeString(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeInt(bytes.size)
            write(bytes)
        }
    }
}

internal fun nativeImageCompilerCommand(
    propertyHome: String? = System.getProperty("kotlin.native.image.home"),
    environmentHome: String? = System.getenv("KOTLIN_NATIVE_IMAGE_HOME"),
): List<String> {
    val home = (propertyHome?.takeIf { it.isNotBlank() } ?: environmentHome?.takeIf { it.isNotBlank() })
        ?.let { File(it).absoluteFile }
        ?: throw IOException("Set kotlin.native.image.home or KOTLIN_NATIVE_IMAGE_HOME to the native compiler distribution")
    val suffix = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) ".exe" else ""
    val executable = home.resolve("bin/kotlinc-native-image$suffix")
    if (!executable.isFile || !executable.canExecute()) {
        throw IOException("Native compiler executable not found or not executable: $executable")
    }
    return listOf(
        executable.absolutePath,
        "-Djava.home=${System.getenv("JAVA_HOME")?.takeIf { it.isNotBlank() } ?: System.getProperty("java.home")}",
        "-Dkotlin.home=$home",
        "--native-image-server",
    )
}
