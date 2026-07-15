/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.tests

import com.google.common.base.StandardSystemProperty
import com.intellij.openapi.util.io.FileUtil
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(AndroidRuntimeReportExtension::class)
@Execution(ExecutionMode.SAME_THREAD)
class AndroidRunner {
    companion object {
        private var pathManager: PathManager? = null
        private var runtimeResults: AndroidRuntimeResultsCache? = null

        @JvmStatic
        @AfterAll
        fun tearDown() {
            try {
                runtimeResults?.close()
            } finally {
                runtimeResults = null
                pathManager?.tmpFolder?.let { FileUtil.delete(File(it)) }
            }
        }
    }

    @TestFactory
    fun runTests(): List<DynamicNode> {
        val tmpFolder = Files.createTempDirectory(
            Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value()!!), null
        ).toFile()
        println("Created temporary folder for running android tests: ${tmpFolder.absolutePath}")
        pathManager = PathManager(tmpFolder.absolutePath)
        val plan = CodegenTestsOnAndroidGenerator.discover(pathManager!!)
        val runtimeResults = AndroidRuntimeResultsCache(pathManager!!, plan)
        AndroidRunner.runtimeResults = runtimeResults
        return listOf(
            DynamicContainer.dynamicContainer(
                "Discovery",
                listOf(DynamicTest.dynamicTest("Discovered ${plan.tests.size} Android box tests") {
                    if (plan.tests.isEmpty()) {
                        fail("There are no Android box tests to run")
                    }
                })
            ),
            DynamicContainer.dynamicContainer(
                "Compilation",
                plan.tests.map { test ->
                    DynamicTest.dynamicTest(test.displayName) {
                        plan.compile(test)
                    }
                }
            ),
            DynamicContainer.dynamicContainer(
                "Emulator",
                listOf(DynamicTest.dynamicTest("Start Android emulator") {
                    runtimeResults.startEmulator()
                })
            ),
            DynamicContainer.dynamicContainer(
                "Runtime",
                plan.tests.map<AndroidPlannedTest, DynamicNode> { test ->
                    DynamicTest.dynamicTest(test.displayName) {
                        runtimeResults.assertPassed(test)
                    }
                } + DynamicTest.dynamicTest("No unexpected Android test results") {
                    runtimeResults.assertNoUnexpectedResults()
                }
            )
        )
    }

    private class AndroidRuntimeResultsCache(
        private val pathManager: PathManager,
        private val plan: AndroidTestPlan
    ) {
        private val plannedTestNames = plan.tests.mapTo(linkedSetOf()) { it.info.name }
        private var cachedRunner: Result<CodegenTestsOnAndroidRunner>? = null
        private var runtimeRun: RuntimeRun? = null

        fun close() {
            cachedRunner?.getOrNull()?.close()
        }

        fun startEmulator() {
            emulatorRunner()
        }

        fun assertPassed(test: AndroidPlannedTest) {
            val result = runBlocking {
                runtimeRun().awaitStarted(test)
                AndroidRuntimeReport.publishStarted(test)
                runtimeRun().awaitFinished(test).also {
                    AndroidRuntimeReport.publishFinished(test, it)
                }
            }
            val failureText = result.failureText
            if (failureText != null) {
                fail<Unit>("Android runtime failure for ${test.info.name} (${test.info.file.path}):\n$failureText")
            }
        }

        fun assertNoUnexpectedResults() {
            val unexpectedTestNames = runBlocking {
                runtimeRun().awaitResults().keys - plannedTestNames
            }
            if (unexpectedTestNames.isNotEmpty()) {
                fail<Unit>("Unexpected Android runtime results: ${unexpectedTestNames.joinToString()}")
            }
        }

        private fun emulatorRunner(): CodegenTestsOnAndroidRunner {
            val cached = cachedRunner
            if (cached != null) return cached.getOrThrow()

            val result = runCatching {
                plan.generateUnitTestFiles()
                CodegenTestsOnAndroidRunner.startEmulator(pathManager)
            }
            cachedRunner = result
            return result.getOrThrow()
        }

        private fun runtimeRun(): RuntimeRun {
            runtimeRun?.let { return it }

            println("Run tests on Android...")
            return RuntimeRun(plan, emulatorRunner()).also {
                runtimeRun = it
            }
        }

        private class RuntimeRun(
            private val plan: AndroidTestPlan,
            runner: CodegenTestsOnAndroidRunner,
        ) {
            private val started = plan.tests.associate { it.info.name to CompletableDeferred<Unit>() }
            private val finished = plan.tests.associate { it.info.name to CompletableDeferred<AndroidRuntimeTestResult>() }

            private val asyncResults: Deferred<Map<String, AndroidRuntimeTestResult>> = runner.runTestsAsync(
                plan.flavorsToRun,
                object : AndroidRuntimeTestListener {
                    override fun testStarted(testName: String) {
                        started[testName]?.complete(Unit)
                    }

                    override fun testFinished(result: AndroidRuntimeTestResult) {
                        started[result.testName]?.complete(Unit)
                        finished[result.testName]?.complete(result)
                    }
                }
            )

            init {
                asyncResults.invokeOnCompletion { cause ->
                    if (cause != null) {
                        completePendingExceptionally(cause)
                    } else {
                        completeMissingResults()
                    }
                }
            }

            suspend fun awaitStarted(test: AndroidPlannedTest) {
                started.getValue(test.info.name).await()
            }

            suspend fun awaitFinished(test: AndroidPlannedTest): AndroidRuntimeTestResult {
                return finished.getValue(test.info.name).await()
            }

            suspend fun awaitResults(): Map<String, AndroidRuntimeTestResult> {
                return asyncResults.await()
            }

            private fun completePendingExceptionally(cause: Throwable) {
                for (job in started.values) {
                    job.completeExceptionally(cause)
                }
                for (job in finished.values) {
                    job.completeExceptionally(cause)
                }
            }

            private fun completeMissingResults() {
                for (job in started.values) {
                    job.complete(Unit)
                }
                for (test in plan.tests) {
                    finished.getValue(test.info.name).completeExceptionally(
                        AssertionError("No Android runtime result for ${test.info.name} (${test.info.file.path})")
                    )
                }
            }
        }
    }
}

private object AndroidRuntimeReport {
    private val currentContext = ThreadLocal<ExtensionContext>()

    fun <T> withContext(extensionContext: ExtensionContext, action: () -> T): T {
        val previous = currentContext.get()
        currentContext.set(extensionContext)
        return try {
            action()
        } finally {
            if (previous == null) {
                currentContext.remove()
            } else {
                currentContext.set(previous)
            }
        }
    }

    fun publishStarted(test: AndroidPlannedTest) {
        publish(
            mapOf(
                "androidRuntimeEvent" to "started",
                "testName" to test.info.name,
                "testFile" to test.info.file.path,
            )
        )
    }

    fun publishFinished(test: AndroidPlannedTest, result: AndroidRuntimeTestResult) {
        publish(
            mapOf(
                "androidRuntimeEvent" to "finished",
                "testName" to test.info.name,
                "testFile" to test.info.file.path,
                "status" to if (result.failureText == null) "OK" else "FAIL",
                "elapsedTimeMs" to result.elapsedTimeMs.toString(),
            )
        )
    }

    private fun publish(values: Map<String, String>) {
        currentContext.get()?.publishReportEntry(values)
    }
}

internal class AndroidRuntimeReportExtension : InvocationInterceptor {
    override fun interceptDynamicTest(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: DynamicTestInvocationContext,
        extensionContext: ExtensionContext,
    ) {
        AndroidRuntimeReport.withContext(extensionContext) {
            invocation.proceed()
        }
    }
}
