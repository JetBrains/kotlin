/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_ERRORS
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicFrontendAnalysisHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class NoCompilationErrorsHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(
    testServices
) {
    override val failureDisablesNextSteps: Boolean = true
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        if (IGNORE_ERRORS in module.directives) return
        AnalyzingUtils.throwExceptionOnErrors(info.analysisResult.bindingContext)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
