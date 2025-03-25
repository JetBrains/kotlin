/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.diagnostics

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.closestPsiElement

// TODO: extract PSI-independent parts, specifically coordinate classes
object PsiDiagnosticUtils {
    @JvmStatic
    fun atLocation(element: PsiElement): String {
        if (element.isValid()) {
            return atLocation(element.getContainingFile(), element.getTextRange())
        }

        var file: PsiFile? = null
        var offset = -1
        try {
            file = element.getContainingFile()
            offset = element.getTextOffset()
        } catch (invalidException: PsiInvalidElementAccessException) {
            // ignore
        }

        return "at offset: " + (if (offset != -1) offset else "<unknown>") + " file: " + (if (file != null) file else "<unknown>")
    }

    @JvmStatic
    fun atLocation(expression: KtExpression): String {
        return atLocation(expression.getNode())
    }

    fun atLocation(node: ASTNode): String {
        val startOffset = node.getStartOffset()
        val element = node.closestPsiElement()
        if (element != null) {
            return atLocation(element)
        }

        return "at offset " + startOffset + " (line and file unknown: no PSI element)"
    }

    @JvmStatic
    fun atLocation(file: PsiFile, textRange: TextRange): String {
        val document = file.getViewProvider().getDocument()
        return atLocation(file, textRange, document)
    }

    fun atLocation(file: PsiFile, textRange: TextRange, document: Document?): String {
        val offset = textRange.getStartOffset()
        val virtualFile = file.getVirtualFile()
        val pathSuffix = " in " + (if (virtualFile == null) file.getName() else virtualFile.getPath())
        return offsetToLineAndColumn(document, offset).toString() + pathSuffix
    }

    @JvmStatic
    fun offsetToLineAndColumn(document: Document?, offset: Int): LineAndColumn {
        if (document == null || document.getTextLength() == 0) {
            return LineAndColumn(-1, offset, null)
        }

        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val column = offset - lineStartOffset

        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val lineContent = document.getCharsSequence().subSequence(lineStartOffset, lineEndOffset)

        return LineAndColumn(lineNumber + 1, column + 1, lineContent.toString())
    }

    class LineAndColumn(@JvmField val line: Int, @JvmField val column: Int, @JvmField val lineContent: String?) {
        // NOTE: This method is used for presenting positions to the user
        override fun toString(): String {
            if (line < 0) {
                return "(offset: " + column + " line unknown)"
            }
            return "(" + line + "," + column + ")"
        }

        companion object {
            @JvmField
            val NONE: LineAndColumn = LineAndColumn(-1, -1, null)
        }
    }

    class LineAndColumnRange(@JvmField val start: LineAndColumn, @JvmField val end: LineAndColumn) {
        // NOTE: This method is used for presenting positions to the user
        override fun toString(): String {
            if (start.line == end.line) {
                return "(" + start.line + "," + start.column + "-" + end.column + ")"
            }

            return start.toString() + " - " + end
        }

        companion object {
            @JvmField
            val NONE: LineAndColumnRange = LineAndColumnRange(LineAndColumn.Companion.NONE, LineAndColumn.Companion.NONE)
        }
    }
}
