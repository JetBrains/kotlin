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

package org.jetbrains.jet.plugin.intentions.declarations

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lexer.JetTokens

public class JetDeclarationJoinLinesHandler : JoinRawLinesHandlerDelegate {

    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        val element = JetPsiUtil.skipSiblingsBackwardByPredicate(file.findElementAt(start), DeclarationUtils.SKIP_DELIMITERS) ?: return -1

        val pair = element.parents(withItself = true)
                           .map { getPropertyAndAssignment(it) }
                           .filterNotNull()
                           .firstOrNull() ?: return -1
        val (property, assignment) = pair

        return doJoin(property, assignment).getTextRange()!!.getStartOffset()
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int)
            = -1

    private fun getPropertyAndAssignment(element: PsiElement): Pair<JetProperty, JetBinaryExpression>? {
        val property = element as? JetProperty ?: return null
        if (property.hasInitializer()) return null

        val assignment = JetPsiUtil.skipSiblingsForwardByPredicate(element, DeclarationUtils.SKIP_DELIMITERS) as? JetBinaryExpression ?: return null
        if (assignment.getOperationToken() != JetTokens.EQ) return null

        val left = assignment.getLeft() as? JetSimpleNameExpression ?: return null
        if (assignment.getRight() == null) return null
        if (left.getReferencedName() != property.getName()) return null

        return property to assignment
    }

    private fun doJoin(property: JetProperty, assignment: JetBinaryExpression): JetProperty {
        val newProperty = DeclarationUtils.changePropertyInitializer(property, assignment.getRight())
        property.getParent()!!.deleteChildRange(property.getNextSibling(), assignment)
        return property.replace(newProperty) as JetProperty
    }

}