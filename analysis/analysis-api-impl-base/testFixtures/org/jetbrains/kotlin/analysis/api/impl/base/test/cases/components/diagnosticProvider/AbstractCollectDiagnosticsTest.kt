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
 * Checks the output of [KaDiagnosticProvider.collectDiagnostics][org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider.collectDiagnostics]
 * and its consistency with [KaDiagnosticProvider.diagnostics][org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider.diagnostics]
 * on all source files in the test data (in all test modules).
 *
 * @see AbstractElementDiagnosticsTest
 */
abstract class AbstractCollectDiagnosticsTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    private object Directives : SimpleDirectivesContainer() {
        val SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK by stringDirective("Suppress individual diagnostics check for the test")
    }

    /**
     * @param name This is the name of the original test file that will be printed in test results. In the case of dangling files, [name]
     *  might deviate from the [ktFile]'s own name. Because the printed names shouldn't differ between non-dangling and dangling file tests,
     *  we need this separate property.
     *
     *  We cannot change the name of the [ktFile] directly because dangling files for *scripts* need to have a `.kt` extension, not a `.kts`
     *  extension, so their name cannot be equal to the original test file's `.kts` name.
     */
    protected class PreparedFile(val ktFile: KtFile, val name: String)

    protected open fun prepareKtFile(ktFile: KtFile, testServices: TestServices): PreparedFile = PreparedFile(ktFile, ktFile.name)

    override fun doTest(testServices: TestServices) {
        val preparedFiles = testServices.ktTestModuleStructure.mainModules
            .flatMap { it.ktFiles }
            .map { prepareKtFile(it, testServices) }

        doTestByPreparedFiles(preparedFiles, testServices)
    }

    /**
     * [preparedFiles] may contain fake files for dangling module tests.
     */
    protected fun doTestByPreparedFiles(preparedFiles: List<PreparedFile>, testServices: TestServices) {
        val actual = buildString {
            preparedFiles.forEachIndexed { index, preparedFile ->
                val ktFile = preparedFile.ktFile
                analyzeForTest(ktFile) {
                    val diagnosticsFromFile = collectFileDiagnostics(ktFile)
                    printFileDiagnostics(preparedFile, diagnosticsFromFile, preparedFiles.size > 1)
                    if (index != preparedFiles.lastIndex) {
                        appendLine()
                    }
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
                    for (preparedFile in preparedFiles) {
                        val ktFile = preparedFile.ktFile
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

    private fun StringBuilder.printFileDiagnostics(
        preparedFile: PreparedFile,
        diagnostics: List<DiagnosticKey>,
        hasMultipleTestFiles: Boolean,
    ) {
        val heading = if (hasMultipleTestFiles) {
            "Diagnostics from ${preparedFile.name}:"
        } else {
            "Diagnostics from file:"
        }

        appendLine(heading)
        if (diagnostics.isNotEmpty()) {
            for (key in diagnostics) {
                val element = key.psi
                appendLine("  for PSI element of type ${element::class.simpleName} at ${element.getLineColumnRange()}")
                printDiagnosticKey(key, 4)
            }
        } else {
            appendLine("  <NO DIAGNOSTICS>")
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
