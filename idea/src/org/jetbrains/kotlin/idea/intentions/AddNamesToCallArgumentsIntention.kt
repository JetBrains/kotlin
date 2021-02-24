/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.LambdaArgument
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class AddNamesToCallArgumentsIntention : SelfTargetingRangeIntention<KtCallElement>(
    KtCallElement::class.java,
    KotlinBundle.lazyMessage("add.names.to.call.arguments")
) {
    override fun applicabilityRange(element: KtCallElement): TextRange? {
        val arguments = element.valueArguments
        if (arguments.all { it.isNamed() || it is LambdaArgument }) return null

        val resolvedCall = element.resolveToCall() ?: return null
        if (!resolvedCall.candidateDescriptor.hasStableParameterNames()) return null

        if (arguments.all {
                AddNameToArgumentIntention.argumentMatchedAndCouldBeNamedInCall(it, resolvedCall, element.languageVersionSettings)
            }
        ) {
            val calleeExpression = element.calleeExpression ?: return null
            if (arguments.size < 2) return calleeExpression.textRange
            val endOffset = (arguments.firstOrNull() as? KtValueArgument)?.endOffset ?: return null
            return TextRange(calleeExpression.startOffset, endOffset)
        }

        return null
    }

    override fun applyTo(element: KtCallElement, editor: Editor?) {
        val arguments = element.valueArguments
        val resolvedCall = element.resolveToCall() ?: return
        for (argument in arguments) {
            if (argument !is KtValueArgument || argument is KtLambdaArgument) continue
            AddNameToArgumentIntention.apply(argument, resolvedCall)
        }
    }
}