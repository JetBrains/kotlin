/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.INFORMATION
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
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
            if (argumentExpression.analyze().diagnostics.forElement(argumentExpression).any { it.severity == Severity.ERROR }) return
            val call = argument.getStrictParentOfType<KtCallExpression>() ?: return
            if (call.resolveToCall()?.resultingDescriptor?.hasStableParameterNames() != true) return

            val valueArguments = call.valueArguments
            fun hasAnotherUnnamedBoolean() = valueArguments.asSequence().filter { it != argument }.any {
                !it.isNamed() && (it.getArgumentExpression() as? KtConstantExpression)?.node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT
            }
            when {
                valueArguments.takeLastWhile { it != argument }.none { !it.isNamed() } ->
                    holder.registerProblem(
                        argument,
                        "Boolean literal argument without parameter name",
                        if (reportSingle || hasAnotherUnnamedBoolean()) GENERIC_ERROR_OR_WARNING else INFORMATION,
                        IntentionWrapper(AddNameToArgumentIntention(), argument.containingKtFile)
                    )
            }
        })

    override fun createOptionsPanel(): JComponent? {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Report also on call with single boolean literal argument", "reportSingle")
        return panel
    }
}
