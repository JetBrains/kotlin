/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.test.framework

import org.jetbrains.kotlin.idea.fir.test.framework.SelectedExpressionProvider
import org.jetbrains.kotlin.idea.fir.test.framework.KtTest
import org.jetbrains.kotlin.idea.fir.test.framework.TestFile
import org.jetbrains.kotlin.idea.fir.test.framework.TestFileStructure
import org.jetbrains.kotlin.idea.fir.test.framework.TestStructureExpectedDataBlock
import org.jetbrains.kotlin.psi.psiUtil.getStartOffsetIn

object TestStructureRenderer {
    fun render(
        testStructure: TestFileStructure,
        vararg expectedData: TestStructureExpectedDataBlock,
        renderingMode: RenderingMode = RenderingMode.EVERY_LINE_WITH_SINGLE_LINE_COMMENT,
    ): String =
        render(testStructure, expectedData.toList(), renderingMode)

    fun render(
        testStructure: TestFileStructure,
        expectedData: List<TestStructureExpectedDataBlock>,
        renderingMode: RenderingMode = RenderingMode.EVERY_LINE_WITH_SINGLE_LINE_COMMENT
    ): String = buildString {
        renderFiles(testStructure)
        appendLine()
        renderExpectedData(expectedData, renderingMode)
        renderCaretSymbol(testStructure)
        renderExpressionTag(testStructure)
    }

    private fun StringBuilder.renderCaretSymbol(testStructure: TestFileStructure) {
        testStructure.caretPosition?.let { position ->
            insert(position, KtTest.CARET_SYMBOL)
        }
    }

    private fun StringBuilder.renderExpressionTag(testStructure: TestFileStructure) {
        testStructure.mainFile.selectedExpression?.let { expression ->
            val offset = expression.getStartOffsetIn(testStructure.mainKtFile)
            insert(offset + expression.textLength, SelectedExpressionProvider.TAGS.CLOSING_EXPRESSION_TAG)
            insert(offset, SelectedExpressionProvider.TAGS.OPENING_EXPRESSION_TAG)
        }
    }

    private fun StringBuilder.renderFiles(testStructure: TestFileStructure) {
        if (testStructure.otherFiles.isEmpty()) {
            appendLine(testStructure.mainFile.psiFile.text)
        } else {
            testStructure.allFiles.forEach { file ->
                renderFile(file)
            }
        }
    }

    private fun StringBuilder.renderExpectedData(expectedData: List<TestStructureExpectedDataBlock>, renderingMode: RenderingMode) {
        if (expectedData.isEmpty()) return
        appendLine(KtTest.RESULT_DIRECTIVE)
        if (renderingMode == RenderingMode.ALL_BLOCKS_IN_MULTI_LINE_COMMENT) {
            appendLine("/*")
        }
        expectedData.forEachIndexed { index, block ->
            renderExpectedDataBlock(
                block,
                asSingleLineComment = renderingMode == RenderingMode.EVERY_LINE_WITH_SINGLE_LINE_COMMENT
            )
            if (index != expectedData.lastIndex) {
                appendLine()
            }
        }
        if (renderingMode == RenderingMode.ALL_BLOCKS_IN_MULTI_LINE_COMMENT) {
            appendLine("*/")
        }
    }

    private fun StringBuilder.renderExpectedDataBlock(block: TestStructureExpectedDataBlock, asSingleLineComment: Boolean) {
        val singleLineCommentPrefix = if (asSingleLineComment) "// " else ""
        block.name?.let { name -> appendLine("$singleLineCommentPrefix$name") }
        block.values.forEachIndexed { index, value ->
            appendLine("$singleLineCommentPrefix${value.trim()}")
            if (index != block.values.lastIndex && !asSingleLineComment) {
                appendLine()
            }
        }
    }

    private fun StringBuilder.renderFile(file: TestFile) {
        appendLine("${KtTest.FILE_DIRECTIVE} ${file.psiFile.name}")
        appendLine(file.psiFile.text)
        appendLine()
    }

    enum class RenderingMode {
        EVERY_LINE_WITH_SINGLE_LINE_COMMENT,
        ALL_BLOCKS_IN_MULTI_LINE_COMMENT
    }
}