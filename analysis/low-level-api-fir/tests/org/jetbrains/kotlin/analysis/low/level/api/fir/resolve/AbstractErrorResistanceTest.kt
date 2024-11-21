/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithClearCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.ErrorResistanceServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractErrorResistanceTest : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiFirSourceTestConfigurator = ErrorResistanceConfigurator

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        resolveWithClearCaches(mainFile) { firResolveSession ->
            ErrorResistanceServiceRegistrar.handleInterruption {
                mainFile.collectDiagnosticsForFile(firResolveSession, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
            }

            val diagnostics = mainFile.collectDiagnosticsForFile(firResolveSession, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
            assert(diagnostics.isEmpty()) {
                val messages = diagnostics.map { it.factoryName }
                "There should be no diagnostics, found:\n" + messages.joinToString("\n")
            }
        }
    }
}

internal object ErrorResistanceConfigurator : AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false) {
    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = buildList {
            addAll(super.serviceRegistrars)
            add(ErrorResistanceServiceRegistrar)
        }
}