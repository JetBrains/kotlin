/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.components.psiDeclarationProvider

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

public abstract class AbstractPsiDeclarationProviderMultiModuleBinaryTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val allKtFiles = moduleStructure.modules.flatMap {
            testServices.ktModuleProvider.getModuleFiles(it).filterIsInstance<KtFile>()
        }
        val mainKtFile = allKtFiles.firstOrNull { it.name == "main.kt" } ?: allKtFiles.first()
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

        val actual = "Resolved to:\n$resolvedTo"
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
