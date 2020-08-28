/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.getLastLambdaExpression
import org.jetbrains.kotlin.idea.inspections.collections.isMap
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.util.calleeTextRangeInThis
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.synthetic.isResolvedWithSamConversions

class JavaMapForEachInspection : AbstractApplicabilityBasedInspection<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java
) {
    override fun isApplicable(element: KtDotQualifiedExpression): Boolean {
        val callExpression = element.callExpression ?: return false
        val calleeExpression = callExpression.calleeExpression ?: return false
        if (calleeExpression.text != "forEach") return false
        if (callExpression.valueArguments.size != 1) return false

        val lambda = callExpression.lambda() ?: return false
        val lambdaParameters = lambda.valueParameters
        if (lambdaParameters.size != 2 || lambdaParameters.any { it.destructuringDeclaration != null }) return false

        val context = element.analyze(BodyResolveMode.PARTIAL)
        if (!element.receiverExpression.getType(context).isMap(DefaultBuiltIns.Instance)) return false
        val resolvedCall = callExpression.getResolvedCall(context) ?: return false
        return resolvedCall.isResolvedWithSamConversions()
    }

    override fun inspectionHighlightRangeInElement(element: KtDotQualifiedExpression): TextRange? = element.calleeTextRangeInThis()

    override fun inspectionText(element: KtDotQualifiedExpression) =
        KotlinBundle.message("java.map.foreach.method.call.should.be.replaced.with.kotlin.s.foreach")

    override val defaultFixText get() = KotlinBundle.message("replace.with.kotlin.s.foreach")

    override fun applyTo(element: KtDotQualifiedExpression, project: Project, editor: Editor?) {
        val call = element.callExpression ?: return
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