/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test.framework

import org.jetbrains.kotlin.psi.psiUtil.getStartOffsetIn

object TestStructureRenderer {
    fun render(testStructure: TestFileStructure, vararg expectedData: TestStructureExpectedDataBlock): String = buildString {
        renderFiles(testStructure)
        renderExpectedData(expectedData.toList())
        renderCaretSymbol(testStructure)
        renderExpressionTag(testStructure)
    }

    fun render(testStructure: TestFileStructure, expectedData: List<TestStructureExpectedDataBlock>): String = buildString {
        renderFiles(testStructure)
        renderExpectedData(expectedData)
        renderCaretSymbol(testStructure)
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

    private fun StringBuilder.renderExpectedData(expectedData: List<TestStructureExpectedDataBlock>) {
        if (expectedData.isNotEmpty()) {
            appendLine(KtTest.RESULT_DIRECTIVE)
            appendLine()
            expectedData.forEach { block ->
                renderExpectedDataBlock(block)
                appendLine()
            }
        }
    }

    private fun StringBuilder.renderExpectedDataBlock(block: TestStructureExpectedDataBlock) {
        block.name?.let { name -> appendLine("// $name") }
        block.values.forEach { value ->
            if (value.lines().size > 1) {
                appendLine(
                    """|/*
                       |${value.trim()}
                       |*/
                    """.trimMargin()
                )
            } else {
                appendLine("// $value")
            }
        }
    }

    private fun StringBuilder.renderFile(file: TestFile) {
        appendLine("${KtTest.FILE_DIRECTIVE} ${file.psiFile.name}")
        appendLine(file.psiFile.text)
        appendLine()
    }
}