/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.INFORMATION
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.AddNamesToCallArgumentsIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch

class BooleanLiteralArgumentInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        valueArgumentVisitor(fun(argument: KtValueArgument) {
            if (argument.getArgumentName() != null) return
            val argumentExpression = argument.getArgumentExpression() as? KtConstantExpression ?: return
            if (argumentExpression.node.elementType != KtNodeTypes.BOOLEAN_CONSTANT) return
            if (argumentExpression.analyze().diagnostics.forElement(argumentExpression).any { it.severity == Severity.ERROR }) return
            val call = argument.getStrictParentOfType<KtCallElement>() ?: return
            if (call.resolveToCall()?.resultingDescriptor?.hasStableParameterNames() != true) return

            val (highlightType, fix) = when {
                call.valueArguments.takeLastWhile { it != argument }.none { !it.isNamed() } -> GENERIC_ERROR_OR_WARNING to AddNameFix()
                AddNamesToCallArgumentsIntention.canAddNamesToCallArguments(call) -> INFORMATION to AddNamesToCallArgumentsFix()
                else -> null
            } ?: return

            holder.registerProblem(
                argument,
                "Boolean literal arguments",
                highlightType,
                fix
            )
        })

    private class AddNameFix : LocalQuickFix {
        override fun getName() = "Add name"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val argument = descriptor.psiElement as? KtValueArgument ?: return
            val call = argument.getStrictParentOfType<KtCallElement>() ?: return
            val argumentMatch = call.resolveToCall()?.getArgumentMapping(argument) as? ArgumentMatch ?: return
            val newArgument = KtPsiFactory(argument).createArgument(
                argument.getArgumentExpression(),
                argumentMatch.valueParameter.name
            )
            argument.replace(newArgument)
        }
    }

    private class AddNamesToCallArgumentsFix : LocalQuickFix {
        override fun getName() = "Add names to call arguments"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val argument = descriptor.psiElement as? KtValueArgument ?: return
            val call = argument.getStrictParentOfType<KtCallElement>() ?: return
            AddNamesToCallArgumentsIntention.addNamesToCallArguments(call)
        }
    }
}