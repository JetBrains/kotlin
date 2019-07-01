/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.config.LanguageFeature.ArrayLiteralsInAnnotations
import org.jetbrains.kotlin.idea.intentions.isArrayOfMethod
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class ReplaceArrayOfWithLiteralInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor(fun(expression) {
            if (!expression.languageVersionSettings.supportsFeature(ArrayLiteralsInAnnotations) &&
                !ApplicationManager.getApplication().isUnitTestMode) return

            val calleeExpression = expression.calleeExpression as? KtNameReferenceExpression ?: return
            if (!expression.isArrayOfMethod()) return

            val parent = expression.parent
            when (parent) {
                is KtValueArgument -> {
                    if (parent.parent.parent !is KtAnnotationEntry) return
                    if (parent.getSpreadElement() != null && !parent.isNamed()) return
                }
                is KtParameter -> {
                    val constructor = parent.parent.parent as? KtPrimaryConstructor ?: return
                    val containingClass = constructor.getContainingClassOrObject()
                    if (!containingClass.isAnnotation()) return
                }
                else -> return
            }

            val calleeName = calleeExpression.getReferencedName()
            holder.registerProblem(
                calleeExpression,
                "'$calleeName' call should be replaced with array literal [...]",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ReplaceWithArrayLiteralFix()
            )
        })
    }

    private class ReplaceWithArrayLiteralFix : LocalQuickFix {
        override fun getFamilyName() = "Replace with [...]"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val calleeExpression = descriptor.psiElement as KtExpression
            val callExpression = calleeExpression.parent as KtCallExpression

            val valueArgument = callExpression.getParentOfType<KtValueArgument>(false)
            valueArgument?.getSpreadElement()?.delete()

            val arguments = callExpression.valueArguments
            val arrayLiteral = KtPsiFactory(callExpression).buildExpression {
                appendFixedText("[")
                for ((index, argument) in arguments.withIndex()) {
                    appendExpression(argument.getArgumentExpression())
                    if (index != arguments.size - 1) {
                        appendFixedText(", ")
                    }
                }
                appendFixedText("]")
            } as KtCollectionLiteralExpression

            callExpression.replace(arrayLiteral)
        }
    }
}