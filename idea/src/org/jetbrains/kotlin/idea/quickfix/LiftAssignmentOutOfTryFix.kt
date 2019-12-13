/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTryExpression

class LiftAssignmentOutOfTryFix(element: KtTryExpression) : KotlinQuickFixAction<KtTryExpression>(element) {
    override fun getFamilyName() = text

    override fun getText() = "Lift assignment out of 'try' expression"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        BranchedFoldingUtils.foldToAssignment(element)
    }

    companion object : KotlinSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = diagnostic.psiElement as? KtExpression ?: return null
            val originalCatch = expression.parent.parent?.parent as? KtCatchClause ?: return null
            val tryExpression = originalCatch.parent as? KtTryExpression ?: return null

            if (BranchedFoldingUtils.getFoldableAssignmentNumber(tryExpression) < 1) return null
            return LiftAssignmentOutOfTryFix(tryExpression)
        }
    }
}