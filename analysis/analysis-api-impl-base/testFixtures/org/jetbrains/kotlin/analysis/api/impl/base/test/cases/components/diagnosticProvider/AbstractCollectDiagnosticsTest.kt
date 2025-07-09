/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
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
        val MULTI_FILE_DIAGNOSTICS_TEST by stringDirective(
            "Ensures that diagnostics are checked for all files across all test modules, disregarding the main file and main module."
        )
    }

    open fun prepareKtFile(ktFile: KtFile, testServices: TestServices): KtFile = ktFile

    override fun doTest(testServices: TestServices) {
        if (testServices.moduleStructure.allDirectives.contains(Directives.MULTI_FILE_DIAGNOSTICS_TEST)) {
            doMultiFileTest(testServices)
        } else {
            val (mainFile, _) = findMainFileAndModule(testServices)
            if (mainFile != null) {
                doMainFileTest(mainFile, testServices)
            } else {
                error(
                    "Cannot find the main file. To enable multi-file tests, use the '${Directives.MULTI_FILE_DIAGNOSTICS_TEST.name}'" +
                            " directive."
                )
            }
        }
    }

    private fun doMainFileTest(mainFile: KtFile, testServices: TestServices) {
        doTestByKtFiles(listOf(prepareKtFile(mainFile, testServices)), testServices)
    }

    private fun doMultiFileTest(testServices: TestServices) {
        val ktFiles = testServices.ktTestModuleStructure.mainModules
            .flatMap { it.ktFiles }
            .map { prepareKtFile(it, testServices) }

        doTestByKtFiles(ktFiles, testServices)
    }

    /**
     * [ktFiles] may contain fake files for dangling module tests.
     */
    protected fun doTestByKtFiles(ktFiles: List<KtFile>, testServices: TestServices) {
        val actual = buildString {
            for (ktFile in ktFiles) {
                analyzeForTest(ktFile) {
                    val diagnosticsFromFile = collectFileDiagnostics(ktFile)
                    printFileDiagnostics(ktFile, diagnosticsFromFile, ktFiles.size > 1)
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)

        if (configurator.frontendKind == FrontendKind.Fir) {
            // The suppression has to be applied once for all files. If we check the suppression per file, some checks will not fail,
            // and fail the test with a message that the suppression is not needed.
            testServices.moduleStructure.allDirectives.suppressIf(
                suppressionDirective = Directives.SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK,
                filter = { it is AssertionError },
                action = {
                    for (ktFile in ktFiles) {
                        analyzeForTest(ktFile) {
                            val diagnosticsFromFile = collectFileDiagnostics(ktFile)
                            checkDiagnosticsFromElements(ktFile, diagnosticsFromFile)
                        }
                    }
                }
            )
        }
    }

    private fun KaSession.collectFileDiagnostics(ktFile: KtFile): List<DiagnosticKey> =
        ktFile
            .collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            .map { it.getDiagnosticKey() }
            .sorted()

    private fun StringBuilder.printFileDiagnostics(ktFile: KtFile, diagnostics: List<DiagnosticKey>, hasMultipleTestFiles: Boolean) {
        val heading = if (hasMultipleTestFiles) {
            "Diagnostics from ${ktFile.originalFile.name}:"
        } else {
            "Diagnostics from file:"
        }

        appendLine(heading)
        for (key in diagnostics) {
            val element = key.psi
            appendLine("  for PSI element of type ${element::class.simpleName} at ${element.getLineColumnRange()}")
            printDiagnosticKey(key, 4)
        }
    }

    private fun StringBuilder.printDiagnosticKey(key: DiagnosticKey, indent: Int) {
        val indentString = " ".repeat(indent)
        append(indentString + key.factoryName)
        appendLine("$indentString  text ranges: ${key.textRanges}")
        appendLine("$indentString  PSI: ${key.psi::class.simpleName} at ${key.psi.getLineColumnRange()}")
    }

    private fun KaSession.checkDiagnosticsFromElements(ktFile: KtFile, diagnosticsFromFile: List<DiagnosticKey>) {
        val diagnosticsFromElements = buildList {
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    element
                        .diagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                        .mapTo(this@buildList) { it.getDiagnosticKey() }

                    super.visitKtElement(element)
                }
            })
        }.sorted()

        assertEquals(
            diagnosticsFromFile,
            diagnosticsFromElements,
            "diagnostics collected from files should be the same as those collected from individual PSI elements."
        )
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

    private fun KaDiagnosticWithPsi<*>.getDiagnosticKey() = DiagnosticKey(factoryName, psi, textRanges)

    private fun PsiElement.getLineColumnRange(): String = getLineAndColumnRangeInPsiFile(containingFile, textRange).toString()
}
