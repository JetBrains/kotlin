/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isElseIf
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.unwrapBlockOrParenthesis
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ConstantConditionIfInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return ifExpressionVisitor { expression ->
            val constantValue = expression.getConditionConstantValueIfAny() ?: return@ifExpressionVisitor
            val fixes = collectFixes(expression, constantValue)
            holder.registerProblem(
                expression.condition!!,
                "Condition is always '$constantValue'",
                *fixes.toTypedArray()
            )
        }
    }

    companion object {
        private fun KtIfExpression.getConditionConstantValueIfAny(): Boolean? {
            val context = condition?.analyze(BodyResolveMode.PARTIAL_WITH_CFA) ?: return null
            return condition?.constantBooleanValue(context)
        }

        private fun collectFixes(
            expression: KtIfExpression,
            constantValue: Boolean? = expression.getConditionConstantValueIfAny()
        ): List<ConstantConditionIfFix> {
            if (constantValue == null) return emptyList()
            val fixes = mutableListOf<ConstantConditionIfFix>()

            if (expression.branch(constantValue) != null) {
                val keepBraces = expression.isElseIf() && expression.branch(constantValue) is KtBlockExpression
                fixes += SimplifyFix(
                    constantValue,
                    expression.isUsedAsExpression(expression.analyze(BodyResolveMode.PARTIAL_WITH_CFA)),
                    keepBraces
                )
            }

            if (!constantValue && expression.`else` == null) {
                fixes += RemoveFix()
            }

            return fixes
        }

        fun applyFixIfSingle(ifExpression: KtIfExpression) {
            collectFixes(ifExpression).singleOrNull()?.applyFix(ifExpression)
        }
    }

    private interface ConstantConditionIfFix : LocalQuickFix {
        fun applyFix(ifExpression: KtIfExpression)
    }

    private class SimplifyFix(
        private val conditionValue: Boolean,
        private val isUsedAsExpression: Boolean,
        private val keepBraces: Boolean
    ) : ConstantConditionIfFix {
        override fun getFamilyName() = name

        override fun getName() = "Simplify expression"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val ifExpression = descriptor.psiElement.getParentOfType<KtIfExpression>(strict = true) ?: return
            applyFix(ifExpression)
        }

        override fun applyFix(ifExpression: KtIfExpression) {
            val branch = ifExpression.branch(conditionValue)?.let {
                if (keepBraces) it else it.unwrapBlockOrParenthesis()
            } ?: return
            ifExpression.replaceWithBranch(branch, isUsedAsExpression, keepBraces)
        }
    }

    private class RemoveFix : ConstantConditionIfFix {
        override fun getFamilyName() = name

        override fun getName() = "Delete expression"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val ifExpression = descriptor.psiElement.getParentOfType<KtIfExpression>(strict = true) ?: return
            applyFix(ifExpression)
        }

        override fun applyFix(ifExpression: KtIfExpression) {
            ifExpression.delete()
        }
    }
}

private fun KtIfExpression.branch(thenBranch: Boolean) = if (thenBranch) then else `else`

private fun KtExpression.constantBooleanValue(context: BindingContext): Boolean? {
    val enumEntriesComparison = enumEntriesComparison()
    if (enumEntriesComparison != null) {
        return enumEntriesComparison
    }
    val type = getType(context) ?: return null
    val constantValue = ConstantExpressionEvaluator.getConstant(this, context)?.toConstantValue(type)
    return constantValue?.value as? Boolean
}

private fun KtExpression.enumEntriesComparison(): Boolean? {
    if (this !is KtBinaryExpression) return null
    val leftEnumEntry = left?.enumEntry() ?: return null
    val rightEnumEntry = right?.enumEntry() ?: return null

    val leftEnum = leftEnumEntry.containingClass() ?: return null
    val rightEnum = rightEnumEntry.containingClass() ?: return null
    if (leftEnum != rightEnum) return null

    val enumEntries = leftEnum.body?.getChildrenOfType<KtEnumEntry>() ?: return null
    val leftIndex = enumEntries.indexOf(leftEnumEntry)
    val rightIndex = enumEntries.indexOf(rightEnumEntry)
    return when (operationToken) {
        KtTokens.EQEQ -> leftIndex == rightIndex
        KtTokens.GT -> leftIndex > rightIndex
        KtTokens.GTEQ -> leftIndex >= rightIndex
        KtTokens.LT -> leftIndex < rightIndex
        KtTokens.LTEQ -> leftIndex <= rightIndex
        KtTokens.EXCLEQ -> leftIndex != rightIndex
        else -> null
    }
}

private fun KtExpression.enumEntry(): KtEnumEntry? {
    return ((this as? KtDotQualifiedExpression)?.selectorExpression ?: this).mainReference?.resolve() as? KtEnumEntry
}

fun KtExpression.replaceWithBranch(branch: KtExpression, isUsedAsExpression: Boolean, keepBraces: Boolean = false) {
    val lastExpression = when {
        branch !is KtBlockExpression -> replaced(branch)
        isUsedAsExpression -> {
            val factory = KtPsiFactory(this)
            replaced(factory.createExpressionByPattern("run $0", branch.text))
        }
        else -> {
            val firstChildSibling = branch.firstChild.nextSibling
            val lastChild = branch.lastChild
            if (firstChildSibling != lastChild) {
                if (keepBraces) {
                    parent.addAfter(branch, this)
                } else {
                    parent.addRangeAfter(firstChildSibling, lastChild.prevSibling, this)
                }
            }
            delete()
            null
        }
    }

    val caretModel = branch.findExistingEditor()?.caretModel
    caretModel?.moveToOffset(lastExpression?.startOffset ?: return)
}