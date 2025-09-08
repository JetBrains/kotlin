/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class FirMetadataLoadingTestSuppressor(
    testServices: TestServices,
    private val suppressDirective: SimpleDirective,
) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (testServices.moduleStructure.modules.any { suppressDirective in it.directives }) {
            return if (failedAssertions.isNotEmpty()) {
                emptyList()
            } else {
                listOf(AssertionError("Looks like this test can be unmuted. Remove ${suppressDirective.name} directive").wrap())
            }
        }
        return failedAssertions
    }
}
