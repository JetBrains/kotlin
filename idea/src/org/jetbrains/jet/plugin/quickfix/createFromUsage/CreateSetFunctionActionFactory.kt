package org.jetbrains.jet.plugin.quickfix.createFromUsage

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import org.jetbrains.jet.lang.types.Variance
import java.util.ArrayList
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

object CreateSetFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetArrayAccessExpression>()) ?: return null
        val arrayExpr = accessExpr.getArrayExpression() ?: return null
        val arrayType = TypeOrExpressionThereof(arrayExpr, Variance.IN_VARIANCE)

        val parameters = accessExpr.getIndexExpressions().mapTo(ArrayList<Parameter>()) {
            Parameter(TypeOrExpressionThereof(it, Variance.IN_VARIANCE))
        }

        val assignmentExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetBinaryExpression>()) ?: return null
        val rhs = assignmentExpr.getRight() ?: return null
        val valType = TypeOrExpressionThereof(rhs, Variance.IN_VARIANCE)
        parameters.add(Parameter(valType, "value"))

        val returnType = TypeOrExpressionThereof(KotlinBuiltIns.getInstance().getUnitType(), Variance.OUT_VARIANCE)
        return CreateFunctionFromUsageFix(accessExpr, arrayType, "set", returnType, parameters)
    }
}