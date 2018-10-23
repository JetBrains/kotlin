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
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.ConstantConditionIfInspection
import org.jetbrains.kotlin.idea.intentions.SimplifyBooleanWithConstantsIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class SimplifyComparisonFix(element: KtExpression, val value: Boolean) : KotlinQuickFixAction<KtExpression>(element) {
    override fun getFamilyName() = "Simplify $element to '$value'"

    override fun getText() = "Simplify comparison"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val replacement = KtPsiFactory(element).createExpression("$value")
        val result = element.replaced(replacement)

        val booleanExpression = result.getNonStrictParentOfType<KtBinaryExpression>()
        val simplifyIntention = SimplifyBooleanWithConstantsIntention()
        if (booleanExpression != null && simplifyIntention.isApplicableTo(booleanExpression)) {
            simplifyIntention.applyTo(booleanExpression, editor)
        } else {
            simplifyIntention.removeRedundantAssertion(result)
        }

        val ifExpression = result.getStrictParentOfType<KtIfExpression>()?.takeIf { it.condition == result }
        if (ifExpression != null) ConstantConditionIfInspection.applyFixIfSingle(ifExpression)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = diagnostic.psiElement as? KtExpression ?: return null
            val value = (diagnostic as? DiagnosticWithParameters2<*, *, *>)?.b as? Boolean ?: return null
            return SimplifyComparisonFix(expression, value)
        }
    }
}