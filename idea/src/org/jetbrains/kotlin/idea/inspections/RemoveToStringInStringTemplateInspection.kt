/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.canPlaceAfterSimpleNameEntry
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RemoveToStringInStringTemplateInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            dotQualifiedExpressionVisitor(fun(expression) {
                if (expression.parent !is KtBlockStringTemplateEntry) return
                if (expression.receiverExpression is KtSuperExpression) return
                val selectorExpression = expression.selectorExpression ?: return
                if (!expression.isToString()) return

                holder.registerProblem(selectorExpression,
                                       "Redundant 'toString()' call in string template",
                                       ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                       RemoveToStringFix())
            })
}

private fun KtDotQualifiedExpression.isToString(): Boolean {
    val resolvedCall = toResolvedCall(BodyResolveMode.PARTIAL) ?: return false
    val callableDescriptor = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return false
    return callableDescriptor.getDeepestSuperDeclarations().any { it.fqNameUnsafe.asString() == "kotlin.Any.toString" }
}

class RemoveToStringFix: LocalQuickFix {
    override fun getName() = "Remove 'toString()' call"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement.parent as? KtDotQualifiedExpression ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(element)) return

        val receiverExpression = element.receiverExpression
        if (receiverExpression is KtNameReferenceExpression) {
            val templateEntry = receiverExpression.parent.parent
            if (templateEntry is KtBlockStringTemplateEntry && canPlaceAfterSimpleNameEntry(templateEntry.nextSibling)) {

                val factory = KtPsiFactory(templateEntry)
                templateEntry.replace(factory.createSimpleNameStringTemplateEntry(receiverExpression.getReferencedName()))
                return
            }
        }
        element.replace(receiverExpression)
    }
}
