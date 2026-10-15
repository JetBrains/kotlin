/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerTest

/**
 * This helper executes sanity tests for klib backward/forward compatibility testsuite,
 * making sure all the ignores are handled properly, including expected exceptions for handcrafted tests.
 * So, the grouping testinfra itself is tested.
 *
 * In contrast, usual grouping testrunners use [initTestRunnerAndCreateModuleStructure],
 * which follows ignore/disable test directives and converts exceptions to "ignored" test results.
 */
fun AbstractTwoStageKotlinCompilerTest.runSanityTest(@TestDataFile filePath: String) {
    initTestRunnerAndCreateModuleStructure(filePath)
    try {
        nonGroupingRunner.runTestPreprocessing()
        nonGroupingRunner.runSteps()

        val hadIgnoredFailuresOnFirstStage = nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
        if (hadIgnoredFailuresOnFirstStage) return

        val nonGroupingStageOutput = NonGroupingStageOutput(
            testServices = nonGroupingRunner.testServices,
            catchingExecutor = { wrapper, block ->
                nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
            },
        )
        groupingStageRunner.run(listOf(nonGroupingStageOutput))

        nonGroupingRunner.failuresInterceptor += groupingStageRunner.failuresInterceptor
        nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
    } finally {
        nonGroupingRunner.finalizeAndDispose()
    }
}
