/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.joinLines

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.siblings

class JoinDeclarationAndAssignmentHandler : JoinRawLinesHandlerDelegate {

    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is KtFile) return -1

        val element = file.findElementAt(start)
            ?.siblings(forward = false, withItself = false)
            ?.firstOrNull { !isToSkip(it) } ?: return -1

        val pair = element.parentsWithSelf
            .mapNotNull { getPropertyAndAssignment(it) }
            .firstOrNull() ?: return -1
        val (property, assignment) = pair

        doJoin(property, assignment)
        return property.textRange!!.startOffset
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int) = -1

    private fun getPropertyAndAssignment(element: PsiElement): Pair<KtProperty, KtBinaryExpression>? {
        val property = element as? KtProperty ?: return null
        if (property.hasInitializer()) return null

        val assignment = element.siblings(forward = true, withItself = false)
            .firstOrNull { !isToSkip(it) } as? KtBinaryExpression ?: return null
        if (assignment.operationToken != KtTokens.EQ) return null

        val left = assignment.left as? KtSimpleNameExpression ?: return null
        if (assignment.right == null) return null
        if (left.getReferencedName() != property.name) return null

        return property to assignment
    }

    private fun doJoin(property: KtProperty, assignment: KtBinaryExpression) {
        property.initializer = assignment.right
        property.parent.deleteChildRange(property.nextSibling, assignment) //TODO: should we delete range?
    }

    private fun isToSkip(element: PsiElement): Boolean {
        return when (element) {
            is PsiWhiteSpace -> StringUtil.getLineBreakCount(element.getText()!!) <= 1 // do not skip blank line
            else -> element.node!!.elementType == KtTokens.SEMICOLON
        }
    }
}
