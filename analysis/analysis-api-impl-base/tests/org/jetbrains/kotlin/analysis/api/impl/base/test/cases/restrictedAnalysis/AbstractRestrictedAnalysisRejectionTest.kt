/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.restrictedAnalysis

import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Checks that the Analysis API correctly rejects analysis based on different permutations of settings in [KotlinRestrictedAnalysisService][org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis.KotlinRestrictedAnalysisService].
 */
abstract class AbstractRestrictedAnalysisRejectionTest : AbstractRestrictedAnalysisTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val isRestricted = mainModule.testModule.directives[Directives.RESTRICTED].singleOrNull().toBoolean()
        val isAllowed = mainModule.testModule.directives[Directives.ALLOWED].singleOrNull().toBoolean()

        val restrictedService = mainModule.restrictedAnalysisService
        restrictedService.enableRestrictedAnalysisMode = isRestricted
        restrictedService.allowRestrictedAnalysis = isAllowed

        val rejectionExpected = mainModule.testModule.directives.contains(Directives.EXPECT_REJECTION)
        var exceptionOccurred = false
        try {
            analyseForTest(mainFile) {
                // Nothing to do here.
            }
        } catch (_: SwitchableRestrictedAnalysisService.RestrictedAnalysisNotAllowedException) {
            exceptionOccurred = true
        }

        testServices.assertions.assertEquals(rejectionExpected, exceptionOccurred) {
            "Expected analysis to be rejected: $rejectionExpected\nAnalysis was rejected: $exceptionOccurred"
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val RESTRICTED by stringDirective("Whether restricted analysis mode is enabled.")
        val ALLOWED by stringDirective("Whether restricted analysis is allowed.")
        val EXPECT_REJECTION by directive("Whether analysis should be rejected.")
    }
}
