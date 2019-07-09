/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isOneLiner
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.countUsages
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.previousStatement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class MoveVariableDeclarationIntoWhenInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        whenExpressionVisitor(fun(expression: KtWhenExpression) {
            val subjectExpression = expression.subjectExpression ?: return
            val property = expression.findDeclarationNear() ?: return
            if (!property.isOneLiner()) return

            val action = property.action(expression)
            if (action == Action.NOTHING) return

            val identifier = property.nameIdentifier ?: return
            holder.registerProblem(
                property,
                TextRange.from(identifier.startOffsetInParent, identifier.textLength),
                action.description, action.createFix(subjectExpression.createSmartPointer())
            )
        })
}

private enum class Action {
    NOTHING,
    MOVE,
    INLINE;

    val description: String
        get() = when (this) {
            MOVE -> "Variable declaration could be moved into `when`"
            INLINE -> "Variable declaration could be inlined"
            NOTHING -> "Nothing to do"
        }

    fun createFix(subjectExpressionPointer: SmartPsiElementPointer<KtExpression>): VariableDeclarationIntoWhenFix = when (this) {
        MOVE -> VariableDeclarationIntoWhenFix("Move variable declaration into `when`", subjectExpressionPointer) { it }
        INLINE -> VariableDeclarationIntoWhenFix("Inline variable", subjectExpressionPointer) { it.initializer }
        else -> error("Illegal action")
    }
}

private fun KtProperty.action(element: KtElement): Action = when (val elementUsages = countUsages(element)) {
    countUsages() -> if (elementUsages == 1) Action.INLINE else Action.MOVE
    else -> Action.NOTHING
}

private fun KtWhenExpression.findDeclarationNear(): KtProperty? {
    val previousProperty = previousStatement() as? KtProperty
        ?: previousPropertyFromParent()
        ?: return null
    return previousProperty.takeIf { !it.isVar && it.hasInitializer() && it.nameIdentifier?.text == subjectExpression?.text }
}

private tailrec fun KtExpression.previousPropertyFromParent(): KtProperty? {
    val parentExpression = parent as? KtExpression ?: return null
    if (this != when (parentExpression) {
            is KtProperty -> parentExpression.initializer
            is KtReturnExpression -> parentExpression.returnedExpression
            is KtBinaryExpression -> parentExpression.left
            is KtUnaryExpression -> parentExpression.baseExpression
            else -> null
        }
    ) return null

    return parentExpression.previousStatement() as? KtProperty ?: parentExpression.previousPropertyFromParent()
}

private class VariableDeclarationIntoWhenFix(
    private val actionName: String,
    private val subjectExpressionPointer: SmartPsiElementPointer<KtExpression>,
    private val transform: (KtProperty) -> KtExpression?
) : LocalQuickFix {
    override fun getName() = actionName

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val property = descriptor.psiElement as? KtProperty ?: return
        val subjectExpression = subjectExpressionPointer.element ?: return
        val newElement = transform(property)?.copy() ?: return

        val lastChild = newElement.lastChild
        if (lastChild is PsiComment && lastChild.node.elementType == KtTokens.EOL_COMMENT) {
            val leftBrace = subjectExpression.siblings(withItself = false).firstOrNull { it.node.elementType == KtTokens.LBRACE }
            val whiteSpaceBeforeComment = lastChild.prevSibling?.takeIf { it is PsiWhiteSpace }
            if (leftBrace != null) {
                subjectExpression.parent.addAfter(lastChild, leftBrace)
                if (whiteSpaceBeforeComment != null) {
                    subjectExpression.parent.addAfter(whiteSpaceBeforeComment, leftBrace)
                }
            }
            whiteSpaceBeforeComment?.delete()
            lastChild.delete()
        }

        val resultElement = subjectExpression.replace(newElement)
        property.delete()

        val editor = resultElement.findExistingEditor() ?: return
        editor.moveCaret((resultElement as? KtProperty)?.nameIdentifier?.startOffset ?: resultElement.startOffset)
    }
}