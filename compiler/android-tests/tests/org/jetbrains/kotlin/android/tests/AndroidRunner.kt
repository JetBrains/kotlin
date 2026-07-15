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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
        private var compilationResults: AndroidCompilationResultsCache? = null
        private var runtimeResults: AndroidRuntimeResultsCache? = null

        @JvmStatic
        @AfterAll
        fun tearDown() {
            try {
                compilationResults?.close()
            } finally {
                compilationResults = null
                try {
                    runtimeResults?.close()
                } finally {
                    runtimeResults = null
                    pathManager?.tmpFolder?.let { FileUtil.delete(File(it)) }
                }
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
        val compilationResults = AndroidCompilationResultsCache(plan)
        val runtimeResults = AndroidRuntimeResultsCache(pathManager!!, plan)
        AndroidRunner.compilationResults = compilationResults
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
                plan.flavorContainers { _, tests ->
                    tests.directoryContainers { test ->
                        DynamicTest.dynamicTest(test.displayName) {
                            compilationResults.assertCompiled(test)
                        }
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
                plan.flavorContainers { flavorName, tests ->
                    listOf(
                        DynamicTest.dynamicTest("Build APKs") {
                            runtimeResults.buildFlavorApks(flavorName)
                        },
                        DynamicTest.dynamicTest("Install APKs") {
                            runtimeResults.installFlavorApks(flavorName)
                        },
                        DynamicContainer.dynamicContainer(
                            "Tests",
                            tests.directoryContainers { test ->
                                DynamicTest.dynamicTest(test.displayName) {
                                    runtimeResults.assertPassed(test)
                                }
                            }
                        )
                    )
                } + DynamicTest.dynamicTest("No unexpected Android test results") {
                    runtimeResults.assertNoUnexpectedResults()
                }
            )
        )
    }

    private fun AndroidTestPlan.flavorContainers(containerFactory: (String, List<AndroidPlannedTest>) -> List<DynamicNode>): List<DynamicNode> {
        val testsByFlavor = tests.groupBy { it.flavorName }
        return flavorsToRun.map { flavorName ->
            DynamicContainer.dynamicContainer(
                flavorName,
                containerFactory(flavorName, testsByFlavor.getValue(flavorName))
            )
        }
    }

    private fun List<AndroidPlannedTest>.directoryContainers(testFactory: (AndroidPlannedTest) -> DynamicTest): List<DynamicNode> {
        val root = DirectoryNode("")
        for (test in this) {
            root.add(relativeDirectorySegments(test), test)
        }
        return root.children.values.map { it.toDynamicNode(testFactory) } + root.tests.map(testFactory)
    }

    private fun relativeDirectorySegments(test: AndroidPlannedTest): List<String> {
        val relativePath = FileUtil.toSystemIndependentName(test.info.file.relativeTo(AndroidPlannedTest.ROOT_PATH).parent ?: "")
        val segments = relativePath.split('/').filter { it.isNotEmpty() }
        return if (segments.firstOrNull() == "codegen") segments.drop(1) else segments
    }

    private class DirectoryNode(
        private val name: String,
    ) {
        val children = linkedMapOf<String, DirectoryNode>()
        val tests = arrayListOf<AndroidPlannedTest>()

        fun add(segments: List<String>, test: AndroidPlannedTest) {
            if (segments.isEmpty()) {
                tests += test
                return
            }

            children.getOrPut(segments.first()) { DirectoryNode(segments.first()) }.add(segments.drop(1), test)
        }

        fun toDynamicNode(testFactory: (AndroidPlannedTest) -> DynamicTest): DynamicNode {
            val compressed = compressed()
            return DynamicContainer.dynamicContainer(
                compressed.name,
                compressed.children.values.map { it.toDynamicNode(testFactory) } + compressed.tests.map(testFactory)
            )
        }

        private fun compressed(): DirectoryNode {
            var result = this
            while (result.tests.isEmpty() && result.children.size == 1) {
                val child = result.children.values.single()
                result = DirectoryNode(result.name + "/" + child.name).also {
                    it.children.putAll(child.children)
                    it.tests.addAll(child.tests)
                }
            }
            return result
        }
    }

    private class AndroidCompilationResultsCache(
        private val plan: AndroidTestPlan
    ) : AutoCloseable {
        private val parallelism = compilationParallelism()
        private val semaphore = Semaphore(parallelism)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val compilationJobs = plan.tests.associate { test ->
            test.info.name to scope.async(start = CoroutineStart.LAZY) {
                semaphore.withPermit {
                    plan.compile(test)
                }
            }
        }

        private var started = false

        fun assertCompiled(test: AndroidPlannedTest) {
            runBlocking {
                startAll()
                compilationJobs.getValue(test.info.name).await()
            }
        }

        override fun close() {
            scope.cancel()
        }

        private fun startAll() {
            if (started) return

            synchronized(this) {
                if (started) return

                println("Compiling Android tests with parallelism $parallelism...")
                compilationJobs.values.forEach { it.start() }
                started = true
            }
        }

        private fun compilationParallelism(): Int {
            val configured = System.getProperty("kotlin.test.android.compilation.parallelism")?.toIntOrNull()
            return (configured ?: Runtime.getRuntime().availableProcessors().coerceAtMost(DEFAULT_COMPILATION_PARALLELISM))
                .coerceAtLeast(1)
        }

        companion object {
            private const val DEFAULT_COMPILATION_PARALLELISM = 4
        }
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

        fun buildFlavorApks(flavorName: String) {
            emulatorRunner().buildFlavorApks(flavorName)
        }

        fun installFlavorApks(flavorName: String) {
            emulatorRunner().installFlavorApks(flavorName)
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
            plan: AndroidTestPlan,
            private val runner: CodegenTestsOnAndroidRunner,
        ) {
            private val testsByFlavor = plan.tests.groupBy { it.flavorName }
            private val started = plan.tests.associate { it.info.name to CompletableDeferred<Unit>() }
            private val finished = plan.tests.associate { it.info.name to CompletableDeferred<AndroidRuntimeTestResult>() }
            private val flavorRuns = linkedMapOf<String, Deferred<List<AndroidRuntimeTestResult>>>()

            private val listener = object : AndroidRuntimeTestListener {
                override fun testStarted(testName: String) {
                    started[testName]?.complete(Unit)
                }

                override fun testFinished(result: AndroidRuntimeTestResult) {
                    started[result.testName]?.complete(Unit)
                    finished[result.testName]?.complete(result)
                }
            }

            suspend fun awaitStarted(test: AndroidPlannedTest) {
                createFlavorRunIfNeeded(test.flavorName)
                started.getValue(test.info.name).await()
            }

            suspend fun awaitFinished(test: AndroidPlannedTest): AndroidRuntimeTestResult {
                createFlavorRunIfNeeded(test.flavorName)
                return finished.getValue(test.info.name).await()
            }

            suspend fun awaitResults(): Map<String, AndroidRuntimeTestResult> {
                val results = linkedMapOf<String, AndroidRuntimeTestResult>()
                for (flavorResult in flavorRuns.values.flatMap { it.await() }) {
                    check(results.put(flavorResult.testName, flavorResult) == null) {
                        "Duplicate Android runtime result for ${flavorResult.testName}"
                    }
                }
                return results
            }

            private fun createFlavorRunIfNeeded(flavorName: String) {
                if (flavorRuns.contains(flavorName)) return

                val run = runner.runFlavorTestsAsync(flavorName, listener)
                run.invokeOnCompletion { cause ->
                    if (cause != null) {
                        completeFlavorExceptionally(flavorName, cause)
                    } else {
                        completeMissingFlavorResults(flavorName)
                    }
                }
                flavorRuns[flavorName] = run
            }

            private fun completeFlavorExceptionally(flavorName: String, cause: Throwable) {
                for (test in testsByFlavor.getValue(flavorName)) {
                    val testName = test.info.name
                    started.getValue(testName).completeExceptionally(cause)
                    finished.getValue(testName).completeExceptionally(cause)
                }
            }

            private fun completeMissingFlavorResults(flavorName: String) {
                for (test in testsByFlavor.getValue(flavorName)) {
                    val testName = test.info.name
                    started.getValue(testName).complete(Unit)
                    finished.getValue(testName).completeExceptionally(
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
                "flavorName" to test.flavorName,
                "testFile" to test.info.file.path,
            )
        )
    }

    fun publishFinished(test: AndroidPlannedTest, result: AndroidRuntimeTestResult) {
        publish(
            mapOf(
                "androidRuntimeEvent" to "finished",
                "testName" to test.info.name,
                "flavorName" to test.flavorName,
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
