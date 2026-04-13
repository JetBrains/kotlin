/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.diagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.ErrorResistanceServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.withResolutionFacade
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractErrorResistanceTest : AbstractAnalysisApiBasedTest() {
    override val configurator = LLSourceLikeTestConfigurator()

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + listOf(ErrorResistanceServiceRegistrar)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        withResolutionFacade(mainFile) { resolutionFacade ->
            ErrorResistanceServiceRegistrar.handleInterruption {
                mainFile.diagnostics(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
                    .count()
            }

            val diagnostics = mainFile.diagnostics(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS).toList()
            assert(diagnostics.isEmpty()) {
                val messages = diagnostics.map { it.factoryName }
                "There should be no diagnostics, found:\n" + messages.joinToString("\n")
            }
        }
    }
}
