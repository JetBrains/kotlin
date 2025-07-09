/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Assumptions

/**
 * Mute (ignore) tests where the custom KLIB compiler failed to compile test data in the first (frontend) phase.
 */
class CustomKlibCompilerFirstPhaseTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val hasFailuresDueToCustomCompilerAtFirstPhase = failedAssertions.asSequence()
            .filter { wrappedException -> wrappedException is WrappedException.FromFacade && wrappedException.facade is CustomKlibCompilerFirstPhaseFacade }
            .mapNotNull { wrappedException -> wrappedException.cause as? CustomKlibCompilerException }
            .any { unwrappedException -> unwrappedException.exitCode == ExitCode.COMPILATION_ERROR }

        // Explicitly mark the test as "ignored" if the test data cannot be compiled with an older (custom) compiler version.
        Assumptions.assumeFalse(hasFailuresDueToCustomCompilerAtFirstPhase)

        return failedAssertions
    }
}
