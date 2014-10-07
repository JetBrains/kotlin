package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*
import java.util.Collections

object CreateInvokeFunctionActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val callExpr = diagnostic.getPsiElement().getParent() as? JetCallExpression ?: return null

        val expectedType = Errors.FUNCTION_EXPECTED.cast(diagnostic).getB()
        if (expectedType.isError()) return null

        val receiverType = TypeInfo(expectedType, Variance.IN_VARIANCE)

        val anyType = KotlinBuiltIns.getInstance().getNullableAnyType()
        val parameters = callExpr.getValueArguments().map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.getReferenceExpression()?.getReferencedName()
            )
        }

        val returnType = TypeInfo(callExpr, Variance.OUT_VARIANCE)
        return CreateCallableFromUsageFix(callExpr, FunctionInfo("invoke", receiverType, returnType, Collections.emptyList(), parameters))
    }
}
