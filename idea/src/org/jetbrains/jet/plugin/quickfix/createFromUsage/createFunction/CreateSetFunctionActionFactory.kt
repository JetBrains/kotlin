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
import org.jetbrains.jet.lang.psi.JetOperationExpression
import org.jetbrains.jet.lang.psi.JetUnaryExpression
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lang.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.types.ErrorUtils

object CreateSetFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetArrayAccessExpression>()) ?: return null
        val arrayExpr = accessExpr.getArrayExpression() ?: return null
        val arrayType = TypeInfo(arrayExpr, Variance.IN_VARIANCE)

        val builtIns = KotlinBuiltIns.getInstance()

        val parameters = accessExpr.getIndexExpressions().mapTo(ArrayList<ParameterInfo>()) {
            ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE))
        }

        val assignmentExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetOperationExpression>()) ?: return null
        val valType = when (assignmentExpr) {
            is JetBinaryExpression -> {
                TypeInfo(assignmentExpr.getRight() ?: return null, Variance.IN_VARIANCE)
            }
            is JetUnaryExpression -> {
                if (assignmentExpr.getOperationToken() !in OperatorConventions.INCREMENT_OPERATIONS) return null

                val context = AnalyzerFacadeWithCache.getContextForElement(assignmentExpr)
                val rhsType = assignmentExpr.getResolvedCall(context)?.getResultingDescriptor()?.getReturnType()
                TypeInfo(if (rhsType == null || ErrorUtils.containsErrorType(rhsType)) builtIns.getAnyType() else rhsType, Variance.IN_VARIANCE)
            }
            else -> return null
        }
        parameters.add(ParameterInfo(valType, "value"))

        val returnType = TypeInfo(builtIns.getUnitType(), Variance.OUT_VARIANCE)
        return CreateCallableFromUsageFix(accessExpr, FunctionInfo("set", arrayType, returnType, Collections.emptyList(), parameters))
    }
}