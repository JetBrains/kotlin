package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.resolve.calls.callUtil.getCall
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.Qualifier
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.JetExpression
import java.util.Collections
import org.jetbrains.jet.plugin.refactoring.getExtractionContainers
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.kotlin.psi.JetTypeReference
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import org.jetbrains.jet.plugin.caches.resolve.analyze

object CreateFunctionOrPropertyFromCallActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val diagElement = diagnostic.getPsiElement()
        if (PsiTreeUtil.getParentOfType(diagElement, javaClass<JetTypeReference>(), javaClass<JetAnnotationEntry>()) != null) return null

        val callExpr = when (diagnostic.getFactory()) {
                           in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS, Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND -> {
                               val parent = diagElement.getParent()
                               if (parent is JetCallExpression && parent.getCalleeExpression() == diagElement) parent else diagElement
                           }

                           Errors.NO_VALUE_FOR_PARAMETER,
                           Errors.TOO_MANY_ARGUMENTS -> diagElement.getNonStrictParentOfType<JetCallExpression>()

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

        val context = calleeExpr.analyze()
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
                val typeParameters = callExpr.getTypeArguments().map { TypeInfo(it.getTypeReference(), Variance.INVARIANT) }
                val returnType = TypeInfo(fullCallExpr, Variance.OUT_VARIANCE)
                FunctionInfo(calleeExpr.getReferencedName(), receiverType, returnType, possibleContainers, parameters, typeParameters)
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
