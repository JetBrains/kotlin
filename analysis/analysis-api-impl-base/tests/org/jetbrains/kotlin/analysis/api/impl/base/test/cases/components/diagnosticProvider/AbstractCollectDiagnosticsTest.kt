/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnRangeInPsiFile
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils.offsetToLineAndColumn
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.test.assertEquals

/**
 * Checks the output of [org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider.collectDiagnostics]
 * and its consistency with [org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider.diagnostics].
 *
 * @see AbstractElementDiagnosticsTest
 */
abstract class AbstractCollectDiagnosticsTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    private object Directives : SimpleDirectivesContainer() {
        val SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK by stringDirective("Suppress individual diagnostics check for the test")
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        doTestByKtFile(mainFile, testServices)
    }

    /**
     * [ktFile] may be a fake file for dangling module tests.
     */
    protected fun doTestByKtFile(ktFile: KtFile, testServices: TestServices) {
        fun TextRange.asLineColumnRange(): String {
            return getLineAndColumnRangeInPsiFile(ktFile, this).toString()
        }

        analyzeForTest(ktFile) {
            val diagnosticsInFile = ktFile.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS).map {
                it.getKey()
            }.sorted()

            val actual = buildString {
                fun DiagnosticKey.print(indent: Int) {
                    val indentString = " ".repeat(indent)
                    append(indentString + factoryName)
                    appendLine("$indentString  text ranges: $textRanges")
                    appendLine("$indentString  PSI: ${psi::class.simpleName} at ${psi.textRange.asLineColumnRange()}")
                }

                appendLine("Diagnostics from file:")
                for (key in diagnosticsInFile) {
                    val element = key.psi
                    appendLine("  for PSI element of type ${element::class.simpleName} at ${element.textRange.asLineColumnRange()}")
                    key.print(4)
                }
            }

            testServices.assertions.assertEqualsToTestOutputFile(actual)

            val diagnosticsFromElements = buildList {
                ktFile.accept(object : KtTreeVisitorVoid() {
                    override fun visitKtElement(element: KtElement) {
                        element.diagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS).mapTo(this@buildList) { it.getKey() }

                        super.visitKtElement(element)
                    }
                })
            }.sorted()

            if (configurator.frontendKind == FrontendKind.Fir) {
                testServices.moduleStructure.allDirectives.suppressIf(
                    suppressionDirective = Directives.SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK,
                    filter = { it is AssertionError },
                    action = {
                        assertEquals(
                            diagnosticsInFile,
                            diagnosticsFromElements,
                            "diagnostics collected from file should be the same as those collected from individual PSI elements."
                        )
                    },
                )
            }
        }
    }

    private data class DiagnosticKey(
        val factoryName: String?,
        val psi: PsiElement,
        val textRanges: Collection<TextRange>,
    ) : Comparable<DiagnosticKey> {
        override fun toString(): String {
            val document = psi.containingFile.viewProvider.document
            return "$factoryName on ${psi::class.simpleName} at ${offsetToLineAndColumn(document, psi.startOffset)})"
        }

        override fun compareTo(other: DiagnosticKey): Int = this.toString().compareTo(other.toString())
    }

    private fun KaDiagnosticWithPsi<*>.getKey() = DiagnosticKey(factoryName, psi, textRanges)
}
