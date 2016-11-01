/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.psi.*

class LiftAssignmentOutOfTryFix(element: KtTryExpression): KotlinQuickFixAction<KtTryExpression>(element) {
    override fun getFamilyName() = text

    override fun getText() = "Lift assignment out of 'try' expression"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val tryAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(element.tryBlock) ?: return

        val op = tryAssignment.operationReference.text
        val leftText = tryAssignment.left?.text ?: return

        tryAssignment.replace(tryAssignment.right ?: return)

        for (catchClause in element.catchClauses) {
            val catchAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(catchClause.catchBody)
            catchAssignment?.replace(catchAssignment.right!!)
        }

        element.replace(KtPsiFactory(element).createExpressionByPattern("$0 $1 $2", leftText, op, element))
    }

    companion object : KotlinSingleIntentionActionFactory() {

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = diagnostic.psiElement as? KtExpression ?: return null
            val originalCatch = expression.parent?.parent?.parent as? KtCatchClause ?: return null
            val tryExpression = originalCatch.parent as? KtTryExpression ?: return null
            val tryAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(tryExpression.tryBlock) ?: return null
            for (catchClause in tryExpression.catchClauses) {
                val catchAssignment = BranchedFoldingUtils.getFoldableBranchedAssignment(catchClause.catchBody) ?: return null
                if (!BranchedFoldingUtils.checkAssignmentsMatch(tryAssignment, catchAssignment)) return null
            }
            return LiftAssignmentOutOfTryFix(tryExpression)
        }
    }
}