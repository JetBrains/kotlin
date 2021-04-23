/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

class AddExclExclCallFix(psiElement: PsiElement, val fixImplicitReceiver: Boolean = false) : ExclExclCallFix(psiElement),
    LowPriorityAction {

    override fun getText() = KotlinBundle.message("fix.introduce.non.null.assertion")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val modifiedExpression = element ?: return
        val psiFactory = KtPsiFactory(project)
        val exclExclExpression = if (fixImplicitReceiver) {
            if (modifiedExpression is KtCallableReferenceExpression) {
                psiFactory.createExpressionByPattern("this!!::$0", modifiedExpression.callableReference)
            } else {
                psiFactory.createExpressionByPattern("this!!.$0", modifiedExpression)
            }
        } else {
            psiFactory.createExpressionByPattern("$0!!", modifiedExpression)
        }
        modifiedExpression.replace(exclExclExpression)
    }
}