/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerTest
import org.opentest4j.TestAbortedException

/**
 * This helper executes sanity tests for klib backward/forward compatibility testsuite,
 * making sure all the ignores are handled properly, including expected exceptions for handcrafted tests.
 * So, the grouping testinfra itself is tested.
 *
 * In contrast, usual grouping testrunners use [initTestRunnerAndCreateModuleStructure],
 * which follows ignore/disable test directives and converts exceptions to "ignored" test results.
 */
fun AbstractTwoStageKotlinCompilerTest.runSanityTest(@TestDataFile filePath: String) {
    runSanityTestStages(filePath)
}

/**
 * Runs a sanity test as [runSanityTest] does, and returns whether its second stage ran. It does not after failures of the first
 * stage, even if all of them were suppressed without muting the test.
 */
private fun AbstractTwoStageKotlinCompilerTest.runSanityTestStages(filePath: String): Boolean {
    initTestRunnerAndCreateModuleStructure(filePath)
    try {
        nonGroupingRunner.runTestPreprocessing()
        nonGroupingRunner.runSteps()

        val hadIgnoredFailuresOnFirstStage = nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
        if (hadIgnoredFailuresOnFirstStage) return false

        val nonGroupingStageOutput = NonGroupingStageOutput(
            testServices = nonGroupingRunner.testServices,
            catchingExecutor = { wrapper, block ->
                nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
            },
        )
        groupingStageRunner.run(listOf(nonGroupingStageOutput))

        nonGroupingRunner.failuresInterceptor += groupingStageRunner.failuresInterceptor
        nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
        return true
    } finally {
        nonGroupingRunner.finalizeAndDispose()
    }
}

/**
 * Runs a sanity test which must pass for real. A muted test is reported as skipped rather than failed, so without this check,
 * a problem muting every grouped test (like an unresolved annotation added to the files of a batched test) would go unnoticed.
 * Some suppressors drop failures of the first stage without muting the test, which then passes without its second stage, so
 * that fails the test too.
 */
fun AbstractTwoStageKotlinCompilerTest.runSanityTestNotMuted(@TestDataFile filePath: String) {
    val secondStageRan = try {
        runSanityTestStages(filePath)
    } catch (e: TestAbortedException) {
        throw AssertionError("The test is expected to pass, but it was muted", e)
    }
    if (!secondStageRan) throw AssertionError("The test is expected to pass, but the failures of its first stage were suppressed")
}
