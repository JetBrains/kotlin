/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.generators.tests

import org.jetbrains.kotlin.fir.AbstractIsolatedFulPipelineTestRunner
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

private val BASE_GEN_PATH = "compiler/fir/modularized-tests/tests-gen"

internal fun TestGroupSuite.generateModularizedTests(
    testDataProjectName: String,
    testDataModelPath: String,
) {
    testGroup(BASE_GEN_PATH, testDataModelPath, testRunnerMethodName = "runTest") {
        testClass<AbstractIsolatedFulPipelineTestRunner>("${testDataProjectName}FullPipelineTestsGenerated") {
            model(pattern = "^model-(.+)\\.xml$")
        }
    }
}

object GenerateModularizedTests {

    @JvmStatic
    fun main(args: Array<String>) {
        val testDataProjectName = System.getProperty("fir.modularized.test.project.name")
        val testDataModelPath = System.getProperty("fir.modularized.test.model.path")
        if (testDataProjectName == null || testDataModelPath == null) {
            println("fir.modularized.test.project.name system property should be set")
            println("fir.modularized.test.model.path system property should be set")
            return
        }

        generateTestGroupSuiteWithJUnit5(args, mainClassName = "GenerateModularizedTests") {
            generateModularizedTests(testDataProjectName, testDataModelPath)
        }
    }
}