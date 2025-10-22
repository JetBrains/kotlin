/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.generators.tests

import org.jetbrains.kotlin.fir.AbstractIsolatedFulPipelineTestRunner
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.test.HeavyTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File

internal fun TestGroupSuite.generateModularizedTests(
    baseGenPath: String,
    testDataProjectName: String,
    testDataModelPath: String,
) {
    testGroup(baseGenPath, testDataModelPath, testRunnerMethodName = "runTest") {
        testClass<AbstractIsolatedFulPipelineTestRunner>(
            "${testDataProjectName}FullPipelineTestsGenerated",
            annotations =
                listOf(
                    annotation(Execution::class.java, ExecutionMode.CONCURRENT)
                )
        ) {
            model(pattern = "^model-(.+)\\.xml$")
        }
    }
}

object GenerateModularizedIsolatedTests {

    @JvmStatic
    fun main(args: Array<String>) {
        println("Arguments: ${args.toList()}")
        require(args.size > 0) { "Expected at least generation path in the arguments" }
        val baseGenPath = args[0]
        var i = 1
        while (i < args.size && args[i] != "--") ++i
        require(i < args.size) { "Expected a \"tail\"(--) argument" }
        ++i
        while (i < args.size) {
            val testDataProjectName = args[i++]
            require(i < args.size) { "invalid arguments format, expecting pairs of project name and path" }
            val testDataModelPath = args[i++]
            if (!testDataModelPath.let { File(it).exists() }) {
                println("Skipping ${testDataProjectName} project: path is empty or incorrect ($testDataModelPath)")
                continue
            }

            println("Generating ${testDataProjectName} project tests from $testDataModelPath")
            generateTestGroupSuiteWithJUnit5(args, mainClassName = "GenerateModularizedTests") {
                generateModularizedTests(baseGenPath, testDataProjectName, testDataModelPath)
            }
        }
    }
}