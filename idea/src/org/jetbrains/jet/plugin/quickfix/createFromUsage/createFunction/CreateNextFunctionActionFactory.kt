package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.lang.psi.JetForExpression

object CreateNextFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val diagnosticWithParameters = DiagnosticFactory.cast(diagnostic, Errors.NEXT_MISSING, Errors.NEXT_NONE_APPLICABLE)
        val ownerType = TypeInfo(diagnosticWithParameters.getA(), Variance.IN_VARIANCE)

        val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>()) ?: return null
        val variableExpr: JetExpression = ((forExpr.getLoopParameter() ?: forExpr.getMultiParameter()) ?: return null) as JetExpression
        val returnType = TypeInfo(variableExpr, Variance.OUT_VARIANCE)
        return CreateFunctionFromUsageFix(forExpr, FunctionInfo("next", ownerType, returnType))
    }
}