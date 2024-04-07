/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CodeFragmentCapturedValueAnalyzer
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.codeFragment
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
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

        val resolveSession = mainModule.ktModule.getFirResolveSession(project)
        val firFile = mainFile.getOrBuildFirFile(resolveSession)

        val firCodeFragment = firFile.codeFragment
        firCodeFragment.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        val frontendDiagnostics = mainFile.collectDiagnosticsForFile(resolveSession, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        val frontendErrors = frontendDiagnostics.filter { it.severity == Severity.ERROR }

        require(frontendErrors.isEmpty()) {
            frontendErrors
        }

        val capturedSymbols = CodeFragmentCapturedValueAnalyzer.analyze(resolveSession, firCodeFragment).symbols

        val actualText = capturedSymbols.joinToString("\n") { capturedSymbol ->
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
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(
            actual = actualText.ifEmpty { "No captured values" },
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