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
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.synthetic.isResolvedWithSamConversions

class JavaMapForEachInspection : AbstractApplicabilityBasedInspection<KtCallExpression>(
    KtCallExpression::class.java
) {
    override fun isApplicable(element: KtCallExpression): Boolean {
        val calleeExpression = element.calleeExpression ?: return false
        if (calleeExpression.text != "forEach") return false
        if (element.valueArguments.size != 1) return false

        val lambda = element.lambda() ?: return false
        val lambdaParameters = lambda.valueParameters
        if (lambdaParameters.size != 2 || lambdaParameters.any { it.destructuringDeclaration != null }) return false

        val context = element.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = element.getResolvedCall(context) ?: return false
        return resolvedCall.dispatchReceiver?.type?.isMap(DefaultBuiltIns.Instance) == true && resolvedCall.isResolvedWithSamConversions()
    }

    override fun inspectionHighlightRangeInElement(element: KtCallExpression): TextRange? = element.calleeExpression?.textRangeIn(element)

    override fun inspectionText(element: KtCallExpression) =
        KotlinBundle.message("java.map.foreach.method.call.should.be.replaced.with.kotlin.s.foreach")

    override val defaultFixText get() = KotlinBundle.message("replace.with.kotlin.s.foreach")

    override fun applyTo(element: KtCallExpression, project: Project, editor: Editor?) {
        val lambda = element.lambda() ?: return
        val valueParameters = lambda.valueParameters
        lambda.functionLiteral.valueParameterList?.replace(
            KtPsiFactory(element).createLambdaParameterList("(${valueParameters[0].text}, ${valueParameters[1].text})")
        )
    }

    private fun KtCallExpression.lambda(): KtLambdaExpression? {
        return lambdaArguments.singleOrNull()?.getArgumentExpression() as? KtLambdaExpression ?: getLastLambdaExpression()
    }
}