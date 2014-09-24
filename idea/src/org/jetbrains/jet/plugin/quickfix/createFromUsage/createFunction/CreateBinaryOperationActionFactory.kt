package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.lang.types.Variance
import java.util.Collections
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lexer.JetTokens

public object CreateBinaryOperationActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val callExpr = diagnostic.getPsiElement().getParent() as? JetBinaryExpression ?: return null
        val token = callExpr.getOperationToken() as JetToken
        val operationName = when (token) {
            JetTokens.IDENTIFIER -> callExpr.getOperationReference().getReferencedName()
            else -> OperatorConventions.getNameForOperationSymbol(token)?.asString()
        } ?: return null
        val inOperation = token in OperatorConventions.IN_OPERATIONS
        val comparisonOperation = token in OperatorConventions.COMPARISON_OPERATIONS

        val leftExpr = callExpr.getLeft() ?: return null
        val rightExpr = callExpr.getRight() ?: return null

        val receiverExpr = if (inOperation) rightExpr else leftExpr
        val argumentExpr = if (inOperation) leftExpr else rightExpr

        val builtIns = KotlinBuiltIns.getInstance()

        val receiverType = TypeInfo(receiverExpr, Variance.IN_VARIANCE)
        val returnType = when {
            inOperation -> TypeInfo.ByType(builtIns.getBooleanType(), Variance.INVARIANT, true)
            comparisonOperation -> TypeInfo.ByType(builtIns.getIntType(), Variance.INVARIANT, true)
            else -> TypeInfo(callExpr, Variance.OUT_VARIANCE)
        }
        val parameters = Collections.singletonList(ParameterInfo(TypeInfo(argumentExpr, Variance.IN_VARIANCE)))
        return CreateFunctionFromUsageFix(callExpr, FunctionInfo(operationName, receiverType, returnType, parameters))
    }
}