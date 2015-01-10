package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.types.Variance
import java.util.Collections
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*

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
            inOperation -> TypeInfo.ByType(builtIns.getBooleanType(), Variance.INVARIANT).noSubstitutions()
            comparisonOperation -> TypeInfo.ByType(builtIns.getIntType(), Variance.INVARIANT).noSubstitutions()
            else -> TypeInfo(callExpr, Variance.OUT_VARIANCE)
        }
        val parameters = Collections.singletonList(ParameterInfo(TypeInfo(argumentExpr, Variance.IN_VARIANCE)))
        return CreateCallableFromUsageFix(callExpr, FunctionInfo(operationName, receiverType, returnType, Collections.emptyList(), parameters))
    }
}
