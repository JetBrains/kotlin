/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.extractIgnoredDirectivesForTargetBackend
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class FirMetadataLoadingTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val moduleStructure = testServices.moduleStructure
        val directive = when (testServices.defaultsProvider.defaultFrontend) {
            FrontendKinds.ClassicFrontend -> CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K1
            FrontendKinds.FIR -> CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K2
            else -> shouldNotBeCalled()
        }
        if (moduleStructure.modules.any { directive in it.directives }) {
            return if (failedAssertions.isNotEmpty()) {
                emptyList()
            } else {
                listOf(AssertionError("Looks like this test can be unmuted. Remove ${directive.name} directive").wrap())
            }
        }
        return failedAssertions
    }
}
