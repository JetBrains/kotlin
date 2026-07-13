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
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import java.util.Base64

class CodegenTestsOnAndroidRunner private constructor(private val pathManager: PathManager) {
    private fun detectArch(): String {
        val arch = System.getProperty("os.arch")?.toLowerCaseAsciiOnly() ?: return Emulator.X86

        return when {
            arch.startsWith("arm") || arch == "aarch64" -> Emulator.ARM
            else -> Emulator.X86
        }
    }

    private suspend fun runTestsInEmulator(): List<DynamicNode> {
        val allTests = mutableListOf<DynamicNode>()

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
                        val dynamicTests = runTestsOnEmulator(emulator, className)
                        allTests.add(DynamicContainer.dynamicContainer(className, dynamicTests))
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

        return allTests
    }

    private fun processReport(resultOutput: String, suiteName: String): List<DynamicTest> {
        try {
            val testCases = parseInstrumentationOutput(resultOutput)
            assertNotEquals(0L, testCases.size.toLong(), "There is no test results in report for $suiteName")
            return testCases
        } catch (e: Throwable) {
            throw RuntimeException("Can't parse test results for $suiteName\n$resultOutput", e)
        }
    }

    private fun parseInstrumentationOutput(output: String): List<DynamicTest> {
        val casePrefix = "KOTLIN_BOX_CASE|"
        val markerPrefix = casePrefix.substringBefore('|')
        val statusFail = "FAIL"
        val lines = extractResultSection(output)
        val dynamicTests = arrayListOf<DynamicTest>()
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

            dynamicTests.add(DynamicTest.dynamicTest(testName) {
                if (failureText != null) {
                    fail(failureText)
                }
            })
        }

        return dynamicTests
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

    private suspend fun runTestsOnEmulator(emulator: Emulator, className: String): List<DynamicTest> {
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

    companion object {
        private const val INSTALL_ATTEMPTS = 2

        private val flavorsToRun: List<String> = listOf(
            "common0", "common1", "common2", "common3", "common4", "reflect0",
        )

        @JvmStatic
        fun runTestsInEmulator(pathManager: PathManager): List<DynamicNode> {
            val result: List<DynamicNode>
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
