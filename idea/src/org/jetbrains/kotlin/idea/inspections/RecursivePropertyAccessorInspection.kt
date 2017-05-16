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

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets

class RecursivePropertyAccessorInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                super.visitSimpleNameExpression(expression)
                if (isRecursivePropertyAccess(expression)) {
                    holder.registerProblem(expression,
                                           "Recursive property accessor",
                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }
        }
    }

    companion object {
        fun isRecursivePropertyAccess(element: KtElement): Boolean {
            if (element !is KtSimpleNameExpression) return false
            val propertyAccessor = element.getParentOfType<KtPropertyAccessor>(true) ?: return false
            if (element.textMatches(KtTokens.FIELD_KEYWORD.value)) return false
            if (element.text != propertyAccessor.property.name) return false
            val bindingContext = element.analyze()
            val targets = element.getReferenceTargets(bindingContext)
            val target = targets.filterIsInstance<PropertyDescriptor>().firstOrNull() ?: return false
            if (DescriptorToSourceUtils.getSourceFromDescriptor(target) != propertyAccessor.property) return false
            if (propertyAccessor.isGetter) {
                val binaryExpr = element.getStrictParentOfType<KtBinaryExpression>()
                if (binaryExpr != null && KtPsiUtil.isAssignment(binaryExpr) && PsiTreeUtil.isAncestor(binaryExpr.left, element, false)) {
                    return KtTokens.AUGMENTED_ASSIGNMENTS.contains(binaryExpr.operationToken)
                }
                return true
            } else if (propertyAccessor.isSetter) {
                val binaryExpr = element.getStrictParentOfType<KtBinaryExpression>()
                if (binaryExpr != null && KtPsiUtil.isAssignment(binaryExpr) && PsiTreeUtil.isAncestor(binaryExpr.left, element, false)) {
                    return true
                }
                val unaryExpr = element.getStrictParentOfType<KtUnaryExpression>()
                if (unaryExpr != null && (unaryExpr.operationToken == KtTokens.PLUSPLUS || unaryExpr.operationToken == KtTokens.MINUSMINUS)) {
                    return true
                }
            }
            return false
        }
    }
}