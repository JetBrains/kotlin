/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class AddIsToWhenConditionFix(
    expression: KtWhenConditionWithExpression,
    private val referenceText: String
) : KotlinQuickFixAction<KtWhenConditionWithExpression>(expression) {

    override fun getText(): String = "Add 'is' before '$referenceText'"

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val expression = element ?: return
        val replaced = expression.replaced(KtPsiFactory(expression).createWhenCondition("is ${expression.text}"))
        editor?.caretModel?.moveToOffset(replaced.endOffset)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtWhenConditionWithExpression>? {
            val element = diagnostic.psiElement.parent as? KtWhenConditionWithExpression ?: return null
            return AddIsToWhenConditionFix(element, element.text)
        }
    }

}