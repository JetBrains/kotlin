/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractRawFirBuilderSourceElementMappingTestCase : AbstractRawFirBuilderTestCase() {
    override fun doRawFirTest(filePath: String) {
        val fileTextWithCaret = loadFile(filePath)!!
        val fileTextWithoutCaret = fileTextWithCaret.replace(START_EXPRESSION_TAG, "").replace(END_EXPRESSION_TAG, "")
        val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileTextWithoutCaret) as KtFile
        val selectedExpression = run {
            val startCaretPosition = fileTextWithCaret.indexOf(START_EXPRESSION_TAG)
            if (startCaretPosition < 0) {
                error("$START_EXPRESSION_TAG was not found in the file")
            }
            val endCaretPosition = fileTextWithCaret.indexOf(END_EXPRESSION_TAG)
            if (endCaretPosition < 0) {
                error("$END_EXPRESSION_TAG was not found in the file")
            }
            val elements = ktFile.elementsInRange(TextRange(startCaretPosition, endCaretPosition - START_EXPRESSION_TAG.length))
            if (elements.size != 1) {
                error("Expected one element at rage but found ${elements.size} [${elements.joinToString { it.text }}]")
            }
            elements.single() as KtElement
        }
        val firFile = ktFile.toFirFile()
        val foundElement = run {
            val foundElements = FindElementVisitor.find(firFile, selectedExpression)
            if (foundElements.size != 1) {
                error("One element expected but found [${foundElements.joinToString { "${it::class.simpleName} `${it.render()}`" }}]")
            }
            foundElements.single()
        }

        val expectedPath = filePath.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), foundElement.render())
    }

    companion object {
        private const val START_EXPRESSION_TAG = "<expr>"
        private const val END_EXPRESSION_TAG = "</expr>"
    }

    private object FindElementVisitor : FirVisitor<Unit, ElementFindingResult>() {
        override fun visitElement(element: FirElement, data: ElementFindingResult) {
            element.realPsi?.let { psi ->
                if (data.psi == psi) {
                    data.result += element
                }
            }
            element.acceptChildren(this, data)
        }

        fun find(firFile: FirFile, element: KtElement): Set<FirElement> =
            ElementFindingResult(element, mutableSetOf()).also { firFile.accept(this, it) }.result
    }

    private data class ElementFindingResult(val psi: KtElement, val result: MutableSet<FirElement>)
}
