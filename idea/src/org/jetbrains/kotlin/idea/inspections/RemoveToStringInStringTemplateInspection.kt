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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.canPlaceAfterSimpleNameEntry
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RemoveToStringInStringTemplateInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    val qualifiedExpression = expression.parent as? KtDotQualifiedExpression ?: return
                    if (qualifiedExpression.parent !is KtBlockStringTemplateEntry) return
                    if (qualifiedExpression.receiverExpression is KtSuperExpression) return
                    if (!qualifiedExpression.isToString()) return

                    holder.registerProblem(expression,
                                           "Redundant 'toString()' call in string template",
                                           ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                           RemoveToStringFix())
                }

                private fun KtDotQualifiedExpression.isToString(): Boolean {
                    val resolvedCall = toResolvedCall(BodyResolveMode.PARTIAL) ?: return false
                    val callableDescriptor = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return false
                    return callableDescriptor.getDeepestSuperDeclarations().any { it.fqNameUnsafe.asString() == "kotlin.Any.toString" }
                }
            }
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
