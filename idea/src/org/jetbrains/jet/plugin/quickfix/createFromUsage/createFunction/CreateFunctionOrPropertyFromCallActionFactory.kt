package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.calls.callUtil.getCall
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.Qualifier
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetExpression
import java.util.Collections
import org.jetbrains.jet.plugin.refactoring.getExtractionContainers
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetTypeReference

object CreateFunctionOrPropertyFromCallActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val diagElement = diagnostic.getPsiElement()
        if (diagElement.getParentByType(javaClass<JetTypeReference>()) != null) return null

        val callExpr = when (diagnostic.getFactory()) {
            in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS, Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND -> {
                val parent = diagElement.getParent()
                if (parent is JetCallExpression && parent.getCalleeExpression() == diagElement) parent else diagElement
            }

            Errors.NO_VALUE_FOR_PARAMETER,
            Errors.TOO_MANY_ARGUMENTS -> diagElement.getParentByType(javaClass<JetCallExpression>())

            else -> throw AssertionError("Unexpected diagnostic: ${diagnostic.getFactory()}")
        } as? JetExpression ?: return null

        val calleeExpr = when (callExpr) {
            is JetCallExpression -> callExpr.getCalleeExpression()
            is JetSimpleNameExpression -> callExpr
            else -> null
        } as? JetSimpleNameExpression ?: return null

        if (calleeExpr.getReferencedNameElementType() != JetTokens.IDENTIFIER) return null

        val callParent = callExpr.getParent()
        val fullCallExpr =
                if (callParent is JetQualifiedExpression && callParent.getSelectorExpression() == callExpr) callParent else callExpr

        val context = AnalyzerFacadeWithCache.getContextForElement(callExpr)
        val receiver = callExpr.getCall(context)?.getExplicitReceiver()

        val receiverType = when (receiver) {
            null, ReceiverValue.NO_RECEIVER -> TypeInfo.Empty
            is Qualifier -> {
                val qualifierType = context[BindingContext.EXPRESSION_TYPE, receiver.expression] ?: return null
                TypeInfo(qualifierType, Variance.IN_VARIANCE)
            }
            else -> TypeInfo(receiver.getType(), Variance.IN_VARIANCE)
        }

        val possibleContainers =
                if (receiverType is TypeInfo.Empty) {
                    val containers = with(fullCallExpr.getExtractionContainers()) {
                        if (callExpr is JetCallExpression) this else filter { it is JetClassBody || it is JetFile }
                    }
                    if (containers.isNotEmpty()) containers else return null
                }
                else Collections.emptyList()

        val callableInfo = when (callExpr) {
            is JetCallExpression -> {
                val anyType = KotlinBuiltIns.getInstance().getNullableAnyType()
                val parameters = callExpr.getValueArguments().map {
                    ParameterInfo(
                            it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                            it.getArgumentName()?.getReferenceExpression()?.getReferencedName()
                    )
                }
                val returnType = TypeInfo(fullCallExpr, Variance.OUT_VARIANCE)
                FunctionInfo(calleeExpr.getReferencedName(), receiverType, returnType, possibleContainers, parameters)
            }

            is JetSimpleNameExpression -> {
                val varExpected = fullCallExpr.getAssignmentByLHS() != null
                val returnType = TypeInfo(
                        fullCallExpr.getExpressionForTypeGuess(),
                        if (varExpected) Variance.INVARIANT else Variance.OUT_VARIANCE
                )
                PropertyInfo(calleeExpr.getReferencedName(), receiverType, returnType, varExpected, possibleContainers)
            }

            else -> return null
        }

        return CreateCallableFromUsageFix(callExpr, callableInfo)
    }
}