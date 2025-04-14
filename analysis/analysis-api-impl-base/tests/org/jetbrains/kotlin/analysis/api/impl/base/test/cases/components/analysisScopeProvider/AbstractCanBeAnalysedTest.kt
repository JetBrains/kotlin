/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.analysisScopeProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.targets.getTestTargetKtElements
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Checks [canBeAnalysed][org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider.canBeAnalysed] on a specific PSI element in a
 * specific use-site analysis session. The checked element is determined by the caret or a
 * [TestSymbolTarget][org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget] (in case of library elements), while the
 * use-site analysis session is determined by the [Directives.USE_SITE_MODULE] directive.
 */
abstract class AbstractCanBeAnalysedTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    object Directives : SimpleDirectivesContainer() {
        val USE_SITE_MODULE by directive("Determines the module of the use-site analysis session from which `canBeAnalysed` is called.")
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val targetElement =
            testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<KtElement>(testServices).singleOrNull()?.first
                ?: getTestTargetKtElements(testDataPath, mainFile).singleOrNull()
                ?: error("Expected exactly one test target element.")

        val useSiteModule =
            testServices.ktTestModuleStructure.mainModules
                .singleOrNull { Directives.USE_SITE_MODULE in it.testModule.directives }
                ?: error("No use-site module specified. Please add the ${Directives.USE_SITE_MODULE.name} directive to one test module.")

        val actualText = analyze(useSiteModule.ktModule) {
            prettyPrint {
                val result = targetElement.canBeAnalysed()
                appendLine("canBeAnalysed: $result")
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }
}
