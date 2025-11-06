/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestDirectives.IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE
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
        if (failedAssertions.isEmpty())
            return emptyList()

        val newFailedAssertions = failedAssertions.flatMap { wrappedException ->
            when (wrappedException) {
                is WrappedException.FromHandler -> processHandlerException(wrappedException)
                is WrappedException.FromFacade -> processFacadeException(wrappedException)
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

    private fun processHandlerException(wrappedException: WrappedException.FromHandler): List<WrappedException> =
        when (wrappedException.handler) {
            is NoFirCompilationErrorsHandler -> emptyList() // Some tests cannot be compiled with previous LV. These are just ignored
            is JsBinaryArtifactHandler -> processSecondPhaseException(wrappedException) // Execution error
            else -> listOf(wrappedException)
        }

    private fun processFacadeException(wrappedException: WrappedException.FromFacade): List<WrappedException> =
        when (wrappedException.facade) {
            is CustomKlibCompilerSecondPhaseFacade -> processSecondPhaseException(wrappedException)
            else -> listOf(wrappedException)
        }

    private fun processSecondPhaseException(wrappedException: WrappedException): List<WrappedException> {
        val directives = testServices.moduleStructure.modules.first().directives
        if (customCompilerVersion == "2.2.0") { // KT-76131 TODO: Drop this clause after master will have LV 2.4.0 and forward compatibility testing would be against 2.3.0
            // Ideally, forward compatibility tests must be compiled against stdlib of previous Kotlin version, so `assertFails` and `assertFailsWith` would be resolved against old stdlib
            // However, unfortunately, KT-82217 prevents now from using old klib for forward compatibility tests.
            // So, using latest stdlib causes `assertFails` and `assertFailsWith` be resolved to the signatures that weren't in old stdlib, and such test fails.
            val wrappedExceptionText = wrappedException.cause.toString()
            if (wrappedExceptionText.contains(messageAssertFails) ||
                wrappedExceptionText.contains(messageAssertFailsWith)
            ) return emptyList()
        }
        if (IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE !in directives)
            return listOf(wrappedException)

        for (prefix in directives[IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE]) {
            if (customCompilerVersion.startsWith(prefix))
                return emptyList()
        }

        return listOf(
            wrappedException,
            AssertionError(
                "Looks like this test can be unmuted. Remove $customCompilerVersion from the $IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE directive"
            ).wrap()
        )
    }

    companion object {
        // KT-79094: kotlin.test.assertFails has changed its signature in 2.3.0-Beta1
        private val messageAssertFails = "IrLinkageError: Function 'assertFails' can not be called: No function found for symbol"

        // KT-79094: kotlin.test.assertFailsWith has changed its signature in 2.3.0-Beta1
        private val messageAssertFailsWith = "IrLinkageError: Function 'assertFailsWith' can not be called: No function found for symbol"
    }
}
