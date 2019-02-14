/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.countUsages
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.previousStatement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class MoveVariableDeclarationIntoWhenInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        whenExpressionVisitor(fun(expression: KtWhenExpression) {
            val subjectExpression = expression.subjectExpression ?: return
            val property = expression.findDeclarationNear() ?: return
            if (!property.isUsedOnlyIn(expression)) return
            val identifier = property.nameIdentifier ?: return
            holder.registerProblem(
                property,
                TextRange.from(identifier.startOffsetInParent, identifier.textLength),
                "Variable declaration could be moved inside `when`",
                MoveVariableDeclarationIntoWhenFix(subjectExpression.createSmartPointer())
            )
        })
}

private fun KtProperty.isUsedOnlyIn(element: KtElement): Boolean = countUsages() == countUsages(element)

private fun KtWhenExpression.findDeclarationNear(): KtProperty? {
    val previousProperty = previousStatement() as? KtProperty ?: return null
    return previousProperty.takeIf { !it.isVar && it.hasInitializer() && it.nameIdentifier?.text == subjectExpression?.text }
}

private class MoveVariableDeclarationIntoWhenFix(val subjectExpressionPointer: SmartPsiElementPointer<KtExpression>) : LocalQuickFix {
    override fun getName() = "Move the variable into `when`"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val property = descriptor.psiElement as? KtProperty ?: return
        val subjectExpression = subjectExpressionPointer.element ?: return
        subjectExpression.replace(property.copy())
        property.delete()
    }
}
