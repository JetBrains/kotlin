/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File
import kotlin.collections.mapNotNull
import kotlin.io.writeText
import kotlin.text.endsWith

/**
 * Does nothing normally.
 *
 * If change [enabled] to true or set system property `kotlin.test.update.test.data` to true,
 * then all text dump related files would be rewritten on test failures.
 *
 * This is useful to update test data after change of dump format.
 *
 * No rewrite happens on muted tests.
 *
 * For example, if you want to update IR text test data by K2 with the JVM backend, you need to run:
 *
 * ./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.ir.FirPsiJvmIrTextTestGenerated" --continue -Pkotlin.test.update.test.data=true
 *
 */
class UpdateTestDataHandler(
    testServices: TestServices
) : AfterAnalysisChecker(testServices) {
    private val enabled = false

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (enabled || System.getProperty("kotlin.test.update.test.data") == "true") {
            for (failure in failedAssertions.mapNotNull { it.cause as? FileComparisonFailure }) {
                File(failure.filePath).writeText(failure.actual)
            }
        }
        return failedAssertions
    }
}