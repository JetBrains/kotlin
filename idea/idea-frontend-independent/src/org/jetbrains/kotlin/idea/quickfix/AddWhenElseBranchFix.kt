/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class AddWhenElseBranchFix(element: KtWhenExpression) : KotlinPsiOnlyQuickFixAction<KtWhenExpression>(element) {
    override fun getFamilyName() = KotlinBundle.message("fix.add.else.branch.when")
    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return element.closeBrace != null
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val psiFactory = KtPsiFactory(file)
        val entry = psiFactory.createWhenEntry("else ->")
        val whenCloseBrace = element.closeBrace ?: error("isAvailable should check if close brace exist")
        val insertedWhenEntry =
            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(element.addBefore(entry, whenCloseBrace)) as KtWhenEntry
        val endOffset = insertedWhenEntry.endOffset
        editor?.document?.insertString(endOffset, " ")
        editor?.caretModel?.moveToOffset(endOffset + 1)
    }

    companion object : QuickFixesPsiBasedFactory<PsiElement>(PsiElement::class, PsiElementSuitabilityCheckers.ALWAYS_SUITABLE) {
        override fun doCreateQuickFix(psiElement: PsiElement): List<IntentionAction> {
            return listOfNotNull(psiElement.getNonStrictParentOfType<KtWhenExpression>()?.let(::AddWhenElseBranchFix))
        }
    }
}
