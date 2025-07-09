/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.junit.jupiter.api.Assumptions

/**
 * Mute (ignore) tests where:
 * - The custom compiler failed to compile test data in the first (frontend) phase.
 * - The custom compiler successfully produced a KLIB artifact, which is known to have
 *   a problem that cause the second phase (backend) to crash. It's only allowed to mute
 *   such tests for a specific version of the custom compiler specified in
 *   [IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE] directive.
 */
class CustomKlibCompilerFirstPhaseTestSuppressor(
    testServices: TestServices,
    private val customCompilerVersion: String,
) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CustomKlibCompilerTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (failedAssertions.isEmpty())
            return emptyList()

        val newFailedAssertions = failedAssertions.asSequence().flatMap { wrappedException ->
            if (wrappedException is WrappedException.FromFacade) {
                if (wrappedException.facade is CustomKlibCompilerFirstPhaseFacade) {
                    // The test failed on the first phase.
                    processFirstPhaseException(wrappedException)
                } else {
                    // The test failed not on the first phase.
                    processNonFirstPhaseException(wrappedException)
                }
            } else if (wrappedException is WrappedException.FromHandler) {
                // The test failed on a handler.
                processNonFirstPhaseException(wrappedException)
            } else {
                listOf(wrappedException)
            }
        }.toList()

        if (newFailedAssertions.isEmpty()) {
            // Explicitly mark the test as "ignored".
            throw Assumptions.abort<Nothing>()
        } else {
            return newFailedAssertions
        }
    }

    private fun processFirstPhaseException(wrappedException: WrappedException.FromFacade): List<WrappedException> {
        val (exitCode, compilerOutput) = wrappedException.cause as? CustomKlibCompilerException
            ?: return listOf(wrappedException)

        return when (exitCode) {
            ExitCode.COMPILATION_ERROR -> {
                // Make sure that the compilation failure really looks like a frontend error. Otherwise, fail the test.
                if (!FRONTEND_ERROR_MESSAGE_REGEX.containsMatchIn(compilerOutput)) {
                    listOf(
                        wrappedException,
                        AssertionError(
                            """
                                        Custom KLIB compiler failed to compile test data on the first (frontend) phase, but the failure does not look like a frontend error.
                                        Please check the compiler output and update the test accordingly.
                                        Compiler output: $compilerOutput
                                    """.trimIndent()
                        ).wrap()
                    )
                } else {
                    emptyList()
                }
            }
            ExitCode.INTERNAL_ERROR -> {
                // Make sure that the compilation failure really looks like an exception in the compiler. Otherwise, fail the test.
                if (!COMPILER_EXCEPTION_MESSAGE_REGEX.containsMatchIn(compilerOutput)) {
                    listOf(
                        wrappedException,
                        AssertionError(
                            """
                                        Custom KLIB compiler failed to compile test data on the first (frontend) phase, but the failure does not look like a compiler exception.
                                        Please check the compiler output and update the test accordingly.
                                        Compiler output: $compilerOutput
                                    """.trimIndent()
                        ).wrap()
                    )
                } else {
                    emptyList()
                }
            }
            else -> listOf(wrappedException)
        }
    }

    private fun processNonFirstPhaseException(wrappedException: WrappedException): List<WrappedException> {
        val directives = testServices.moduleStructure.modules.first().directives
        if (IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE !in directives)
            return listOf(wrappedException)

        for (prefix in directives[IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE]) {
            if (customCompilerVersion.startsWith(prefix))
                return emptyList()
        }

        return listOf(
            wrappedException,
            AssertionError(
                "Looks like this test can be unmuted. Remove $customCompilerVersion from the $IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE directive"
            ).wrap()
        )
    }

    companion object {
        private val FRONTEND_ERROR_MESSAGE_REGEX = Regex("\\S+.kt:\\d+:\\d+: error: .*")
        private val COMPILER_EXCEPTION_MESSAGE_REGEX = Regex("exception: (org\\.jetbrains\\.kotlin\\..*|java\\..*Exception):")
    }
}
