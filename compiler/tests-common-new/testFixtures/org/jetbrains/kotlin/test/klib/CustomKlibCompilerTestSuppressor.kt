/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Assumptions

/**
 * A simplified alternative to [BlackBoxCodegenSuppressor] to be used in KLIB-compatibility tests.
 * - Does not support custom ignore directives. Only the standard ones: `IGNORE_BACKEND*`.
 * - Does not offer to unmute the test if it happens to be successful.
 */
class CustomKlibCompilerTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(BlackBoxCodegenSuppressor::SuppressionChecker.bind(null, emptyList())))

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (failedAssertions.isEmpty())
            return emptyList()

        val suppressionChecker = testServices.codegenSuppressionChecker
        val modules = testServices.moduleStructure.modules

        return if (modules.any { suppressionChecker.failuresInModuleAreIgnored(it) }) {
            // Explicitly mark the test as "ignored".
            throw Assumptions.abort<Nothing>()
        } else
            failedAssertions
    }
}
