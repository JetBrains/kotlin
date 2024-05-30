/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.TestReferenceResolveResultRenderer.renderResolvedTo
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaRendererAnnotationsFilter
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables.KaPropertyAccessorsRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.CaretMarker
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractReferenceResolveTest : AbstractAnalysisApiBasedTest() {
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

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val caretPositions = testServices.expressionMarkerProvider.getAllCarets(mainFile).ifEmpty {
            testServices.expressionMarkerProvider.getSelectedRangeOrNull(mainFile)?.let {
                CaretMarker(tag = "from_expression", offset = it.startOffset)
            }.let(::listOfNotNull)
        }

        doTestByFileStructure(mainFile, caretPositions, mainModule, testServices)
    }

    protected fun doTestByFileStructure(ktFile: KtFile, carets: List<CaretMarker>, mainModule: KtTestModule, testServices: TestServices) {
        if (carets.isEmpty()) {
            testServices.assertions.fail { "No carets were specified for resolve test" }
        }

        val resolutionAtPositions = carets.map { caret ->
            caret to renderResolvedReferencesForCaretPosition(ktFile, caret, mainModule, testServices)
        }

        val actual = if (resolutionAtPositions.size == 1) {
            val (_, singleResolutionResult) = resolutionAtPositions.single()
            singleResolutionResult
        } else {
            prettyPrint {
                printCollection(resolutionAtPositions, separator = "\n\n") { (caret, referencesAtCaret) ->
                    append(caret.fullTag)
                    appendLine(':')
                    withIndent {
                        append(referencesAtCaret)
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = "references.txt")
    }

    private fun renderResolvedReferencesForCaretPosition(
        ktFile: KtFile,
        caret: CaretMarker,
        mainModule: KtTestModule,
        testServices: TestServices,
    ): String {
        val ktReferences = findReferencesAtCaret(ktFile, caret.offset)
        if (ktReferences.isEmpty()) {
            return "no references found"
        }

        return prettyPrint {
            analyzeReferenceElement(ktReferences.first().element, mainModule) {
                printCollection(ktReferences, separator = "\n\n") { reference ->
                    append(renderCommonClassName(reference))
                    append(':')

                    val symbols = reference.resolveToSymbols()
                    val symbolsAgain = reference.resolveToSymbols()
                    testServices.assertions.assertEquals(symbols, symbolsAgain)

                    val renderPsiClassName = Directives.RENDER_PSI_CLASS_NAME in mainModule.testModule.directives
                    val symbolsAsText = renderResolvedTo(symbols, renderPsiClassName, renderingOptions) { getAdditionalSymbolInfo(it) }
                    if (symbols.isEmpty()) {
                        append(' ')
                        append(symbolsAsText)
                    } else {
                        appendLine()
                        withIndent {
                            append(symbolsAsText)
                        }
                    }
                }
            }
        }
    }

    private fun renderCommonClassName(instance: Any): String {
        var classToRender: Class<*> = instance::class.java
        while (classToRender.simpleName.let { it.contains("Fir") || it.contains("Fe10") } == true) {
            classToRender = classToRender.superclass
        }

        return classToRender.simpleName
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
