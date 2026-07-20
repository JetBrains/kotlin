/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.util.concurrent.TimeUnit

object CliProcessUtils {
    fun runProcess(
        executableName: String,
        vararg args: String,
        expectedStdout: String,
        expectedStderr: String,
        expectedExitCode: Int,
        workDirectory: File? = null,
        environment: Map<String, String> = mapOf("JAVA_HOME" to KtTestUtil.getJdk8Home().absolutePath),
        testDataDirectory: String,
        tmpdir: File,
    ) {
        runProcess(
            executableName = executableName,
            args = args,
            checkStdout = { stdout -> assertEquals(expectedStdout.trim(), stdout.trim()) },
            checkStderr = { stderr -> assertEquals(expectedStderr.trim(), stderr.trim()) },
            expectedExitCode = expectedExitCode,
            workDirectory = workDirectory,
            environment = environment,
            testDataDirectory = testDataDirectory,
            tmpdir = tmpdir
        )
    }

    fun runProcess(
        executableName: String,
        vararg args: String,
        checkStdout: (String) -> Unit,
        checkStderr: (String) -> Unit,
        expectedExitCode: Int,
        workDirectory: File? = null,
        environment: Map<String, String> = mapOf("JAVA_HOME" to KtTestUtil.getJdk8Home().absolutePath),
        testDataDirectory: String,
        tmpdir: File,
    ) {
        val executableFileName = if (SystemInfo.isWindows) "$executableName.bat" else executableName
        val launcherFile = File(PathUtil.kotlinPathsForDistDirectory.homePath, "bin/$executableFileName")
        assertTrue(launcherFile.exists()) { "Launcher script not found, run dist task: ${launcherFile.absolutePath}" }

        // For some reason, IntelliJ's ExecUtil screws quotes up on windows.
        // So, use ProcessBuilder instead.
        val pb = ProcessBuilder(
            launcherFile.absolutePath,
            // In cmd, `=` is delimiter, so we need to surround parameter with quotes.
            *quoteIfNeeded(args)
        )
        pb.environment().putAll(environment)
        pb.directory(workDirectory)
        val process = pb.start()
        /*
         * If the compiler invocation throws an exception, then the stderr could be bigger than pipe buffer (64 kb).
         * If it happens, trying to read from stdout first could clog the buffer and cause a deadlock. So the stderr should be read first.
         */
        val stderr =
            AbstractCliTest.getNormalizedCompilerOutput(
                StringUtil.convertLineSeparators(process.errorStream.bufferedReader().use { it.readText() }),
                null, testDataDirectory, tmpdir.absolutePath
            ).replace("Picked up [_A-Z]+:.*\n".toRegex(), "")
                .replace("The system cannot find the file specified", "No such file or directory") // win -> unix
        val stdout =
            AbstractCliTest.getNormalizedCompilerOutput(
                StringUtil.convertLineSeparators(process.inputStream.bufferedReader().use { it.readText() }),
                null, testDataDirectory, tmpdir.absolutePath
            )
        process.waitFor(10, TimeUnit.SECONDS)
        val exitCode = process.exitValue()
        try {
            checkStdout(stdout.trim())
            checkStderr(stderr.trim())

            assertEquals(expectedExitCode, exitCode)
        } catch (e: Throwable) {
            System.err.println("exit code $exitCode")
            System.err.println("=== STDOUT ===")
            System.err.println(stdout)
            System.err.println("=== STDERR ===")
            System.err.println(stderr)
            throw e
        } finally {
            process.destroy()
        }
    }

    private fun quoteIfNeeded(args: Array<out String>): Array<String> {
        @Suppress("UNCHECKED_CAST")
        return if (SystemInfo.isWindows) args.map {
            if (it.contains('=') || it.contains(" ") || it.contains(";") || it.contains(",")) "\"$it\"" else it
        }.toTypedArray()
        else args as Array<String>
    }
}
