/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.deleteSingle
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class AddIfBodyFix(element: KtIfExpression) : KotlinQuickFixAction<KtIfExpression>(element) {
    override fun getFamilyName() = "Add if body"
    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return element.closeBrace != null
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val psiFactory = KtPsiFactory(file)

        val condition = element.condition
        val expression = element.subjectExpression

        // Check if there's an expression
        if(element.openBrace != null) {
            expression?.children?.map {
                if(it.text.contains("//")){
                    // Add comment to the end of the expression
                    expression.lastChild?.add(it)

                    // Delete the comment in it's old position
                    expression.deleteChildRange(it, it.nextSibling)
                }
            }
        }

        val entry = psiFactory.createIfWithBody(condition, expression)
        val ifCloseBrace = element.closeBrace ?: error("isAvailable should check if close brace exist")
        val insertedIfBody =
            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(element.addBefore(entry, ifCloseBrace)) as KtIfExpression
        val endOffset = insertedIfBody.endOffset
        editor?.document?.insertString(endOffset, " ")
        editor?.caretModel?.moveToOffset(endOffset + 1)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): AddIfBodyFix? {
            return diagnostic.psiElement.getNonStrictParentOfType<KtIfExpression>()?.let(::AddIfBodyFix)
        }
    }
}
