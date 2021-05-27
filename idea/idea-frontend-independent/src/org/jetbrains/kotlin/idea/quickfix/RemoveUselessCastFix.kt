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
import org.jetbrains.kotlin.idea.core.dropEnclosingParenthesesIfPossible
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveUselessCastFix(element: KtBinaryExpressionWithTypeRHS) : KotlinPsiOnlyQuickFixAction<KtBinaryExpressionWithTypeRHS>(element),
    CleanupFix {
    override fun getFamilyName() = KotlinBundle.message("remove.useless.cast")

    override fun getText(): String = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        invoke(element ?: return)
    }

    companion object : QuickFixesPsiBasedFactory<PsiElement>(PsiElement::class, PsiElementSuitabilityCheckers.ALWAYS_SUITABLE) {
        operator fun invoke(element: KtBinaryExpressionWithTypeRHS) = element.replaced(element.left).dropEnclosingParenthesesIfPossible()

        override fun doCreateQuickFix(psiElement: PsiElement): List<IntentionAction> {
            val expression = psiElement.getNonStrictParentOfType<KtBinaryExpressionWithTypeRHS>() ?: return emptyList()
            return listOf(RemoveUselessCastFix(expression))
        }
    }
}

