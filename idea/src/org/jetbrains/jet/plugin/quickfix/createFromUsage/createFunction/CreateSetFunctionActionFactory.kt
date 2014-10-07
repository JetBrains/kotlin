package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import org.jetbrains.jet.lang.types.Variance
import java.util.ArrayList
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*
import java.util.Collections

object CreateSetFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetArrayAccessExpression>()) ?: return null
        val arrayExpr = accessExpr.getArrayExpression() ?: return null
        val arrayType = TypeInfo(arrayExpr, Variance.IN_VARIANCE)

        val parameters = accessExpr.getIndexExpressions().mapTo(ArrayList<ParameterInfo>()) {
            ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE))
        }

        val assignmentExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetBinaryExpression>()) ?: return null
        val rhs = assignmentExpr.getRight() ?: return null
        val valType = TypeInfo(rhs, Variance.IN_VARIANCE)
        parameters.add(ParameterInfo(valType, "value"))

        val returnType = TypeInfo(KotlinBuiltIns.getInstance().getUnitType(), Variance.OUT_VARIANCE)
        return CreateCallableFromUsageFix(accessExpr, FunctionInfo("set", arrayType, returnType, Collections.emptyList(), parameters))
    }
}