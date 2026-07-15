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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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
        private var cachedResults: Result<Map<String, AndroidRuntimeTestResult>>? = null
        private var cachedRunner: Result<CodegenTestsOnAndroidRunner>? = null

        fun close() {
            cachedRunner?.getOrNull()?.close()
        }

        fun startEmulator() {
            emulatorRunner()
        }

        fun assertPassed(test: AndroidPlannedTest) {
            val result = results()[test.info.name] ?: fail<AndroidRuntimeTestResult>(
                "No Android runtime result for ${test.info.name} (${test.info.file.path})"
            )
            val failureText = result.failureText
            if (failureText != null) {
                fail<Unit>("Android runtime failure for ${test.info.name} (${test.info.file.path}):\n$failureText")
            }
        }

        fun assertNoUnexpectedResults() {
            val unexpectedTestNames = results().keys - plannedTestNames
            if (unexpectedTestNames.isNotEmpty()) {
                fail<Unit>("Unexpected Android runtime results: ${unexpectedTestNames.joinToString()}")
            }
        }

        private fun results(): Map<String, AndroidRuntimeTestResult> {
            val cached = cachedResults
            if (cached != null) return cached.getOrThrow()

            val result = runCatching {
                println("Run tests on Android...")
                emulatorRunner().runTests(plan.flavorsToRun)
            }
            cachedResults = result
            return result.getOrThrow()
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
    }
}
