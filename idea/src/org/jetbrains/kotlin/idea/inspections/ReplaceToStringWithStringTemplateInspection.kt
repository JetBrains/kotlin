/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceToStringWithStringTemplateInspection : AbstractApplicabilityBasedInspection<KtDotQualifiedExpression>(
        KtDotQualifiedExpression::class.java
) {
    override fun isApplicable(element: KtDotQualifiedExpression): Boolean {
        if (element.receiverExpression !is KtReferenceExpression) return false
        if (element.parent is KtBlockStringTemplateEntry) return false
        return element.isToString()
    }

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val expression = element.getParentOfType<KtDotQualifiedExpression>(strict = false) ?: return
        val variable = expression.receiverExpression.text
        element.replace(KtPsiFactory(element).createExpression("\"$$variable\""))
    }

    override fun inspectionText(element: KtDotQualifiedExpression) = "Should be replaced 'toString' with string template"

    override fun inspectionTarget(element: KtDotQualifiedExpression) = element

    override val defaultFixText = "Replace 'toString' with string template"

    private fun KtDotQualifiedExpression.isToString(): Boolean {
        val resolvedCall = toResolvedCall(BodyResolveMode.PARTIAL) ?: return false
        val callableDescriptor = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return false
        return callableDescriptor.getDeepestSuperDeclarations().any { it.fqNameUnsafe.asString() == "kotlin.Any.toString" }
    }
}