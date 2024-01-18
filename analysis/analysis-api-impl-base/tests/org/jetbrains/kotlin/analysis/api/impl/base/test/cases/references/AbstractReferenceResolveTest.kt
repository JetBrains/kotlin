/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.TestReferenceResolveResultRenderer.renderResolvedTo
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.KtPropertyAccessorsRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractReferenceResolveTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
            forTestsMatching("analysis/analysis-api/testData/referenceResolve/kDoc/*") {
                defaultDirectives {
                    +AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE
                    +AnalysisApiTestDirectives.IGNORE_FE10
                }
            }
            forTestsMatching("analysis/analysis-api/testData/referenceResolve/kDoc/qualified/stdlib/*") {
                defaultDirectives {
                    +ConfigurationDirectives.WITH_STDLIB
                }
            }

        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val caretPosition = testServices.expressionMarkerProvider.getCaretPosition(mainFile)
        doTestByFileStructure(mainFile, caretPosition, mainModule, testServices)
    }

    protected fun doTestByFileStructure(ktFile: KtFile, caretPosition: Int, mainModule: TestModule, testServices: TestServices) {
        val ktReferences = findReferencesAtCaret(ktFile, caretPosition)
        if (ktReferences.isEmpty()) {
            testServices.assertions.fail { "No references at caret found" }
        }

        val resolvedTo = analyzeReferenceElement(ktReferences.first().element, mainModule) {
            val symbols = ktReferences.flatMap { it.resolveToSymbols() }
            val renderPsiClassName = Directives.RENDER_PSI_CLASS_NAME in mainModule.directives
            renderResolvedTo(symbols, renderPsiClassName, renderingOptions) { getAdditionalSymbolInfo(it) }
        }

        val actual = "Resolved to:\n$resolvedTo"
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    protected open fun <R> analyzeReferenceElement(element: KtElement, mainModule: TestModule, action: KtAnalysisSession.() -> R): R {
        return analyseForTest(element) { action() }
    }

    open fun KtAnalysisSession.getAdditionalSymbolInfo(symbol: KtSymbol): String? = null

    private fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
        mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_PSI_CLASS_NAME by directive(
            "Render also PSI class name for resolved reference"
        )
    }

    private val renderingOptions = KtDeclarationRendererForDebug.WITH_QUALIFIED_NAMES.with {
        annotationRenderer = annotationRenderer.with {
            annotationFilter = KtRendererAnnotationsFilter.NONE
        }
        propertyAccessorsRenderer = KtPropertyAccessorsRenderer.NONE
    }

}
