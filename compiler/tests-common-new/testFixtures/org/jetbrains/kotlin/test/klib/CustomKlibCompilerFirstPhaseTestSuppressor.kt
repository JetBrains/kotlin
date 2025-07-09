/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Assumptions

/**
 * Mute (ignore) tests where the custom KLIB compiler failed to compile test data in the first (frontend) phase.
 */
class CustomKlibCompilerFirstPhaseTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val customCompilerAtFirstPhaseFailure: CustomKlibCompilerException? = failedAssertions.asSequence()
            .filter { wrappedException -> wrappedException is WrappedException.FromFacade && wrappedException.facade is CustomKlibCompilerFirstPhaseFacade }
            .mapNotNull { wrappedException -> wrappedException.cause as? CustomKlibCompilerException }
            .firstOrNull { unwrappedException -> unwrappedException.exitCode == ExitCode.COMPILATION_ERROR }

        if (customCompilerAtFirstPhaseFailure != null) {
            // Make sure that the compilation failure really looks like a frontend error. Otherwise, fail the test.
            testServices.assertions.assertTrue(FRONTEND_ERROR_MESSAGE_REGEX.containsMatchIn(customCompilerAtFirstPhaseFailure.compilerOutput)) {
                "Custom KLIB compiler failed to compile test data on the first (frontend) phase, but the failure does not look like a frontend error." +
                        " Please check the compiler output and update the test accordingly." +
                        " Compiler output: ${customCompilerAtFirstPhaseFailure.compilerOutput}"
            }

            // Explicitly mark the test as "ignored" if the test data cannot be compiled with an older (custom) compiler version.
            Assumptions.abort<Nothing>()
        }

        return failedAssertions
    }

    companion object {
        private val FRONTEND_ERROR_MESSAGE_REGEX = Regex("\\S+.kt:\\d+:\\d+: error: .*")
    }
}
