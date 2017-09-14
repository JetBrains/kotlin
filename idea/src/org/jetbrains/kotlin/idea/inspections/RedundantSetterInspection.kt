/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class RedundantSetterInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                super.visitPropertyAccessor(accessor)
                if (accessor.isRedundantSetter()) {
                    holder.registerProblem(accessor,
                                           "Redundant setter",
                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                           RemoveRedundantSetterFix())
                }
            }
        }
    }
}

private fun KtPropertyAccessor.isRedundantSetter(): Boolean {
    if (!isSetter) return false
    if (annotationEntries.isNotEmpty()) return false
    if (hasLowerVisibilityThanProperty()) return false
    val expression = bodyExpression ?: return true
    if (expression is KtBlockExpression) {
        if (expression.statements.isEmpty()) return true
        val statement = expression.statements.takeIf { it.size == 1 }?.firstOrNull() ?: return false
        val parameter = valueParameters.takeIf { it.size == 1 }?.firstOrNull() ?: return false
        val binaryExpression = statement as? KtBinaryExpression ?: return false
        return binaryExpression.operationToken == KtTokens.EQ
               && binaryExpression.left?.isFieldText() == true
               && binaryExpression.right?.mainReference?.resolve() == parameter
    }
    return false
}

private fun KtPropertyAccessor.hasLowerVisibilityThanProperty(): Boolean {
    val p = property
    return when {
        p.hasModifier(KtTokens.PRIVATE_KEYWORD) ->
            false
        p.hasModifier(KtTokens.PROTECTED_KEYWORD) ->
            hasModifier(KtTokens.PRIVATE_KEYWORD)
        p.hasModifier(KtTokens.INTERNAL_KEYWORD) ->
            hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.PROTECTED_KEYWORD)
        else ->
            hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.PROTECTED_KEYWORD) || hasModifier(KtTokens.INTERNAL_KEYWORD)
    }
}

private fun KtExpression.isFieldText(): Boolean = this.textMatches("field")

private class RemoveRedundantSetterFix : LocalQuickFix {
    override fun getName() = "Remove redundant setter"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val accessor = descriptor.psiElement as? KtPropertyAccessor ?: return
        accessor.delete()
    }
}