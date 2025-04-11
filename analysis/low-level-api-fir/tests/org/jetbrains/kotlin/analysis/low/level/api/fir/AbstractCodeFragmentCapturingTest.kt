/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedValueAnalyzer
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.utils.indented
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCodeFragmentCapturingTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val project = mainFile.project

        val resolutionFacade = mainModule.ktModule.getResolutionFacade(project)
        val firFile = mainFile.getOrBuildFirFile(resolutionFacade)

        val firCodeFragment = firFile.codeFragment
        firCodeFragment.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        val frontendDiagnostics = mainFile.collectDiagnosticsForFile(resolutionFacade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        val frontendErrors = frontendDiagnostics.filter { it.severity == Severity.ERROR }

        require(frontendErrors.isEmpty()) {
            frontendErrors
        }

        val capturedValueData = CodeFragmentCapturedValueAnalyzer.analyze(resolutionFacade, firCodeFragment)


        val actualText = buildString {
            if (capturedValueData.symbols.isNotEmpty()) {
                append(capturedValueData.symbols.joinToString(prefix = "Captured values:\n", separator = "\n") { capturedSymbol ->
                    val firRenderer = FirRenderer(
                        bodyRenderer = null,
                        classMemberRenderer = null,
                        contractRenderer = null,
                        modifierRenderer = null
                    )
                    buildString {
                        append(capturedSymbol.value)
                        appendLine().append(firRenderer.renderElementAsString(capturedSymbol.symbol.fir).indented(4))
                        appendLine().append(capturedSymbol.typeRef.render().indented(4))
                    }
                })
            } else {
                append("No captured values")
            }
            if (capturedValueData.files.isNotEmpty()) {
                append(capturedValueData.files.joinToString(prefix = "\nCaptured files:\n", separator = "\n") { it.name })
            } else {
                appendLine().append("No captured files")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(
            actual = actualText,
            extension = ".capturing.txt"
        )
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }
        }
    }
}