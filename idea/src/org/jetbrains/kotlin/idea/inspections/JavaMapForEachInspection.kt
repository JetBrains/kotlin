/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.getLastLambdaExpression
import org.jetbrains.kotlin.idea.inspections.collections.isMap
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor

class JavaMapForEachInspection : AbstractApplicabilityBasedInspection<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java
) {
    override fun isApplicable(element: KtDotQualifiedExpression): Boolean {
        val callExpression = element.callExpression ?: return false
        val calleeExpression = callExpression.calleeExpression ?: return false
        if (calleeExpression.text != "forEach") return false

        val lambda = callExpression.lambda() ?: return false
        if (lambda.valueParameters.size != 2) return false

        val context = element.analyze(BodyResolveMode.PARTIAL)
        if (!element.receiverExpression.getType(context).isMap(DefaultBuiltIns.Instance)) return false
        return callExpression.getResolvedCall(context)?.resultingDescriptor is SamAdapterExtensionFunctionDescriptor
    }

    override fun inspectionTarget(element: KtDotQualifiedExpression) = element.callExpression?.calleeExpression ?: element

    override fun inspectionText(element: KtDotQualifiedExpression) = "Java Map.forEach method call should be replaced with Kotlin's forEach"

    override fun inspectionHighlightType(element: KtDotQualifiedExpression) = ProblemHighlightType.GENERIC_ERROR_OR_WARNING

    override val defaultFixText = "Replace with Kotlin's forEach"

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val call = element.getStrictParentOfType<KtCallExpression>() ?: return
        val lambda = call.lambda() ?: return
        val valueParameters = lambda.valueParameters
        lambda.functionLiteral.valueParameterList?.replace(
            KtPsiFactory(call).createLambdaParameterList("(${valueParameters[0].text}, ${valueParameters[1].text})")
        )
    }

    private fun KtCallExpression.lambda(): KtLambdaExpression? {
        return lambdaArguments.singleOrNull()?.getArgumentExpression() as? KtLambdaExpression ?: getLastLambdaExpression()
    }
}