/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

class RecursivePropertyAccessorInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return simpleNameExpressionVisitor { expression ->
            if (isRecursivePropertyAccess(expression)) {
                holder.registerProblem(
                    expression,
                    "Recursive property accessor",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ReplaceWithFieldFix()
                )
            } else if (isRecursiveSyntheticPropertyAccess(expression)) {
                holder.registerProblem(
                    expression,
                    "Recursive synthetic property accessor",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        }
    }

    class ReplaceWithFieldFix : LocalQuickFix {

        override fun getName() = "Replace with 'field'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as KtExpression
            val factory = KtPsiFactory(expression)
            expression.replace(factory.createExpression("field"))
        }
    }

    companion object {

        private fun KtBinaryExpression?.isAssignmentTo(expression: KtSimpleNameExpression): Boolean =
            this != null && KtPsiUtil.isAssignment(this) && PsiTreeUtil.isAncestor(left, expression, false)

        private fun isSameAccessor(expression: KtSimpleNameExpression, isGetter: Boolean): Boolean {
            val binaryExpr = expression.getStrictParentOfType<KtBinaryExpression>()
            if (isGetter) {
                if (binaryExpr.isAssignmentTo(expression)) {
                    return KtTokens.AUGMENTED_ASSIGNMENTS.contains(binaryExpr?.operationToken)
                }
                return true
            } else /* isSetter */ {
                if (binaryExpr.isAssignmentTo(expression)) {
                    return true
                }
                val unaryExpr = expression.getStrictParentOfType<KtUnaryExpression>()
                if (unaryExpr?.operationToken.let { it == KtTokens.PLUSPLUS || it == KtTokens.MINUSMINUS }) {
                    return true
                }
            }
            return false
        }

        fun isRecursivePropertyAccess(element: KtElement): Boolean {
            if (element !is KtSimpleNameExpression) return false
            val propertyAccessor = element.getParentOfType<KtDeclarationWithBody>(true) as? KtPropertyAccessor ?: return false
            if (element.text != propertyAccessor.property.name) return false
            if (element.parent is KtCallableReferenceExpression) return false
            val bindingContext = element.analyze()
            val target = bindingContext[REFERENCE_TARGET, element]
            if (target != bindingContext[DECLARATION_TO_DESCRIPTOR, propertyAccessor.property]) return false
            (element.parent as? KtQualifiedExpression)?.let {
                if (it.receiverExpression.text != KtTokens.THIS_KEYWORD.value && !it.hasObjectReceiver(bindingContext)) return false
            }
            return isSameAccessor(element, propertyAccessor.isGetter)
        }

        fun isRecursiveSyntheticPropertyAccess(element: KtElement): Boolean {
            if (element !is KtSimpleNameExpression) return false
            val namedFunction = element.getParentOfType<KtDeclarationWithBody>(true) as? KtNamedFunction ?: return false
            val name = namedFunction.name ?: return false
            val referencedName = element.text.capitalize()
            val isGetter = name == "get$referencedName"
            val isSetter = name == "set$referencedName"
            if (!isGetter && !isSetter) return false
            if (element.parent is KtCallableReferenceExpression) return false
            val bindingContext = element.analyze()
            val syntheticDescriptor = bindingContext[REFERENCE_TARGET, element] as? SyntheticJavaPropertyDescriptor ?: return false
            val namedFunctionDescriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, namedFunction]
            if (namedFunctionDescriptor != syntheticDescriptor.getMethod &&
                namedFunctionDescriptor != syntheticDescriptor.setMethod
            ) return false
            return isSameAccessor(element, isGetter)
        }

        private fun KtQualifiedExpression.hasObjectReceiver(context: BindingContext): Boolean {
            val receiver = receiverExpression as? KtReferenceExpression ?: return false
            return (context[REFERENCE_TARGET, receiver] as? ClassDescriptor)?.kind == ClassKind.OBJECT
        }
    }
}