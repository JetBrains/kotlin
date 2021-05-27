/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveUselessIsCheckFixForWhen(element: KtWhenConditionIsPattern) : KotlinPsiOnlyQuickFixAction<KtWhenConditionIsPattern>(element) {
    override fun getFamilyName() = KotlinBundle.message("remove.useless.is.check")

    override fun getText(): String = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val condition = element ?: return
        val whenEntry = condition.parent as? KtWhenEntry ?: return
        val whenExpression = whenEntry.parent as? KtWhenExpression ?: return

        if (condition.isNegated) {
            condition.parent.delete()
        } else {
            whenExpression.entries.dropWhile { it != whenEntry }.forEach { it.delete() }
            val whenEntryExpression = whenEntry.expression ?: return
            val newEntry = KtPsiFactory(project).createWhenEntry("else -> ${whenEntryExpression.text}")
            whenExpression.addBefore(newEntry, whenExpression.closeBrace)
        }
    }

    companion object : QuickFixesPsiBasedFactory<PsiElement>(PsiElement::class, PsiElementSuitabilityCheckers.ALWAYS_SUITABLE) {
        override fun doCreateQuickFix(psiElement: PsiElement): List<IntentionAction> {
            val expression = psiElement.getNonStrictParentOfType<KtWhenConditionIsPattern>() ?: return emptyList()
            return listOf(RemoveUselessIsCheckFixForWhen(expression))
        }
    }
}
