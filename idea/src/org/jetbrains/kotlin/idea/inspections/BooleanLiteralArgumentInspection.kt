/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.INFORMATION
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.AddNameToArgumentIntention
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.valueArgumentVisitor
import javax.swing.JComponent

class BooleanLiteralArgumentInspection(
    @JvmField var reportSingle: Boolean = false
) : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        valueArgumentVisitor(fun(argument: KtValueArgument) {
            if (argument.getArgumentName() != null) return
            val argumentExpression = argument.getArgumentExpression() as? KtConstantExpression ?: return
            if (argumentExpression.node.elementType != KtNodeTypes.BOOLEAN_CONSTANT) return
            val call = argument.getStrictParentOfType<KtCallExpression>() ?: return
            val valueArguments = call.valueArguments
            if (valueArguments.takeLastWhile { it != argument }.any { !it.isNamed() }) return

            if (argumentExpression.analyze().diagnostics.forElement(argumentExpression).any { it.severity == Severity.ERROR }) return
            if (AddNameToArgumentIntention.detectNameToAdd(argument) == null) return

            val hasPreviousUnnamedBoolean = valueArguments.asSequence().windowed(size = 2, step = 1).any { (prev, next) ->
                next == argument && !prev.isNamed() &&
                        (prev.getArgumentExpression() as? KtConstantExpression)?.node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT
            }
            val fixes = mutableListOf<LocalQuickFix>()
            if (hasPreviousUnnamedBoolean) {
                fixes += AddNamesToLastBooleanArgumentsFix()
            }
            fixes += IntentionWrapper(AddNameToArgumentIntention(), argument.containingKtFile)
            holder.registerProblemWithoutOfflineInformation(
                argument,
                "Boolean literal argument without parameter name",
                isOnTheFly,
                if (reportSingle || hasPreviousUnnamedBoolean) GENERIC_ERROR_OR_WARNING else INFORMATION,
                *fixes.toTypedArray()
            )
        })

    override fun createOptionsPanel(): JComponent? {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Report also on call with single boolean literal argument", "reportSingle")
        return panel
    }

    private class AddNamesToLastBooleanArgumentsFix : LocalQuickFix {
        override fun getFamilyName(): String = name

        override fun getName() = "Add names to boolean arguments"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val argument = descriptor.psiElement as? KtValueArgument ?: return
            val call = argument.getStrictParentOfType<KtCallExpression>() ?: return
            val valueArguments = call.valueArguments

            var problemArgumentFound = false
            for (currentArgument in valueArguments.reversed()) {
                if (currentArgument == argument) {
                    problemArgumentFound = true
                } else if (!problemArgumentFound) continue
                if (currentArgument.isNamed()) return
                if ((currentArgument.getArgumentExpression() as? KtConstantExpression)?.node?.elementType != KtNodeTypes.BOOLEAN_CONSTANT) {
                    return
                }
                if (!AddNameToArgumentIntention.apply(currentArgument)) return
            }
        }
    }
}
