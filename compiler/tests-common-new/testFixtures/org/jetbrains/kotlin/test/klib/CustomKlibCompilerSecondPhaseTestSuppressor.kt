/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives.IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_PHASE
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives.IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_PHASE
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.junit.jupiter.api.Assumptions

/**
 * Mute (ignore) tests where the custom compiler failed to compile test data in the second (backend) phase.
 * It's only allowed to mute such tests for a specific version of the custom compiler specified in
 *   [IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE] directive.
 */
class CustomKlibCompilerSecondPhaseTestSuppressor(
    testServices: TestServices,
    private val customCompilerVersion: String,
) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CustomKlibCompilerTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (failedAssertions.isEmpty()) {
            return buildList {
                with(testServices.moduleStructure.modules.first().directives) {
                    addAll(createUnmutingErrorIfNeeded(IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_PHASE))
                    addAll(createUnmutingErrorIfNeeded(IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE))
                    addAll(createUnmutingErrorIfNeeded(IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_PHASE))
                }
            }
        }

        val newFailedAssertions = failedAssertions.flatMap { wrappedException ->
            when (wrappedException) {
                is WrappedException.FromHandler -> when (wrappedException.handler) {
                    is NoFirCompilationErrorsHandler -> emptyList() // Some tests cannot be compiled with previous LV. These are just ignored
                    is JsBinaryArtifactHandler -> processException(  // Execution error
                        wrappedException,
                        IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_PHASE
                    )
                    else -> listOf(wrappedException)
                }
                is WrappedException.FromFacade -> when (wrappedException.facade) {
                    is CustomKlibCompilerSecondPhaseFacade -> processException(
                        wrappedException,
                        IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE
                    )
                    else -> processException(wrappedException, IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_PHASE)
                }
                else -> error("Yet unsupported wrapped exception type: ${wrappedException::class.qualifiedName} ")
            }
        }

        if (newFailedAssertions.isEmpty()) {
            // Explicitly mark the test as "ignored".
            throw Assumptions.abort<Nothing>()
        } else {
            return newFailedAssertions
        }
    }

    context(directives: RegisteredDirectives)
    private fun createUnmutingErrorIfNeeded(stringDirective: StringDirective): List<WrappedException> {
        return if (customCompilerVersion in directives[stringDirective])
            listOf(
                AssertionError(
                    "Looks like this test can be unmuted. Remove $customCompilerVersion from the $stringDirective directive"
                ).wrap()
            )
        else emptyList()
    }

    private fun processException(wrappedException: WrappedException, ignoreDirective: StringDirective): List<WrappedException> {
        val directives = testServices.moduleStructure.modules.first().directives
        for (prefix in directives[ignoreDirective]) {
            if (customCompilerVersion.startsWith(prefix))
                return emptyList()
        }

        return listOf(wrappedException)
    }
}
