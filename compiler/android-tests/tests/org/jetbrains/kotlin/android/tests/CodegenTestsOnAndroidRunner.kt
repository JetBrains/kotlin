/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests

import com.intellij.util.PlatformUtils
import junit.framework.TestCase
import junit.framework.TestSuite
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.android.tests.emulator.Emulator
import org.jetbrains.kotlin.android.tests.gradle.GradleRunner
import org.jetbrains.kotlin.android.tests.run.ProcessFailedException
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.junit.Assert
import java.util.Base64

class CodegenTestsOnAndroidRunner private constructor(private val pathManager: PathManager) {
    private fun detectArch(): String {
        val arch = System.getProperty("os.arch")?.toLowerCaseAsciiOnly() ?: return Emulator.X86

        return when {
            arch.startsWith("arm") || arch == "aarch64" -> Emulator.ARM
            else -> Emulator.X86
        }
    }

    private suspend fun runTestsInEmulator(): TestSuite {
        val rootSuite = TestSuite("Root")

        val emulatorType = detectArch()
        println("Using $emulatorType emulator!")
        val emulator = Emulator(pathManager, emulatorType)

        coroutineScope {
            emulator.createEmulator()

            val gradleRunner = GradleRunner(pathManager)
            cleanAndBuildProject(gradleRunner)

            emulator.startServer()

            val emulatorJob = launch { emulator.runEmulator() }
            val logcatJob = launch { emulator.printLog() }

            try {
                emulator.waitEmulatorStart()
                emulator.waitForInstallStabilization()

                for (flavor in flavorsToRun) {
                    installAndroidDebugTestWithRetry(gradleRunner, emulator, flavor)
                    val className = flavor.capitalizeAsciiOnly()
                    runTestsOnEmulator(emulator, className, TestSuite(className)).apply {
                        rootSuite.addTest(this)
                    }
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
                throw e
            } finally {
                withContext(NonCancellable) {
                    logcatJob.cancelAndJoin()
                    emulatorJob.cancelAndJoin()
                    emulator.stopAdbServer()
                }
            }
        }

        return rootSuite
    }

    private fun processReport(rootSuite: TestSuite, resultOutput: String, suiteName: String) {
        try {
            val testCases = parseInstrumentationOutput(resultOutput)
            testCases.forEach { rootSuite.addTest(it) }
            Assert.assertNotEquals("There is no test results in report for $suiteName", 0, testCases.size.toLong())
        } catch (e: Throwable) {
            throw RuntimeException("Can't parse test results for $suiteName\n$resultOutput", e)
        }
    }

    private fun parseInstrumentationOutput(output: String): List<TestCase> {
        val casePrefix = "KOTLIN_BOX_CASE|"
        val markerPrefix = casePrefix.substringBefore('|')
        val statusFail = "FAIL"
        val lines = extractResultSection(output)
        val testCases = arrayListOf<TestCase>()
        val logicalLines = arrayListOf<String>()
        var pendingLine: StringBuilder? = null

        for (line in lines) {
            val startsWithMarker = line.startsWith(markerPrefix)
            if (startsWithMarker) {
                pendingLine?.let { logicalLines.add(it.toString()) }
                pendingLine = StringBuilder(line)
                continue
            }

            if (pendingLine == null) {
                pendingLine = StringBuilder(line)
                continue
            }

            // Non-marker line is a continuation:
            // - if previous logical line started with marker => payload split
            // - if previous logical line did not start with marker => marker itself was split
            if (line.isNotBlank()) {
                pendingLine.append(line.trim())
            }
        }

        pendingLine?.let { logicalLines.add(it.toString()) }

        for (line in logicalLines) {
            val markerIndex = line.indexOf(casePrefix)
            if (markerIndex == -1) continue

            val payload = line.substring(markerIndex)
            val chunks = payload.split("|", limit = 4)
            if (chunks.size < 3) continue

            val testName = chunks[1]
            val status = chunks[2]
            val failureText = if (status == statusFail && chunks.size == 4) {
                String(Base64.getDecoder().decode(chunks[3].replace("\\s+".toRegex(), "")))
            } else {
                null
            }

            testCases.add(object : TestCase(testName) {
                @Throws(Throwable::class)
                override fun runTest() {
                    if (failureText != null) {
                        Assert.fail(failureText)
                    }
                }
            })
        }

        return testCases
    }

    private fun extractResultSection(output: String): List<String> {
        val resultBegin = "KOTLIN_BOX_RESULTS_BEGIN"
        val resultEnd = "KOTLIN_BOX_RESULTS_END"

        val lines = output.lines()
        val resultLines = arrayListOf<String>()
        var collecting = false

        for (rawLine in lines) {
            val beginIdx = rawLine.indexOf(resultBegin)
            if (beginIdx != -1) {
                collecting = true
                resultLines.add(rawLine.substring(beginIdx))
                continue
            }

            if (!collecting) continue

            val line = if (rawLine.startsWith("INSTRUMENTATION_RESULT: stream=")) {
                rawLine.removePrefix("INSTRUMENTATION_RESULT: stream=")
            } else {
                rawLine
            }
            resultLines.add(line)

            if (line.contains(resultEnd)) {
                break
            }
        }

        return resultLines
    }

    private suspend fun installAndroidDebugTestWithRetry(
        gradleRunner: GradleRunner,
        emulator: Emulator,
        flavor: String,
    ) {
        var firstFailure: ProcessFailedException? = null

        repeat(INSTALL_ATTEMPTS) { attemptIndex ->
            val attempt = attemptIndex + 1
            try {
                gradleRunner.installAndroidDebugTest(flavor)
                return
            } catch (e: ProcessFailedException) {
                emulator.dumpInstallDiagnostics(
                    "Install for flavor $flavor failed on attempt $attempt/$INSTALL_ATTEMPTS: ${e.result}"
                )

                if (attempt == INSTALL_ATTEMPTS) {
                    firstFailure?.let { e.addSuppressed(it) }
                    throw e
                }

                if (firstFailure == null) {
                    firstFailure = e
                }

                val retryDelay = emulator.installRetryDelay()
                println("Waiting ${retryDelay.inWholeSeconds}s before retrying install for flavor $flavor...")
                delay(retryDelay)
            }
        }
    }

    private suspend fun runTestsOnEmulator(emulator: Emulator, className: String, suite: TestSuite): TestSuite {
        val platformPrefixProperty = System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, "Idea")
        try {
            val resultOutput = emulator.runTestsViaInstrumentation("org.jetbrains.kotlin.android.tests.$className")
            processReport(suite, resultOutput, className)
            return suite
        } finally {
            if (platformPrefixProperty != null) {
                System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platformPrefixProperty)
            } else {
                System.clearProperty(PlatformUtils.PLATFORM_PREFIX_KEY)
            }
        }

    }

    companion object {
        private const val INSTALL_ATTEMPTS = 2

        private val flavorsToRun: List<String> = listOf(
            "common0", "common1", "common2", "common3", "common4", "reflect0",
        )

        @JvmStatic
        fun runTestsInEmulator(pathManager: PathManager): TestSuite {
            val result: TestSuite
            runBlocking {
                result = CodegenTestsOnAndroidRunner(pathManager).runTestsInEmulator()
            }
            return result
        }

        private suspend fun cleanAndBuildProject(gradleRunner: GradleRunner) {
            gradleRunner.clean()
            gradleRunner.assembleAndroidTest()
        }
    }
}
