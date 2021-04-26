/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

abstract class ExclExclCallFix(psiElement: PsiElement) : KotlinPsiOnlyQuickFixAction<PsiElement>(psiElement) {
    override fun getFamilyName(): String = text

    override fun startInWriteAction(): Boolean = true
}

class RemoveExclExclCallFix(psiElement: PsiElement) : ExclExclCallFix(psiElement), CleanupFix, HighPriorityAction {
    override fun getText(): String = KotlinBundle.message("fix.remove.non.null.assertion")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val postfixExpression = element as? KtPostfixExpression ?: return
        val expression = KtPsiFactory(project).createExpression(postfixExpression.baseExpression!!.text)
        postfixExpression.replace(expression)
    }

    companion object : QuickFixesPsiBasedFactory<PsiElement>(PsiElement::class, PsiElementSuitabilityCheckers.ALWAYS_SUITABLE) {
        override fun doCreateQuickFix(psiElement: PsiElement): List<IntentionAction> {
            val postfixExpression = psiElement.getNonStrictParentOfType<KtPostfixExpression>() ?: return emptyList()
            return listOfNotNull(RemoveExclExclCallFix(postfixExpression))
        }
    }
}