/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests

import com.intellij.util.PlatformUtils
import kotlinx.coroutines.*
import org.jetbrains.kotlin.android.tests.emulator.Emulator
import org.jetbrains.kotlin.android.tests.gradle.GradleRunner
import org.jetbrains.kotlin.android.tests.run.ProcessFailedException
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.junit.jupiter.api.Assertions.assertNotEquals
import java.util.Base64

data class AndroidRuntimeTestResult(val testName: String, val failureText: String?, val elapsedTimeMs: Long)

interface AndroidRuntimeTestListener {
    fun testStarted(testName: String) {}
    fun testFinished(result: AndroidRuntimeTestResult) {}
}

class CodegenTestsOnAndroidRunner private constructor(private val pathManager: PathManager) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var emulator: Emulator? = null
    private var gradleRunner: GradleRunner? = null
    private var emulatorJob: Deferred<Unit>? = null
    private var logcatJob: Deferred<Unit>? = null
    private var adbServerStarted: Boolean = false
    private var isStarted: Boolean = false

    private fun detectArch(): String {
        val arch = System.getProperty("os.arch")?.toLowerCaseAsciiOnly() ?: return Emulator.X86

        return when {
            arch.startsWith("arm") || arch == "aarch64" -> Emulator.ARM
            else -> Emulator.X86
        }
    }

    private suspend fun startEmulator() {
        check(!isStarted) { "Android emulator is already started" }
        val emulatorType = detectArch()
        println("Using $emulatorType emulator!")
        val emulator = Emulator(pathManager, emulatorType)
        this.emulator = emulator

        try {
            emulator.createEmulator()

            val gradleRunner = GradleRunner(pathManager)
            this.gradleRunner = gradleRunner

            emulator.startAdbServer()
            adbServerStarted = true

            val emulatorJob = scope.async { emulator.runEmulator() }
            this.emulatorJob = emulatorJob

            emulator.waitEmulatorStart()
            if (emulatorJob.isCompleted) {
                emulatorJob.await()
            }
            emulator.waitForInstallStabilization()

            logcatJob = scope.async { emulator.printLog() }
            isStarted = true
        } catch (e: Throwable) {
            if (e is RuntimeException) {
                e.printStackTrace()
            }
            closeStartedProcesses()
            throw e
        }
    }

    fun buildFlavorApks(flavorName: String) {
        runBlocking {
            check(isStarted) { "Android emulator is not started" }
            val gradleRunner = gradleRunner ?: error("Gradle runner is not created")
            throwIfBackgroundProcessFailed()
            gradleRunner.assembleAndroidDebugTest(flavorName)
            throwIfBackgroundProcessFailed()
        }
    }

    fun installFlavorApks(flavorName: String) {
        runBlocking {
            check(isStarted) { "Android emulator is not started" }
            val emulator = emulator ?: error("Android emulator is not created")
            val gradleRunner = gradleRunner ?: error("Gradle runner is not created")
            throwIfBackgroundProcessFailed()
            installAndroidDebugTestWithRetry(gradleRunner, emulator, flavorName)
            throwIfBackgroundProcessFailed()
        }
    }

    fun runFlavorTestsAsync(
        flavorName: String,
        listener: AndroidRuntimeTestListener,
    ): Deferred<List<AndroidRuntimeTestResult>> {
        return scope.async {
            runFlavorTestsInStartedEmulator(flavorName, listener)
        }
    }

    private suspend fun runFlavorTestsInStartedEmulator(
        flavorName: String,
        listener: AndroidRuntimeTestListener,
    ): List<AndroidRuntimeTestResult> {
        check(isStarted) { "Android emulator is not started" }
        val emulator = emulator ?: error("Android emulator is not created")

        try {
            throwIfBackgroundProcessFailed()
            val className = flavorName.capitalizeAsciiOnly()
            return runTestsOnEmulator(emulator, className, listener).also {
                throwIfBackgroundProcessFailed()
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            throw e
        }
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

    private suspend fun runTestsOnEmulator(
        emulator: Emulator,
        className: String,
        listener: AndroidRuntimeTestListener,
    ): List<AndroidRuntimeTestResult> {
        val platformPrefixProperty = System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, "Idea")
        val statusParser = InstrumentationStatusParser(listener)
        try {
            emulator.runTestsViaInstrumentation(
                "org.jetbrains.kotlin.android.tests.$className",
                stdoutProcessor = statusParser::append,
            )
            statusParser.flush()
            return statusParser.results(className)
        } finally {
            if (platformPrefixProperty != null) {
                System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platformPrefixProperty)
            } else {
                System.clearProperty(PlatformUtils.PLATFORM_PREFIX_KEY)
            }
        }
    }

    private suspend fun throwIfBackgroundProcessFailed() {
        emulatorJob?.let { if (it.isCompleted) it.await() }
        logcatJob?.let { if (it.isCompleted) it.await() }
    }

    private class InstrumentationStatusParser(
        private val listener: AndroidRuntimeTestListener,
    ) {
        private val pendingText = StringBuilder()
        private val statusValues = linkedMapOf<String, String>()
        private val results = linkedMapOf<String, AndroidRuntimeTestResult>()
        private var lastStatusKey: String? = null

        fun append(chunk: String) {
            pendingText.append(chunk)
            while (true) {
                val lineEnd = pendingText.indexOf("\n")
                if (lineEnd == -1) return

                val line = pendingText.substring(0, lineEnd).trimEnd('\r')
                pendingText.delete(0, lineEnd + 1)
                processLine(line)
            }
        }

        fun flush() {
            if (pendingText.isNotEmpty()) {
                processLine(pendingText.toString().trimEnd('\r'))
                pendingText.clear()
            }
        }

        fun results(suiteName: String): List<AndroidRuntimeTestResult> {
            assertNotEquals(0L, results.size.toLong(), "There is no test results in status output for $suiteName")
            return results.values.toList()
        }

        private fun processLine(line: String) {
            when {
                line.startsWith(INSTRUMENTATION_STATUS_PREFIX) -> {
                    val payload = line.removePrefix(INSTRUMENTATION_STATUS_PREFIX)
                    val separatorIndex = payload.indexOf('=')
                    if (separatorIndex == -1) {
                        lastStatusKey = null
                        return
                    }

                    val key = payload.substring(0, separatorIndex)
                    val value = payload.substring(separatorIndex + 1)
                    statusValues[key] = value
                    lastStatusKey = key
                }

                line.startsWith(INSTRUMENTATION_STATUS_CODE_PREFIX) -> {
                    reportStatusBlock()
                    statusValues.clear()
                    lastStatusKey = null
                }

                line.isNotBlank() && lastStatusKey != null -> {
                    val key = lastStatusKey ?: return
                    statusValues[key] = statusValues.getValue(key) + line.trim()
                }
            }
        }

        private fun reportStatusBlock() {
            when (statusValues[EVENT_KEY]) {
                EVENT_STARTED -> {
                    val testName = statusValues[TEST_NAME_KEY] ?: return
                    listener.testStarted(testName)
                }

                EVENT_FINISHED -> {
                    val testName = statusValues[TEST_NAME_KEY] ?: return
                    val status = statusValues[STATUS_KEY] ?: return
                    val elapsedTimeMs = statusValues[ELAPSED_TIME_MS_KEY]?.toLongOrNull() ?: return
                    val failureText = statusValues[FAILURE_PAYLOAD_KEY]?.let { payload ->
                        String(Base64.getDecoder().decode(payload.replace("\\s+".toRegex(), "")))
                    }
                    val result = AndroidRuntimeTestResult(
                        testName,
                        failureText.takeIf { status == STATUS_FAIL },
                        elapsedTimeMs,
                    )
                    check(results.put(result.testName, result) == null) {
                        "Duplicate Android runtime status result for ${result.testName}"
                    }
                    listener.testFinished(result)
                }
            }
        }

        companion object {
            private const val INSTRUMENTATION_STATUS_PREFIX = "INSTRUMENTATION_STATUS: "
            private const val INSTRUMENTATION_STATUS_CODE_PREFIX = "INSTRUMENTATION_STATUS_CODE: "
            private const val EVENT_KEY = "kotlinBoxEvent"
            private const val EVENT_STARTED = "started"
            private const val EVENT_FINISHED = "finished"
            private const val TEST_NAME_KEY = "testName"
            private const val STATUS_KEY = "status"
            private const val STATUS_FAIL = "FAIL"
            private const val ELAPSED_TIME_MS_KEY = "elapsedTimeMs"
            private const val FAILURE_PAYLOAD_KEY = "failurePayloadBase64"
        }
    }

    override fun close() {
        try {
            runBlocking {
                closeStartedProcesses()
            }
        } finally {
            scope.cancel()
        }
    }

    private suspend fun closeStartedProcesses() {
        withContext(NonCancellable) {
            logcatJob?.cancelAndJoin()
            logcatJob = null
            emulatorJob?.cancelAndJoin()
            emulatorJob = null
            if (adbServerStarted) {
                emulator?.stopAdbServer()
                adbServerStarted = false
            }
            isStarted = false
        }
    }

    companion object {
        private const val INSTALL_ATTEMPTS = 2

        @JvmStatic
        fun startEmulator(pathManager: PathManager): CodegenTestsOnAndroidRunner {
            val runner = CodegenTestsOnAndroidRunner(pathManager)
            try {
                runBlocking {
                    runner.startEmulator()
                }
                return runner
            } catch (e: Throwable) {
                try {
                    runner.close()
                } catch (closeException: Throwable) {
                    e.addSuppressed(closeException)
                }
                throw e
            }
        }
    }
}
