/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReplaceArraysCopyOfWithCopyOfInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)

                if (expression.isArraysCopyOf()) {
                    holder.registerProblem(
                        expression,
                        "Replace 'Arrays.copyOf' with 'copyOf'",
                        ProblemHighlightType.WEAK_WARNING,
                        ReplaceArraysCopyOfWithCopyOfQuickfix()
                    )
                }
            }
        }
    }
}

class ReplaceArraysCopyOfWithCopyOfQuickfix : LocalQuickFix {
    override fun getName() = "Replace 'Arrays.copyOf' with 'copyOf'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtDotQualifiedExpression
        val args = element.callExpression?.valueArguments?.mapNotNull { it.getArgumentExpression() }?.toTypedArray() ?: return
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0.copyOf($1)", *args))
    }
}

private fun KtDotQualifiedExpression.isArraysCopyOf(): Boolean {
    if (callExpression?.valueArguments?.size != 2) return false
    if (callExpression?.valueArguments?.mapNotNull { it.getArgumentExpression() }?.size != 2) return false
    if (calleeName != "copyOf") return false
    return getCallableDescriptor()?.containingDeclaration?.fqNameSafe == FqName("java.util.Arrays")
}