/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.AddNameToArgumentIntention
import org.jetbrains.kotlin.idea.intentions.AddNamesToCallArgumentsIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class BooleanLiteralArgumentInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        valueArgumentVisitor(fun(argument: KtValueArgument) {
            if (argument.getArgumentName() != null) return
            val argumentExpression = argument.getArgumentExpression() as? KtConstantExpression ?: return
            if (argumentExpression.node.elementType != KtNodeTypes.BOOLEAN_CONSTANT) return
            if (argumentExpression.analyze().diagnostics.forElement(argumentExpression).any { it.severity == Severity.ERROR }) return
            val call = argument.getStrictParentOfType<KtCallExpression>() ?: return
            if (call.resolveToCall()?.resultingDescriptor?.hasStableParameterNames() != true) return

            val file = argument.containingKtFile
            val description = "Boolean literal arguments"
            val highlightType = GENERIC_ERROR_OR_WARNING
            when {
                call.valueArguments.takeLastWhile { it != argument }.none { !it.isNamed() } ->
                    holder.registerProblem(
                        argument,
                        description,
                        highlightType,
                        IntentionWrapper(AddNameToArgumentIntention(), file)
                    )
                AddNamesToCallArgumentsIntention.canAddNamesToCallArguments(call) ->
                    holder.registerProblem(
                        holder.manager.createProblemDescriptor(
                            call,
                            argument.textRange.shiftRight(-call.startOffset),
                            description,
                            highlightType,
                            isOnTheFly,
                            IntentionWrapper(AddNamesIntention(), file)
                        )
                    )
            }
        })

    private class AddNamesIntention : AddNamesToCallArgumentsIntention() {
        override fun applicabilityRange(element: KtCallElement): TextRange? = element.textRange
    }
}
