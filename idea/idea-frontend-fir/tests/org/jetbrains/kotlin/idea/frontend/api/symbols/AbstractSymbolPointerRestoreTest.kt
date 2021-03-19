/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.analyseOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.test.framework.AbstractKtIdeaTest
import org.jetbrains.kotlin.idea.test.framework.TestFileStructure
import org.jetbrains.kotlin.idea.test.framework.TestStructureExpectedDataBlock
import org.jetbrains.kotlin.idea.test.framework.TestStructureRenderer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractSymbolPointerRestoreTest : AbstractKtIdeaTest() {
    abstract fun KtAnalysisSession.collectSymbols(fileStructure: TestFileStructure): List<KtSymbol>

    override fun doTestByFileStructure(fileStructure: TestFileStructure) {
        val pointersWithRendered = analyseOnPooledThreadInReadAction(fileStructure.mainKtFile) {
            collectSymbols(fileStructure).map { symbol ->
                PointerWithRenderedSymbol(
                    symbol.createPointer(),
                    DebugSymbolRenderer.render(symbol)
                )
            }
        }

        val actual = TestStructureRenderer.render(
            fileStructure,
            TestStructureExpectedDataBlock(values = pointersWithRendered.map { it.rendered }),
            renderingMode = TestStructureRenderer.RenderingMode.ALL_BLOCKS_IN_MULTI_LINE_COMMENT,
        )

        KotlinTestUtils.assertEqualsToFile(fileStructure.filePath.toFile(), actual)

        doOutOfBlockModification(fileStructure.mainKtFile)

        restoreSymbolsInOtherReadActionAndCompareResults(fileStructure, pointersWithRendered)
    }

    private fun restoreSymbolsInOtherReadActionAndCompareResults(
        fileStructure: TestFileStructure,
        pointersWithRendered: List<PointerWithRenderedSymbol>
    ) {
        val restored = analyseOnPooledThreadInReadAction(fileStructure.mainKtFile) {
            pointersWithRendered.map { (pointer, expectedRender) ->
                val restored = pointer.restoreSymbol() ?: error("Symbol $expectedRender was not not restored correctly")
                DebugSymbolRenderer.render(restored)
            }
        }
        val actualRestored = TestStructureRenderer.render(
            fileStructure,
            TestStructureExpectedDataBlock(values = restored),
            renderingMode = TestStructureRenderer.RenderingMode.ALL_BLOCKS_IN_MULTI_LINE_COMMENT,
        )

        KotlinTestUtils.assertEqualsToFile(fileStructure.filePath.toFile(), actualRestored)
    }

    private fun doOutOfBlockModification(ktFile: KtFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            runWriteAction {
                val document = PsiDocumentManager.getInstance(project).getDocument(ktFile) ?: error("Cannot find document for ktFile")
                val initialText = ktFile.text
                val ktPsiFactory = KtPsiFactory(ktFile)
                ktFile.add(ktPsiFactory.createNewLine(lineBreaks = 2))
                ktFile.add(ktPsiFactory.createProperty("val aaaaaa: Int = 10"))
                commitDocument(document)
                document.setText(initialText)
                commitDocument(document)
            }
        }
    }

    private fun commitDocument(document: Document) {
        PsiDocumentManager.getInstance(project).commitDocument(document)
    }
}

private data class PointerWithRenderedSymbol(val pointer: KtSymbolPointer<*>, val rendered: String)