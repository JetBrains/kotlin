/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.addExternalTestFiles
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.getAnalysisSessionFor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractSymbolsByPsiBuildingTest : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(path: String) {
        addExternalTestFiles(path)
        val file = File(path)
        val ktFile = myFixture.configureByText(file.name, FileUtil.loadFile(file)) as KtFile

        val renderedSymbols = executeOnPooledThreadInReadAction {
            analyze(ktFile) {
                val declarationSymbols = ktFile.collectDescendantsOfType<KtDeclaration>().map { declaration ->
                    declaration.getSymbol()
                }
                declarationSymbols.map(DebugSymbolRenderer::render)
            }
        }

        val actual = buildString {
            val actualSymbolsData = renderedSymbols.joinToString(separator = "\n")
            val fileTextWithoutSymbolsData = ktFile.text.substringBeforeLast(SYMBOLS_TAG).trimEnd()
            appendLine(fileTextWithoutSymbolsData)
            appendLine()
            appendLine(SYMBOLS_TAG)
            appendLine("/*")
            append(actualSymbolsData)
            appendLine("*/")
        }
        KotlinTestUtils.assertEqualsToFile(file, actual)

    }

    companion object {
        private const val SYMBOLS_TAG = "// SYMBOLS:"
    }
}
