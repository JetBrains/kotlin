/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.util.start
import org.jetbrains.kotlin.idea.core.util.end
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList

class AddNamesToFollowingArgumentsIntention : SelfTargetingIntention<KtValueArgument>(
    KtValueArgument::class.java, "Add names to this argument and following arguments"
), LowPriorityAction {
    override fun isApplicableTo(element: KtValueArgument, caretOffset: Int): Boolean {
        val argumentList = element.parent as? KtValueArgumentList ?: return false
        // Shadowed by AddNamesToCallArguments
        if (argumentList.arguments.firstOrNull() == element) return false
        // Shadowed by AddNameToArgument
        if (argumentList.arguments.lastOrNull { !it.isNamed() } == element) return false
        val expression = element.getArgumentExpression() ?: return false
        AddNameToArgumentIntention.detectNameToAdd(element, shouldBeLastUnnamed = false) ?: return false

        if (expression is KtLambdaExpression) {
            val range = expression.textRange
            return caretOffset == range.start || caretOffset == range.end
        }

        return true
    }

    override fun applyTo(element: KtValueArgument, editor: Editor?) {
        val argumentList = element.parent as? KtValueArgumentList ?: return
        val callElement = argumentList.parent as? KtCallElement ?: return
        val resolvedCall = callElement.resolveToCall() ?: return
        for (argument in argumentList.arguments.dropWhile { it != element }) {
            AddNameToArgumentIntention.apply(argument, resolvedCall)
        }
    }
}