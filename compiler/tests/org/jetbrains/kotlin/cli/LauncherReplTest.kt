/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import com.intellij.openapi.util.SystemInfo
import junit.framework.TestCase
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LauncherReplTest : TestCaseWithTmpdir() {

    private fun runInteractive(
        executableName: String,
        vararg inputs: String,
        expectedOutPatterns: List<String> = emptyList(),
        expectedExitCode: Int = 0,
        workDirectory: File? = null
    ) {
        val executableFileName = if (SystemInfo.isWindows) "$executableName.bat" else executableName
        val launcherFile = File(PathUtil.kotlinPathsForDistDirectory.homePath, "bin/$executableFileName")
        assertTrue("Launcher script not found, run dist task: ${launcherFile.absolutePath}", launcherFile.exists())

        val processBuilder = ProcessBuilder(launcherFile.absolutePath)
        if (workDirectory != null) {
            processBuilder.directory(workDirectory)
        }
        val process = processBuilder.start()

        val inputIter = inputs.iterator()

        data class ExceptionContainer(
            var value: Throwable? = null
        )

        fun InputStream.captureStream(): Triple<Thread, ExceptionContainer, ArrayList<String>> {
            val out = ArrayList<String>()
            val exceptionContainer = ExceptionContainer()
            val thread = thread {
                try {
                    reader().forEachLine {
                        out.add(it.trim())
                    }
                } catch (e: Throwable) {
                    exceptionContainer.value = e
                }
            }
            return Triple(thread, exceptionContainer, out)
        }

        val (stdoutThread, stdoutException, processOut) = process.inputStream.captureStream()
        val (stderrThread, stderrException, processErr) = process.errorStream.captureStream()

        var stdinException: Throwable? = null
        val stdinThread =
            thread {
                try {
                    val writer = process.outputStream.writer()
                    val eol = System.getProperty("line.separator")
                    while (inputIter.hasNext()) {
                        with(writer) {
                            write(inputIter.next())
                            write(eol)
                            flush()
                        }
                    }
                } catch (e: Throwable) {
                    stdinException = e
                }
            }

        process.waitFor(10000, TimeUnit.MILLISECONDS)

        try {
            if (process.isAlive) {
                process.destroyForcibly()
                TestCase.fail("Process terminated forcibly")
            }
            stdoutThread.join(100)
            TestCase.assertFalse("stdout thread not finished", stdoutThread.isAlive)
            TestCase.assertNull(stdoutException.value)
            TestCase.assertFalse("stderr thread not finished", stderrThread.isAlive)
            TestCase.assertNull(stderrException.value)
            TestCase.assertFalse("stdin thread not finished", stdinThread.isAlive)
            TestCase.assertNull(stdinException)
            TestCase.assertEquals(expectedOutPatterns.size, processOut.size)
            for (i in 0 until expectedOutPatterns.size) {
                val expectedPattern = expectedOutPatterns[i]
                val actualLine = processOut[i]
                TestCase.assertTrue(
                    "line \"$actualLine\" do not match with expected pattern \"$expectedPattern\"",
                    Regex(expectedPattern).matches(actualLine)
                )
            }
            TestCase.assertEquals(expectedExitCode, process.exitValue())
            TestCase.assertFalse(inputIter.hasNext())

        } catch (e: Throwable) {
            println("OUT:\n${processOut.joinToString("\n")}")
            println("ERR:\n${processErr.joinToString("\n")}")
            println("REMAINING IN:\n${inputIter.asSequence().joinToString("\n")}")
            throw e
        }
    }

    fun testSimpleRepl() {
        runInteractive(
            "kotlinc",
            "println(42)",
            ":quit",
            expectedOutPatterns = listOf(
                "Welcome to Kotlin version .*",
                "Type :help for help, :quit for quit",
                ".*42$",
                ".*"
            )
        )
    }
}