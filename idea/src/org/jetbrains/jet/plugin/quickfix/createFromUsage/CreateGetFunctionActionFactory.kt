package org.jetbrains.jet.plugin.quickfix.createFromUsage

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import org.jetbrains.jet.lang.types.Variance

object CreateGetFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetArrayAccessExpression>()) ?: return null
        val arrayExpr = accessExpr.getArrayExpression() ?: return null
        val arrayType = TypeOrExpressionThereof(arrayExpr, Variance.IN_VARIANCE)

        val parameters = accessExpr.getIndexExpressions().map {
            Parameter(TypeOrExpressionThereof(it, Variance.IN_VARIANCE))
        }

        val returnType = TypeOrExpressionThereof(accessExpr, Variance.OUT_VARIANCE)
        return CreateFunctionFromUsageFix(accessExpr, arrayType, "get", returnType, parameters)
    }
}