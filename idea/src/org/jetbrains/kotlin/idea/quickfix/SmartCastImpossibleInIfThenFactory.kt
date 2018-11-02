/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToSafeAccessInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToElvisIntention
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

object SmartCastImpossibleInIfThenFactory : KotlinSingleIntentionActionFactory() {

    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val element = diagnostic.psiElement as? KtNameReferenceExpression ?: return null
        val ifExpression = element.getStrictParentOfType<KtContainerNodeForControlStructureBody>()?.parent as? KtIfExpression ?: return null
        return when {
            IfThenToSafeAccessInspection.canReplaceWithSafeAccess(ifExpression, needStableElement = false) ->
                IfThenToSafeAccessFix(ifExpression, IfThenToSafeAccessInspection.createFixText(ifExpression))
            IfThenToElvisIntention.canReplaceWithElvis(ifExpression, needStableElement = false) ->
                IfThenToElvisFix(ifExpression)
            else ->
                null
        }
    }

    class IfThenToSafeAccessFix(
        ifExpression: KtIfExpression,
        private val fixText: String
    ) : KotlinQuickFixAction<KtIfExpression>(ifExpression) {
        override fun getText() = fixText

        override fun getFamilyName() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val ifExpression = element ?: return
            IfThenToSafeAccessInspection.replaceWithSafeAccess(ifExpression, editor)
        }
    }

    class IfThenToElvisFix(ifExpression: KtIfExpression) : KotlinQuickFixAction<KtIfExpression>(ifExpression) {
        override fun getText() = IfThenToElvisIntention.fixText

        override fun getFamilyName() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val ifExpression = element ?: return
            IfThenToElvisIntention.replaceWithElvis(ifExpression, editor)
        }
    }
}