/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.TestReferenceResolveResultRenderer.renderResolvedTo
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.KaPropertyAccessorsRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.CaretMarker
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveReferenceTest : AbstractResolveTest<KtReference?>() {
    override val resolveKind: String get() = "references"

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
            forTestsMatching("analysis/analysis-api/testData/components/resolver/singleByPsi/kDoc/*") {
                defaultDirectives {
                    +AnalysisApiTestDirectives.DISABLE_DEPENDED_MODE
                    +AnalysisApiTestDirectives.IGNORE_FE10
                }
            }
            forTestsMatching("analysis/analysis-api/testData/components/resolver/singleByPsi/kDoc/qualified/stdlib/*") {
                defaultDirectives {
                    +ConfigurationDirectives.WITH_STDLIB
                }
            }
        }
    }

    override fun collectElementsToResolve(
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<KtReference?>> {
        val caretPositions = testServices.expressionMarkerProvider.getAllCarets(mainFile).ifEmpty {
            testServices.expressionMarkerProvider.getSelectedRangeOrNull(mainFile)?.let {
                CaretMarker(tag = "", offset = it.startOffset)
            }.let(::listOfNotNull)
        }

        return collectElementsToResolve(caretPositions, mainFile)
    }

    protected fun collectElementsToResolve(
        carets: List<CaretMarker>,
        file: KtFile,
    ): Collection<ResolveTestCaseContext<KtReference?>> = carets.flatMap<CaretMarker, ResolveTestCaseContext<KtReference?>> { caret ->
        val marker = caret.fullTag
        val contexts: List<ResolveTestCaseContext<KtReference?>> = findReferencesAtCaret(file, caret.offset).map { reference ->
            ResolveTestCaseContext(element = reference, context = reference.element, marker = marker)
        }

        contexts.ifEmpty {
            listOf(ResolveTestCaseContext<KtReference?>(element = null, context = null, marker = marker))
        }
    }

    override fun generateResolveOutput(
        context: ResolveTestCaseContext<KtReference?>,
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ): String {
        val reference = context.element ?: return "no references found"

        return analyzeReferenceElement(reference.element, mainModule) {
            val symbols = reference.resolveToSymbols()
            val symbolsAgain = reference.resolveToSymbols()
            testServices.assertions.assertEquals(symbols, symbolsAgain)

            val renderPsiClassName = Directives.RENDER_PSI_CLASS_NAME in mainModule.testModule.directives
            renderResolvedTo(symbols, renderPsiClassName, renderingOptions) { getAdditionalSymbolInfo(it) }
        }
    }

    protected open fun <R> analyzeReferenceElement(element: KtElement, mainModule: KtTestModule, action: KaSession.() -> R): R {
        return analyseForTest(element) { action() }
    }

    open fun KaSession.getAdditionalSymbolInfo(symbol: KaSymbol): String? = null

    private fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
        mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_PSI_CLASS_NAME by directive(
            "Render also PSI class name for resolved reference"
        )
    }

    private val renderingOptions = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES.with {
        annotationRenderer = annotationRenderer.with {
            annotationFilter = KaRendererAnnotationsFilter.NONE
        }
        propertyAccessorsRenderer = KaPropertyAccessorsRenderer.NONE
    }

}
