/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisabledOnOs(OS.WINDOWS)
class ProcessAppleToolRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    private val runner = ProcessAppleToolRunner()

    private fun script(body: String): Path {
        val directory = tempDir.resolve("tools with spaces ü").createDirectories()
        return directory.resolve("tool.sh").apply {
            writeText("#!/bin/sh\n$body\n")
            setPosixFilePermissions(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))
        }
    }

    @Test
    fun `captures stdout, stderr and the exit code`() {
        val tool = script("""echo "out: ${'$'}1"; echo "err" >&2; exit 3""")

        val result = runner.run(AppleToolInvocation(tool, listOf("a b")))

        assertEquals(3, result.exitCode)
        assertEquals("out: a b\n", result.stdout)
        assertEquals("err\n", result.stderr)
    }

    @Test
    fun `starts in the working directory`() {
        val workingDirectory = tempDir.resolve("work dir").createDirectories()
        val tool = script("pwd -P")

        val result = runner.run(AppleToolInvocation(tool, emptyList(), workingDirectory = workingDirectory))

        assertEquals(workingDirectory.toRealPath().toString(), result.stdout.trim())
    }

    @Test
    fun `removes and sets environment variables`() {
        // HOME is always inherited from the test JVM. PATH is not usable here: sh restores a default when it is unset.
        val tool = script("""echo "HOME=${'$'}{HOME-unset} MARKER=${'$'}MARKER"""")

        val byName = runner.run(AppleToolInvocation(tool, emptyList(), environmentCleanup = EnvironmentCleanup(names = setOf("HOME"))))
        val byPrefix = runner.run(
            AppleToolInvocation(tool, emptyList(), environment = mapOf("MARKER" to "set"), environmentCleanup = EnvironmentCleanup(prefixes = setOf("HOM")))
        )

        assertEquals("HOME=unset MARKER=", byName.stdout.trim())
        assertEquals("HOME=unset MARKER=set", byPrefix.stdout.trim())
    }

    @Test
    fun `forwards stdout line by line`() {
        val tool = script("printf 'one\\ntwo\\nthree'")
        val lines = mutableListOf<String>()

        val result = runner.run(AppleToolInvocation(tool, emptyList(), output = OutputMode.Forward { lines += it }))

        assertEquals(listOf("one", "two", "three"), lines)
        assertEquals("", result.stdout)
    }

    @Test
    fun `discards stdout but keeps stderr`() {
        val tool = script("echo out; echo err >&2")

        val result = runner.run(AppleToolInvocation(tool, emptyList(), output = OutputMode.Discard))

        assertEquals("", result.stdout)
        assertEquals("err\n", result.stderr)
    }

    @Test
    fun `forwards stderr line by line and still keeps it for error messages`() {
        val tool = script("echo out; echo 'err one' >&2; echo 'err two' >&2")
        val lines = java.util.concurrent.ConcurrentLinkedQueue<String>()
        val errors = java.util.concurrent.ConcurrentLinkedQueue<String>()

        val result = runner.run(AppleToolInvocation(tool, emptyList(), output = OutputMode.Forward({ lines += it }, { errors += it })))

        assertEquals(listOf("out"), lines.toList())
        assertEquals(listOf("err one", "err two"), errors.toList())
        assertEquals("err one\nerr two\n", result.stderr)
    }

    @Test
    fun `reads large output on both pipes without blocking`() {
        val tool = script("i=0; while [ \$i -lt 20000 ]; do echo \"out \$i\"; echo \"err \$i\" >&2; i=\$((i+1)); done")

        val result = runner.run(AppleToolInvocation(tool, emptyList()))

        assertEquals(20000, result.stdout.lines().count { it.startsWith("out ") })
        assertEquals(20000, result.stderr.lines().count { it.startsWith("err ") })
    }

    @Test
    fun `writes the input to stdin`() {
        val tool = script("cat")

        val result = runner.run(AppleToolInvocation(tool, emptyList(), input = "héllo\nwörld\n"))

        assertEquals("héllo\nwörld\n", result.stdout)
    }

    @Test
    fun `a process that ignores its input still finishes`() {
        val tool = script("echo done")

        val result = runner.run(AppleToolInvocation(tool, emptyList(), input = "x".repeat(1_000_000)))

        assertEquals("done\n", result.stdout)
    }

    @Test
    fun `a throwing stdout consumer stops the process`() {
        val tool = script("echo \$\$; exec sleep 600")
        var pid = ""

        val exception = assertFailsWith<IllegalStateException> {
            runner.run(AppleToolInvocation(tool, emptyList(), output = OutputMode.Forward { pid = it; error("consumer failed") }))
        }

        assertEquals("consumer failed", exception.message)
        assertProcessStops(pid)
    }

    @Test
    fun `a throwing stderr consumer stops the process`() {
        val tool = script("echo \$\$ >&2; exec sleep 600")
        var pid = ""

        val exception = assertFailsWith<IllegalStateException> {
            runner.run(AppleToolInvocation(tool, emptyList(), output = OutputMode.Forward({ }, { pid = it; error("consumer failed") })))
        }

        assertEquals("consumer failed", exception.message)
        assertProcessStops(pid)
    }

    @Test
    fun `interrupting the calling thread stops the process`() {
        val tool = script("echo \$\$; exec sleep 600")
        val started = java.util.concurrent.CountDownLatch(1)
        var pid = ""
        var failure: Throwable? = null
        val caller = Thread {
            try {
                runner.run(AppleToolInvocation(tool, emptyList(), output = OutputMode.Forward { pid = it; started.countDown() }))
            } catch (e: Throwable) {
                failure = e
            }
        }.apply { start() }

        started.await()
        caller.interrupt()
        caller.join()

        assertTrue(failure is InterruptedException, "Unexpected failure: $failure")
        assertProcessStops(pid)
    }

    /** Waits for the process to be reaped; `kill -0` fails once it is gone. */
    private fun assertProcessStops(pid: String) {
        assertTrue(pid.isNotEmpty() && pid.all { it.isDigit() }, "Expected a pid, got '$pid'")
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30)
        while (System.nanoTime() < deadline) {
            if (ProcessBuilder("kill", "-0", pid).start().waitFor() != 0) return
            Thread.yield()
        }
        kotlin.test.fail("Process $pid is still running")
    }

    @Test
    fun `stdin is empty, so tools waiting for input do not hang`() {
        val tool = script("cat")

        val result = runner.run(AppleToolInvocation(tool, emptyList()))

        assertEquals(0, result.exitCode)
        assertEquals("", result.stdout)
    }

    @Test
    fun `a missing executable throws AppleToolUnavailableException`() {
        val missing = tempDir.resolve("missing-tool")

        val exception = assertFailsWith<AppleToolUnavailableException> {
            runner.run(AppleToolInvocation(missing, listOf("--version")))
        }

        assertTrue(exception.message!!.startsWith("Cannot start '$missing --version': "), exception.message)
    }
}
