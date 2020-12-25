/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicFrontendAnalysisHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class NoCompilationErrorsHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(testServices) {
    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        AnalyzingUtils.throwExceptionOnErrors(info.analysisResult.bindingContext)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
