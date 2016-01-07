/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.joinLines

import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.parents
import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

class JoinDeclarationAndAssignmentHandler : JoinRawLinesHandlerDelegate {

    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is KtFile) return -1

        val element = file.findElementAt(start)
                              ?.siblings(forward = false, withItself = false)
                              ?.firstOrNull { !isToSkip(it) }  ?: return -1

        val pair = element.parentsWithSelf
                           .mapNotNull { getPropertyAndAssignment(it) }
                           .firstOrNull() ?: return -1
        val (property, assignment) = pair

        doJoin(property, assignment)
        return property.textRange!!.startOffset
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int)
            = -1

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
        property.setInitializer(assignment.right)
        property.parent!!.deleteChildRange(property.nextSibling, assignment) //TODO: should we delete range?
    }

    private fun isToSkip(element: PsiElement): Boolean {
        return when (element) {
            is PsiWhiteSpace -> StringUtil.getLineBreakCount(element.getText()!!) <= 1 // do not skip blank line
            else -> element.node!!.elementType == KtTokens.SEMICOLON
        }
    }
}
