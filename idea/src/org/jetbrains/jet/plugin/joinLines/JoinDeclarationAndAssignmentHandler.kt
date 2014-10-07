/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.joinLines

import org.jetbrains.jet.lang.psi.psiUtil.siblings
import org.jetbrains.jet.lang.psi.psiUtil.parents
import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiComment
import org.jetbrains.jet.lexer.JetTokens

public class JoinDeclarationAndAssignmentHandler : JoinRawLinesHandlerDelegate {

    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is JetFile) return -1

        val element = file.findElementAt(start)
                              ?.siblings(forward = false, withItself = false)
                              ?.firstOrNull { !isToSkip(it) }  ?: return -1

        val pair = element.parents(withItself = true)
                           .map { getPropertyAndAssignment(it) }
                           .filterNotNull()
                           .firstOrNull() ?: return -1
        val (property, assignment) = pair

        doJoin(property, assignment)
        return property.getTextRange()!!.getStartOffset()
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int)
            = -1

    private fun getPropertyAndAssignment(element: PsiElement): Pair<JetProperty, JetBinaryExpression>? {
        val property = element as? JetProperty ?: return null
        if (property.hasInitializer()) return null

        val assignment = element.siblings(forward = true, withItself = false)
                                 .firstOrNull { !isToSkip(it) } as? JetBinaryExpression ?: return null
        if (assignment.getOperationToken() != JetTokens.EQ) return null

        val left = assignment.getLeft() as? JetSimpleNameExpression ?: return null
        if (assignment.getRight() == null) return null
        if (left.getReferencedName() != property.getName()) return null

        return property to assignment
    }

    private fun doJoin(property: JetProperty, assignment: JetBinaryExpression) {
        property.setInitializer(assignment.getRight())
        property.getParent()!!.deleteChildRange(property.getNextSibling(), assignment) //TODO: should we delete range?
    }

    private fun isToSkip(element: PsiElement)
            = element is PsiWhiteSpace || element is PsiComment || element.getNode()!!.getElementType() == JetTokens.SEMICOLON
}