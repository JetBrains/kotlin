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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveUselessIsCheckFix(element: KtIsExpression) : KotlinPsiOnlyQuickFixAction<KtIsExpression>(element) {
    override fun getFamilyName() = KotlinBundle.message("remove.useless.is.check")

    override fun getText(): String = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.run {
            val expressionsText = this.isNegated.not().toString()
            val newExpression = KtPsiFactory(project).createExpression(expressionsText)
            replace(newExpression)
        }
    }

    companion object : QuickFixesPsiBasedFactory<PsiElement>(PsiElement::class, PsiElementSuitabilityCheckers.ALWAYS_SUITABLE) {
        override fun doCreateQuickFix(psiElement: PsiElement): List<IntentionAction> {
            val expression = psiElement.getNonStrictParentOfType<KtIsExpression>() ?: return emptyList()
            return listOf(RemoveUselessIsCheckFix(expression))
        }
    }
}
