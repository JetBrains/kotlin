/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests

import com.intellij.util.PlatformUtils
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.android.tests.emulator.Emulator
import org.jetbrains.kotlin.android.tests.gradle.GradleRunner
import org.jetbrains.kotlin.android.tests.run.ProcessFailedException
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.junit.jupiter.api.Assertions.assertNotEquals
import java.util.Base64

data class AndroidRuntimeTestResult(val testName: String, val failureText: String?, val elapsedTimeMs: Long)

class CodegenTestsOnAndroidRunner private constructor(private val pathManager: PathManager) {
    private fun detectArch(): String {
        val arch = System.getProperty("os.arch")?.toLowerCaseAsciiOnly() ?: return Emulator.X86

        return when {
            arch.startsWith("arm") || arch == "aarch64" -> Emulator.ARM
            else -> Emulator.X86
        }
    }

    private suspend fun runTestsInEmulator(flavorsToRun: List<String>): Map<String, AndroidRuntimeTestResult> {
        assertNotEquals(0L, flavorsToRun.size.toLong(), "There are no generated Android test flavors to run")
        val allResults = linkedMapOf<String, AndroidRuntimeTestResult>()

        val emulatorType = detectArch()
        println("Using $emulatorType emulator!")
        val emulator = Emulator(pathManager, emulatorType)

        coroutineScope {
            emulator.createEmulator()

            val gradleRunner = GradleRunner(pathManager)
            cleanAndBuildProject(gradleRunner)

            emulator.startAdbServer()

            val emulatorJob = launch { emulator.runEmulator() }

            try {
                emulator.waitEmulatorStart()
                emulator.waitForInstallStabilization()

                val logcatJob = launch { emulator.printLog() }

                try {
                    for (flavor in flavorsToRun) {
                        installAndroidDebugTestWithRetry(gradleRunner, emulator, flavor)
                        val className = flavor.capitalizeAsciiOnly()
                        val flavorResults = runTestsOnEmulator(emulator, className)
                        reportElapsedTimes(flavor, flavorResults)
                        for (result in flavorResults) {
                            check(allResults.put(result.testName, result) == null) {
                                "Duplicate Android runtime result for ${result.testName}"
                            }
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        logcatJob.cancelAndJoin()
                    }
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
                throw e
            } finally {
                withContext(NonCancellable) {
                    emulatorJob.cancelAndJoin()
                    emulator.stopAdbServer()
                }
            }
        }

        return allResults
    }

    private fun processReport(resultOutput: String, suiteName: String): List<AndroidRuntimeTestResult> {
        try {
            val testCases = parseInstrumentationOutput(resultOutput)
            assertNotEquals(0L, testCases.size.toLong(), "There is no test results in report for $suiteName")
            return testCases
        } catch (e: Throwable) {
            throw RuntimeException("Can't parse test results for $suiteName\n$resultOutput", e)
        }
    }

    private fun parseInstrumentationOutput(output: String): List<AndroidRuntimeTestResult> {
        val casePrefix = "KOTLIN_BOX_CASE|"
        val markerPrefix = casePrefix.substringBefore('|')
        val statusFail = "FAIL"
        val lines = extractResultSection(output)
        val results = arrayListOf<AndroidRuntimeTestResult>()
        val logicalLines = arrayListOf<String>()
        var pendingLine: StringBuilder? = null

        for (line in lines) {
            val startsWithMarker = line.startsWith(markerPrefix) || line.startsWith("KOTLIN_BOX_RESULTS_END")
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
            val elapsedTimeMs = chunks[3].toLong()
            val failureText = if (status == statusFail && chunks.size == 5) {
                String(Base64.getDecoder().decode(chunks[4].replace("\\s+".toRegex(), "")))
            } else {
                null
            }

            results += AndroidRuntimeTestResult(testName, failureText, elapsedTimeMs)
        }

        return results
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

    private suspend fun runTestsOnEmulator(emulator: Emulator, className: String): List<AndroidRuntimeTestResult> {
        val platformPrefixProperty = System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, "Idea")
        try {
            val resultOutput = emulator.runTestsViaInstrumentation("org.jetbrains.kotlin.android.tests.$className")
            return processReport(resultOutput, className)
        } finally {
            if (platformPrefixProperty != null) {
                System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platformPrefixProperty)
            } else {
                System.clearProperty(PlatformUtils.PLATFORM_PREFIX_KEY)
            }
        }
    }

    private fun reportElapsedTimes(flavor: String, results: List<AndroidRuntimeTestResult>) {
        println("Android runtime elapsed times for flavor $flavor:")
        for (result in results) {
            println("  ${result.testName}: ${result.elapsedTimeMs} ms")
        }
    }

    companion object {
        private const val INSTALL_ATTEMPTS = 2

        @JvmStatic
        fun runTestsInEmulator(pathManager: PathManager, flavorsToRun: List<String>): Map<String, AndroidRuntimeTestResult> {
            val result: Map<String, AndroidRuntimeTestResult>
            runBlocking {
                result = CodegenTestsOnAndroidRunner(pathManager).runTestsInEmulator(flavorsToRun)
            }
            return result
        }

        private suspend fun cleanAndBuildProject(gradleRunner: GradleRunner) {
            gradleRunner.clean()
            gradleRunner.assembleAndroidTest()
        }
    }
}
