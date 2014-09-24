package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.calls.callUtil.getCall
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.Qualifier

object CreateFunctionFromCallActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val callExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetCallExpression>()) ?: return null

        val calleeExpr = callExpr.getCalleeExpression() as? JetSimpleNameExpression ?: return null
        if (calleeExpr.getReferencedNameElementType() != JetTokens.IDENTIFIER) return null

        val callParent = callExpr.getParent()
        val fullCallExpr =
                if (callParent is JetQualifiedExpression && callParent.getSelectorExpression() == callExpr) callParent else callExpr

        val context = AnalyzerFacadeWithCache.getContextForElement(callExpr)
        val receiver = callExpr.getCall(context)?.getExplicitReceiver() ?: return null

        val receiverType = when (receiver) {
            ReceiverValue.NO_RECEIVER -> TypeInfo.Empty
            is Qualifier -> when {
                receiver.classifier != null -> TypeInfo(receiver.expression, Variance.IN_VARIANCE)
                else -> return null
            }
            else -> TypeInfo(receiver.getType(), Variance.IN_VARIANCE)
        }

        val anyType = KotlinBuiltIns.getInstance().getNullableAnyType()
        val parameters = callExpr.getValueArguments().map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.getReferenceExpression()?.getReferencedName()
            )
        }

        val returnType = TypeInfo(fullCallExpr, Variance.OUT_VARIANCE)
        return CreateFunctionFromUsageFix(callExpr, FunctionInfo(calleeExpr.getReferencedName(), receiverType, returnType, parameters))
    }
}