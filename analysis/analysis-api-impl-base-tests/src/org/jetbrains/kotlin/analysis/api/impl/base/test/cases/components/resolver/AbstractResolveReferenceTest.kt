/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceService
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.TestReferenceResolveResultRenderer.renderResolvedTo
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.KaDeclarationModifiersRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KaModifierListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KaDeclarationNameRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.KaTypeParameterRendererFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.KaPropertyAccessorsRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.CaretMarker
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfTypeInPreorder
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
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<KtReference?>> {
        val caretPositions = testServices.expressionMarkerProvider.getAllCarets(file).ifEmpty {
            testServices.expressionMarkerProvider.getSelectedRangeOrNull(file)?.let {
                CaretMarker(tag = "", offset = it.startOffset)
            }.let(::listOfNotNull)
        }

        return collectElementsToResolve(caretPositions, file)
    }

    protected fun collectElementsToResolve(
        carets: List<CaretMarker>,
        file: KtFile,
    ): Collection<ResolveTestCaseContext<KtReference?>> = carets.flatMap<CaretMarker, ResolveTestCaseContext<KtReference?>> { caret ->
        val marker = caret.fullTag
        val contexts: List<ResolveTestCaseContext<KtReference?>> = findReferencesAtCaret(file, caret.offset).map { reference ->
            ResolveReferenceTestCaseContext(element = reference, marker = marker)
        }

        contexts.ifEmpty {
            listOf(ResolveReferenceTestCaseContext(element = null, marker = marker))
        }
    }

    protected fun collectAllReferences(file: KtFile): Collection<ResolveReferenceTestCaseContext> = buildSet {
        val referenceService = PsiReferenceService.getService()
        file.forEachDescendantOfTypeInPreorder<PsiElement> { element ->
            for (reference in referenceService.getContributedReferences(element)) {
                if (reference !is KtReference) continue
                val context = ResolveReferenceTestCaseContext(element = reference, marker = null)
                add(context)
            }
        }
    }

    class ResolveReferenceTestCaseContext(
        override val element: KtReference?,
        override val marker: String?,
    ) : ResolveTestCaseContext<KtReference?> {
        override val context: KtElement? get() = element?.element
    }

    override fun generateResolveOutput(
        context: ResolveTestCaseContext<KtReference?>,
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): String {
        val reference = context.element ?: return "no references found"

        return analyzeReferenceElement(reference.element, module) {
            val symbols = reference.resolveToSymbols()
            val symbolsAgain = reference.resolveToSymbols()
            testServices.assertions.assertEquals(symbols, symbolsAgain)

            val renderPsiClassName = Directives.RENDER_PSI_CLASS_NAME in module.testModule.directives
            val options = createRenderingOptions(renderPsiClassName)
            renderResolvedTo(symbols, options) { getAdditionalSymbolInfo(it) }
        }
    }

    private fun createRenderingOptions(renderPsiClassName: Boolean): KaDeclarationRenderer {
        if (!renderPsiClassName) return defaultRenderingOptions
        return defaultRenderingOptions.with {

            modifiersRenderer = modifiersRenderer.with {
                val delegateModifierListRenderer = modifierListRenderer
                modifierListRenderer = object : KaModifierListRenderer {
                    override fun renderModifiers(
                        analysisSession: KaSession,
                        symbol: KaDeclarationSymbol,
                        declarationModifiersRenderer: KaDeclarationModifiersRenderer,
                        printer: PrettyPrinter,
                    ) {
                        printer {
                            append("{psi: ${symbol.psi?.let { it::class.simpleName }}}")
                        }
                        delegateModifierListRenderer.renderModifiers(analysisSession, symbol, declarationModifiersRenderer, printer)
                    }
                }
            }
        }
    }


    protected open fun <R> analyzeReferenceElement(element: KtElement, module: KtTestModule, action: KaSession.() -> R): R {
        return analyseForTest(element) { action() }
    }

    open fun KaSession.getAdditionalSymbolInfo(symbol: KaSymbol): String? = null

    private fun findReferencesAtCaret(file: KtFile, caretPosition: Int): List<KtReference> =
        file.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_PSI_CLASS_NAME by directive(
            "Render also PSI class name for resolved reference"
        )
    }

    private val defaultRenderingOptions = KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES.with {
        annotationRenderer = annotationRenderer.with {
            annotationFilter = KaRendererAnnotationsFilter.NONE
        }
        propertyAccessorsRenderer = KaPropertyAccessorsRenderer.NONE
        typeParametersFilter = KaTypeParameterRendererFilter { _, _ -> true }
    }

}
