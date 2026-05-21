/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.relationProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.targets.getTestTargetSymbols
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.junit.AssumptionViolatedException

abstract class AbstractGetExpectsForActualTest : AbstractAnalysisApiBasedTest() {
    protected fun KaSession.performExpectCheck(symbol: KaDeclarationSymbol, testServices: TestServices) {
        val expectedSymbols = if (symbol is KaCallableSymbol) {
            // For callable symbols, exercise the endpoint also for their receiver parameter,
            // since it is kind of a special case and not a `KtDeclaration` itself.
            symbol.receiverParameter?.getExpectsForActual().orEmpty() + symbol.getExpectsForActual()
        } else {
            symbol.getExpectsForActual()
        }

        val actualText = buildString {
            appendLine("expected symbols:")
            expectedSymbols.joinTo(this, separator = "\n") { expectedSymbol ->
                val prefix = if (expectedSymbol is KaReceiverParameterSymbol) "receiver parameter " else ""
                prefix + expectedSymbol.render(KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES)
            }
            appendLine()
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }
}

abstract class AbstractGetExpectsForActualByCoordinatesTest : AbstractGetExpectsForActualTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doTest(testServices: TestServices) {
        if (Directives.DISABLE_COORDINATE_TEST in testServices.moduleStructure.allDirectives) {
            throw AssumptionViolatedException("The test is disabled with ${Directives.DISABLE_COORDINATE_TEST::name}")
        }

        val moduleStructure = testServices.ktTestModuleStructure
        val implementationModule = moduleStructure.mainModules
            .map { it.ktModule }
            .single { !it.targetPlatform.isCommon() }

        analyze(implementationModule) {
            val symbols = getTestTargetSymbols(testDataPath, contextFile = null)
                .filterIsInstanceAnd<KaDeclarationSymbol> { !it.containingModule.targetPlatform.isCommon() }

            val symbol = when {
                symbols.isEmpty() -> error("Symbol isn't find by specified coordinates")
                symbols.size > 1 -> {
                    val renderer = KaDebugRenderer()
                    val errorMessage = buildString {
                        append("Multiple symbols found:")
                        for (symbol in symbols) {
                            appendLine().append(renderer.render(useSiteSession, symbol))
                        }
                    }
                    error(errorMessage)
                }
                else -> symbols.first()
            }

            performExpectCheck(symbol, testServices)
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        /** Used for tests for declarations that aren't representable by a
         * [org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget].
         */
        val DISABLE_COORDINATE_TEST by directive(
            "If provided, the coordinate test is disabled",
            applicability = DirectiveApplicability.Global
        )
    }
}

abstract class AbstractGetExpectsForActualByMarkerTest : AbstractGetExpectsForActualTest() {
    @OptIn(KtExperimentalApi::class)
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(mainFile) {
                val expressionMarkerProvider = testServices.expressionMarkerProvider

                val symbol: KaDeclarationSymbol

                val referenceExpression = expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtExpression>(mainFile)
                if (referenceExpression != null) {
                    check(referenceExpression is KtResolvable) { "Resolvable expression expected, got ${referenceExpression::class}" }
                    val resolvedSymbol = referenceExpression.resolveSymbol() ?: error("Reference expression cannot be resolved")
                    check(resolvedSymbol is KaDeclarationSymbol) { "Expected declaration symbol, got ${resolvedSymbol::class}" }
                    symbol = resolvedSymbol
                } else {
                    symbol = expressionMarkerProvider.getTopmostSelectedElementOfType<KtDeclaration>(mainFile).symbol
                }

                performExpectCheck(symbol, testServices)
            }
        }
    }
}
