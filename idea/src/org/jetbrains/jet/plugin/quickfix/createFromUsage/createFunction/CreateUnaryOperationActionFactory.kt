package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.types.Variance
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetUnaryExpression
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*

public object CreateUnaryOperationActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val callExpr = diagnostic.getPsiElement().getParent() as? JetUnaryExpression ?: return null
        val token = callExpr.getOperationToken() as JetToken
        val operationName = OperatorConventions.getNameForOperationSymbol(token) ?: return null
        val incDec = token in OperatorConventions.INCREMENT_OPERATIONS

        val receiverExpr = callExpr.getBaseExpression() ?: return null

        val receiverType = TypeInfo(receiverExpr, Variance.IN_VARIANCE)
        val returnType = if (incDec) TypeInfo.ByReceiverType(Variance.OUT_VARIANCE) else TypeInfo(callExpr, Variance.OUT_VARIANCE)
        return CreateCallableFromUsageFix(callExpr, FunctionInfo(operationName.asString(), receiverType, returnType))
    }
}
