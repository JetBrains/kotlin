/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.SymbolByFqName
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.util.suffixIfNot
import org.junit.Assert
import java.io.File

abstract class AbstractSymbolPointerRestoreTest : KotlinLightCodeInsightFixtureTestCase() {
    abstract fun KtAnalysisSession.collectSymbols(filePath: String, ktFile: KtFile): List<KtSymbol>

    protected fun doTest(path: String) {
        val file = File(path)
        val ktFile = myFixture.configureByText(file.name.suffixIfNot(".kt"), FileUtil.loadFile(file)) as KtFile

        val pointersWithRendered = executeOnPooledThreadInReadAction {
            analyze(ktFile) {
                collectSymbols(path, ktFile).map { symbol ->
                    PointerWithRenderedSymbol(
                        symbol.createPointer(),
                        DebugSymbolRenderer.render(symbol)
                    )
                }
            }
        }

        val actual = SymbolByFqName.textWithRenderedSymbolData(
            path,
            pointersWithRendered.joinToString(separator = "\n") { it.rendered },
        )

        KotlinTestUtils.assertEqualsToFile(File(path), actual)


        CommandProcessor.getInstance().runUndoTransparentAction {
            runWriteAction {
                KtPsiFactory(ktFile).apply {
                    ktFile.add(createNewLine(lineBreaks = 2))
                    ktFile.add(createProperty("val aaaaaa: Int = 10"))
                }
                PsiDocumentManager.getInstance(project).apply {
                    commitDocument(getDocument(ktFile) ?: error("Cannot find document for ktFile"))
                }
            }
        }

        // another read action
        executeOnPooledThreadInReadAction {
            analyze(ktFile) {
                val restored = pointersWithRendered.map { (pointer, expectedRender) ->
                    val restored = pointer.restoreSymbol() ?: error("Symbol $expectedRender was not not restored correctly")
                    DebugSymbolRenderer.render(restored)
                }
                val actualRestored = SymbolByFqName.textWithRenderedSymbolData(
                    path,
                    restored.joinToString(separator = "\n"),
                )

                KotlinTestUtils.assertEqualsToFile(File(path), actualRestored)
            }
        }
    }
}

private data class PointerWithRenderedSymbol(val pointer: KtSymbolPointer<*>, val rendered: String)