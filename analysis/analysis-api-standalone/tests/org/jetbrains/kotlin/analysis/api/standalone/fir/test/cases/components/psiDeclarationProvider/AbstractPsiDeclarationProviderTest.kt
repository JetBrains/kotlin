/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.components.psiDeclarationProvider

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.providers.DecompiledPsiDeclarationProvider.findPsi
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

public abstract class AbstractPsiDeclarationProviderTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val mainKtFile = ktFiles.singleOrNull() ?: ktFiles.firstOrNull { it.name == "main.kt" } ?: ktFiles.first()
        val caretPosition = testServices.expressionMarkerProvider.getCaretPosition(mainKtFile)
        val ktReferences = findReferencesAtCaret(mainKtFile, caretPosition)
        if (ktReferences.isEmpty()) {
            testServices.assertions.fail { "No references at caret found" }
        }

        val element = ktReferences.first().element
        val resolvedTo =
            analyseForTest(element) {
                val symbols = ktReferences.flatMap { it.resolveToSymbols() }
                val psiElements = symbols.mapNotNull { psiForTest(it, element.project) }
                psiElements.joinToString(separator = "\n") { TestPsiElementRenderer.render(it) }
            }

        if (Directives.UNRESOLVED_REFERENCE in module.directives) {
            return
        }

        val actual = "Resolved to:\n$resolvedTo"
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
        mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

    // Mimic [psiForUast] in FIR UAST
    private fun KtAnalysisSession.psiForTest(symbol: KtSymbol, project: Project): PsiElement? {
        return when (symbol.origin) {
            KtSymbolOrigin.LIBRARY -> {
                findPsi(symbol, project) ?: symbol.psi
            }
            else -> symbol.psi
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val UNRESOLVED_REFERENCE by directive(
            "Reference should be unresolved",
        )
    }
}