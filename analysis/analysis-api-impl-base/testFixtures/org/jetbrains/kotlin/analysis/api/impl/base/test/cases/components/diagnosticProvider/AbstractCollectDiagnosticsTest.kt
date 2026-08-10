/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.components.collectDiagnostics
import org.jetbrains.kotlin.analysis.api.diagnostics.*
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnRangeInPsiFile
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils.offsetToLineAndColumn
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the output of [KaDiagnosticProvider.collectDiagnostics][org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider.collectDiagnostics]
 * and its consistency with the [KaDiagnostics] query on all source files in the test data (in all test modules).
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
                    val ignoringSuppression = collectFileDiagnosticsIgnoringSuppression(ktFile)
                    checkDiagnosticsIgnoringSuppressionSupersetOfRegular(
                        withoutSuppression = diagnosticsFromFile,
                        withSuppression = ignoringSuppression,
                    )

                    val additionallyReported = ignoringSuppression.filter { it !in diagnosticsFromFile }
                    if (additionallyReported.isNotEmpty()) {
                        printFileDiagnostics(preparedFile, additionallyReported, preparedFiles.size > 1, ignoringSuppression = true)
                    }

                    if (index != preparedFiles.lastIndex) {
                        appendLine()
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)

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
                        checkDiagnosticsConsistentWithObsoleteApi(ktFile, diagnosticsFromFile)
                        checkDiagnosticsFromElements(ktFile, diagnosticsFromFile)
                        checkSubtreeDiagnostics(ktFile)
                        checkSuppressionMarks(ktFile, diagnosticsFromFile)
                        checkQuerySemantics(ktFile)
                    }
                }
            }
        )
    }

    context(_: KaSession)
    private fun collectFileDiagnostics(ktFile: KtFile): List<DiagnosticKey> =
        ktFile
            .diagnostics()
            .withCommonAndExtendedDiagnostics()
            .map { it.getDiagnosticKey() }
            .sorted()
            .toList()

    context(_: KaSession)
    private fun collectFileDiagnosticsIgnoringSuppression(ktFile: KtFile): List<DiagnosticKey> =
        ktFile
            .diagnostics()
            .withCommonAndExtendedDiagnostics()
            .ignoreSuppressed(false)
            .map { it.getDiagnosticKey() }
            .sorted()
            .toList()

    private fun StringBuilder.printFileDiagnostics(
        preparedFile: PreparedFile,
        diagnostics: List<DiagnosticKey>,
        hasMultipleTestFiles: Boolean,
        ignoringSuppression: Boolean = false,
    ) {
        val heading = when {
            ignoringSuppression -> "Additionally reported when ignoring suppression:"
            hasMultipleTestFiles -> "Diagnostics from ${preparedFile.name}:"
            else -> "Diagnostics from file:"
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

    context(_: KaSession)
    private fun checkDiagnosticsFromElements(ktFile: KtFile, diagnosticsFromFile: List<DiagnosticKey>) {
        val diagnosticsFromElements = buildList {
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    element
                        .diagnostics()
                        .directOnly(true)
                        .withCommonAndExtendedDiagnostics()
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

    context(_: KaSession)
    private fun checkDiagnosticsIgnoringSuppressionSupersetOfRegular(
        withoutSuppression: List<DiagnosticKey>,
        withSuppression: List<DiagnosticKey>,
    ) {
        val missing = withoutSuppression.filter { it !in withSuppression }
        assertTrue(
            missing.isEmpty(),
            "The set of diagnostics with `ignoreSuppressed(true)` must include every diagnostic that `ignoreSuppressed(true)` returns. Missing:\n$missing",
        )
    }

    context(_: KaSession)
    private fun checkDiagnosticsConsistentWithObsoleteApi(
        file: KtFile,
        fromFile: List<DiagnosticKey>,
    ) {
        val obsolete = file
            .collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            .map { it.getDiagnosticKey() }
            .sorted()

        assertEquals(
            fromFile,
            obsolete,
            "The output of the obsolete and actual API must be the same",
        )
    }

    /**
     * Diagnostics of an element's subtree must be exactly those file diagnostics which are reported inside the element.
     */
    context(_: KaSession)
    private fun checkSubtreeDiagnostics(ktFile: KtFile) {
        val fileDiagnostics = ktFile.diagnostics().withCommonAndExtendedDiagnostics().toList()

        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                val expected = fileDiagnostics
                    .filter { element.isAncestor(it.psi, strict = false) }
                    .map { it.getDiagnosticKey() }
                    .sorted()

                val actual = element.diagnostics()
                    .withCommonAndExtendedDiagnostics()
                    .map { it.getDiagnosticKey() }
                    .sorted()
                    .toList()

                assertEquals(
                    expected,
                    actual,
                    "diagnostics of the ${element::class.simpleName} subtree at ${element.getLineColumnRange()} should be the same as" +
                            " the file diagnostics reported inside it."
                )

                super.visitKtElement(element)
            }
        })
    }

    /**
     * Only [KaDiagnostics.ignoreSuppressed] with `false` parameter may yield suppressed diagnostics, and every additionally yielded diagnostic must be marked
     * as suppressed.
     */
    context(_: KaSession)
    private fun checkSuppressionMarks(ktFile: KtFile, diagnosticsFromFile: List<DiagnosticKey>) {
        val diagnostics = ktFile.diagnostics()
        val reported = diagnostics.withCommonAndExtendedDiagnostics().filter { it.isSuppressed }.toList()
        assertTrue(
            reported.isEmpty(),
            "diagnostics() must not yield suppressed diagnostics. Suppressed: ${reported.map { it.getDiagnosticKey() }}",
        )

        val reportedKeys = diagnosticsFromFile.toSet()
        val additional = diagnostics.withCommonAndExtendedDiagnostics()
            .ignoreSuppressed(false)
            .filter { it.getDiagnosticKey() !in reportedKeys }
            .toList()

        val notMarked = additional.filterNot { it.isSuppressed }
        assertTrue(
            notMarked.isEmpty(),
            "every diagnostic which is only yielded by includingSuppressed() must be marked as suppressed." +
                    " Not marked: ${notMarked.map { it.getDiagnosticKey() }}",
        )
    }

    context(_: KaSession)
    private fun KaDiagnostics.withCommonAndExtendedDiagnostics(): KaDiagnostics =
        withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)

    /**
     * Checks the contract of the [KaDiagnostics] query modifiers.
     */
    context(_: KaSession)
    private fun checkQuerySemantics(ktFile: KtFile) {
        val query = ktFile.diagnostics()

        assertEquals(
            query.diagnosticKeys(),
            query.withCheckers(KaDiagnosticCheckerKind.COMMON).diagnosticKeys(),
            "by default, a query must yield diagnostics of common checkers.",
        )

        assertTrue(
            query.withCheckers(emptySet()).none(),
            "a query without checkers must not yield any diagnostic.",
        )

        assertEquals(
            query.withCheckers(KaDiagnosticCheckerKind.COMMON).diagnosticKeys(),
            query.withCheckers(KaDiagnosticCheckerKind.EXPERIMENTAL).withCheckers(KaDiagnosticCheckerKind.COMMON).diagnosticKeys(),
            "'withCheckers' must replace the requested checker kinds instead of adding to them.",
        )

        val allCheckers = query.withCommonAndExtendedDiagnostics()
        assertEquals(
            allCheckers.diagnosticKeys(),
            allCheckers.diagnosticKeys(),
            "a query must be re-iterable.",
        )

        assertEquals(
            allCheckers.ignoreSuppressed(false).diagnosticKeys(),
            allCheckers.ignoreSuppressed(false).ignoreSuppressed(false).diagnosticKeys(),
            "'includingSuppressed' must be idempotent.",
        )

        assertEquals(
            allCheckers.diagnosticKeys(),
            allCheckers.ignoreSuppressed(false).ignoreSuppressed(true).diagnosticKeys(),
            "'exclude' must undo 'include'.",
        )

        assertEquals(
            allCheckers.diagnosticKeys(),
            allCheckers.ignoreSuppressed(true).diagnosticKeys(),
            "'exclude' must be the default behavior.",
        )
    }

    private fun KaDiagnostics.diagnosticKeys(): List<DiagnosticKey> = map { it.getDiagnosticKey() }.sorted().toList()

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
