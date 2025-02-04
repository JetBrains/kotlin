/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import org.opentest4j.MultipleFailuresError
import java.io.File

private val enabled = false
private val updateTestData = enabled || System.getProperty("kotlin.test.update.test.data") == "true"

/**
 * Those two helpers do nothing normally.
 *
 * They have the same functionality, but for different test frameworks:
 * if change [enabled] to true or set system property `kotlin.test.update.test.data` to true,
 * then all text dump related files would be rewritten on test failures.
 *
 * This is useful to update test data after change of dump format.
 *
 * This mechanism is meant for "simple" changes - it will just override a file with an expected content.
 * If some other change in test data is required, like adding/removing/renaming files, it should be done manually.
 *
 * No rewrite happens on muted tests.
 *
 * For example, if you want to update IR text test data by K2 with the JVM backend, you need to run:
 * ```
 * ./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.ir.FirPsiJvmIrTextTestGenerated" --continue -Pkotlin.test.update.test.data=true
 * ```
 */

class UpdateTestDataSupport : TestExecutionExceptionHandler {
    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) {
        if (updateTestData) {
            throwable.tryUpdateTestData()
        }
        throw throwable
    }
}

class UpdateTestDataHandler(
    testServices: TestServices
) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (updateTestData) {
            failedAssertions.forEach {
                it.cause.tryUpdateTestData()
            }
        }
        return failedAssertions
    }
}

private fun Throwable.tryUpdateTestData() {
    when {
        this is AssertionFailedError -> tryUpdateTestData()
        this is MultipleFailuresError -> this.failures.forEach { it.tryUpdateTestData() }
    }
}

private fun AssertionFailedError.tryUpdateTestData() {
    val path = (expected?.value as? FileInfo)?.path ?: return
    File(path).writeText(actual.stringRepresentation)
}
