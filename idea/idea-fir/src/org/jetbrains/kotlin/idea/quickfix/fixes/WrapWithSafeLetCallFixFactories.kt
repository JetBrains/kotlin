/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.api.applicator.applicator
import org.jetbrains.kotlin.idea.core.FirKotlinNameSuggester
import org.jetbrains.kotlin.idea.fir.api.fixes.HLQuickFix
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.calls.KtCall
import org.jetbrains.kotlin.idea.frontend.api.calls.KtVariableWithInvokeFunctionCall
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.types.expressions.OperatorConventions

object WrapWithSafeLetCallFixFactories {
    class Input(
        val nullableExpressionPointer: SmartPsiElementPointer<KtExpression>,
        val suggestedVariableName: String,
        val isImplicitInvokeCallToMemberProperty: Boolean,
    ) : HLApplicatorInput

    private val LOG = Logger.getInstance(this::class.java)

    /**
     *  Applicator that wraps a given target expression inside a `let` call on the input `nullableExpression`.
     *
     *  Consider the following code snippet:
     *
     *  ```
     *  fun test(s: String?) {
     *    println(s.length)
     *  }
     *  ```
     *
     *  In this case, one use the applicator with the following arguments
     *    - target expression: `s.length`
     *    - nullable expression: `s`
     *    - suggestedVariableName: `myName`
     *    - isImplicitInvokeCallToMemberProperty: false
     *
     *  Then the applicator changes the code to
     *
     *  ```
     *  fun test(s: String?) {
     *    println(s?.let { myName -> myName.length })
     *  }
     *  ```
     *  `isImplicitInvokeCallToMemberProperty` controls the behavior when hoisting up the nullable expression. It should be set to true
     *  if the call is to a invocable member property.
     */
    val applicator: HLApplicator<KtExpression, Input> = applicator {
        familyAndActionName(KotlinBundle.lazyMessage("wrap.with.let.call"))
        applyTo { targetExpression, input ->
            val nullableExpression = input.nullableExpressionPointer.element ?: return@applyTo
            if (!nullableExpression.parents.contains(targetExpression)) {
                LOG.warn(
                    "Unexpected input for WrapWithSafeLetCall. Nullable expression '${nullableExpression.text}' should be a descendant" +
                            " of '${targetExpression.text}'."
                )
                return@applyTo
            }
            val suggestedVariableName = input.suggestedVariableName
            val factory = KtPsiFactory(targetExpression)

            fun getNewExpression(nullableExpressionText: String, expressionUnderLetText: String): KtExpression {
                return when (suggestedVariableName) {
                    "it" -> factory.createExpressionByPattern("$0?.let { $1 }", nullableExpressionText, expressionUnderLetText)
                    else -> factory.createExpressionByPattern(
                        "$0?.let { $1 -> $2 }",
                        nullableExpressionText,
                        suggestedVariableName,
                        expressionUnderLetText
                    )
                }
            }

            val callExpression = nullableExpression.parentOfType<KtCallExpression>(withSelf = true)
            val qualifiedExpression = callExpression?.getQualifiedExpressionForSelector()
            val receiverExpression = qualifiedExpression?.receiverExpression
            if (receiverExpression != null && input.isImplicitInvokeCallToMemberProperty) {
                // In this case, the nullable expression is an invocable member. For example consider the following
                //
                // interface Foo {
                //   val bar: (() -> Unit)?
                // }
                // fun test(foo: Foo) {
                //   foo.bar()
                // }
                //
                // In this case, `foo.bar` is nullable and this fix should change the code to `foo.bar?.let { it() }`. But note that
                // the PSI structure of the above code is
                //
                // - qualifiedExpression: foo.bar()
                //   - receiver: foo
                //   - operationTokenNode: .
                //   - selectorExpression: bar()
                //     - calleeExpression: bar
                //     - valueArgumentList: ()
                //
                // So we need to explicitly construct the nullable expression text `foo.bar`.
                val nullableExpressionText =
                    "${receiverExpression.text}${qualifiedExpression.operationSign.value}${nullableExpression.text}"
                val newInvokeCallText =
                    "${suggestedVariableName}${callExpression.valueArgumentList?.text ?: ""}${
                        callExpression.lambdaArguments.joinToString(
                            " ",
                            prefix = " "
                        ) { it.text }
                    }"
                if (qualifiedExpression == targetExpression) {
                    targetExpression.replace(getNewExpression(nullableExpressionText, newInvokeCallText))
                } else {
                    qualifiedExpression.replace(factory.createExpression(newInvokeCallText))
                    targetExpression.replace(getNewExpression(nullableExpressionText, targetExpression.text))
                }

            } else {
                val nullableExpressionText = when (nullableExpression) {
                    is KtBinaryExpression, is KtBinaryExpressionWithTypeRHS -> "(${nullableExpression.text})"
                    else -> nullableExpression.text
                }
                nullableExpression.replace(factory.createExpression(suggestedVariableName))
                targetExpression.replace(getNewExpression(nullableExpressionText, targetExpression.text))
            }
        }
    }

    val forUnsafeCall = diagnosticFixFactory<KtFirDiagnostic.UnsafeCall> { diagnostic ->
        val nullableExpression = diagnostic.receiverExpression
        createWrapWithSafeLetCallInputForNullableExpressionIfMoreThanImmediateParentIsWrapped(nullableExpression)
    }

    val forUnsafeImplicitInvokeCall = diagnosticFixFactory<KtFirDiagnostic.UnsafeImplicitInvokeCall> { diagnostic ->
        val callExpression = diagnostic.psi.parentOfType<KtCallExpression>(withSelf = true) ?: return@diagnosticFixFactory emptyList()
        val callingFunctionalVariableInLocalScope =
            isCallingFunctionalTypeVariableInLocalScope(callExpression) ?: return@diagnosticFixFactory emptyList()
        createWrapWithSafeLetCallInputForNullableExpression(
            callExpression.calleeExpression,
            isImplicitInvokeCallToMemberProperty = !callingFunctionalVariableInLocalScope
        )
    }

    private fun KtAnalysisSession.isCallingFunctionalTypeVariableInLocalScope(callExpression: KtCallExpression): Boolean? {
        val calleeName = callExpression.calleeExpression?.text ?: return null
        val callSite = callExpression.parent as? KtQualifiedExpression ?: callExpression
        val functionalVariableSymbol = (callExpression.resolveCall() as? KtVariableWithInvokeFunctionCall)?.target ?: return false
        val localScope = callExpression.containingKtFile.getScopeContextForPosition(callSite)
        // If no symbol in the local scope contains the called symbol, then the symbol must be a member symbol.
        return localScope.scopes.getCallableSymbols { it.identifierOrNullIfSpecial == calleeName }.any { it == functionalVariableSymbol }
    }

    val forUnsafeInfixCall = diagnosticFixFactory<KtFirDiagnostic.UnsafeInfixCall> { diagnostic ->
        createWrapWithSafeLetCallInputForNullableExpressionIfMoreThanImmediateParentIsWrapped(diagnostic.receiverExpression)
    }

    val forUnsafeOperatorCall = diagnosticFixFactory<KtFirDiagnostic.UnsafeOperatorCall> { diagnostic ->
        createWrapWithSafeLetCallInputForNullableExpressionIfMoreThanImmediateParentIsWrapped(diagnostic.receiverExpression)
    }

    val forArgumentTypeMismatch = diagnosticFixFactory<KtFirDiagnostic.ArgumentTypeMismatch> { diagnostic ->
        if (diagnostic.isMismatchDueToNullability) createWrapWithSafeLetCallInputForNullableExpression(diagnostic.psi.wrappingExpressionOrSelf)
        else emptyList()
    }

    private fun KtAnalysisSession.createWrapWithSafeLetCallInputForNullableExpressionIfMoreThanImmediateParentIsWrapped(
        nullableExpression: KtExpression?,
        isImplicitInvokeCallToMemberProperty: Boolean = false,
    ): List<IntentionAction> {
        val surroundingExpression = nullableExpression?.surroundingExpression
        if (
            surroundingExpression == null ||
            // If the surrounding expression is at a place that accepts null value, then we don't provide wrap with let call because the
            // plain safe call operator (?.) is a better fix.
            isExpressionAtNullablePosition(surroundingExpression)
        ) {
            return emptyList()
        }
        // In addition, if there is no parent that is at a nullable position, then we don't offer wrapping with let either because
        // it still doesn't fix the code. Hence, the plain safe call operator is a better fix.
        val surroundingNullableExpression = findParentExpressionAtNullablePosition(nullableExpression) ?: return emptyList()
        return createWrapWithSafeLetCallInputForNullableExpression(
            nullableExpression,
            isImplicitInvokeCallToMemberProperty,
            surroundingNullableExpression
        )
    }

    private fun KtAnalysisSession.createWrapWithSafeLetCallInputForNullableExpression(
        nullableExpression: KtExpression?,
        isImplicitInvokeCallToMemberProperty: Boolean = false,
        surroundingExpression: KtExpression? = findParentExpressionAtNullablePosition(nullableExpression)
            ?: nullableExpression?.surroundingExpression
    ): List<IntentionAction> {
        if (nullableExpression == null || surroundingExpression == null) return emptyList()
        val existingNames =
            nullableExpression.containingKtFile.getScopeContextForPosition(nullableExpression).scopes.getPossibleCallableNames()
                .mapNotNull { it.identifierOrNullIfSpecial }
        // Note, the order of the candidate matters. We would prefer the default `it` so the generated code won't need to declare the
        // variable explicitly.
        val candidateNames = listOfNotNull("it", getDeclaredParameterNameForArgument(nullableExpression))
        val suggestedName = FirKotlinNameSuggester.suggestNameByMultipleNames(candidateNames) { it !in existingNames }
        return listOf(
            HLQuickFix(
                surroundingExpression,
                Input(nullableExpression.createSmartPointer(), suggestedName, isImplicitInvokeCallToMemberProperty),
                applicator
            )
        )
    }

    private fun KtAnalysisSession.getDeclaredParameterNameForArgument(argumentExpression: KtExpression): String? {
        val valueArgument = argumentExpression.parent as? KtValueArgument ?: return null
        val successCallTarget =
            (argumentExpression.parentOfType<KtCallExpression>()?.resolveCall()?.targetFunction?.candidates?.singleOrNull()) ?: return null
        return successCallTarget.valueParameters.getOrNull(valueArgument.argumentIndex)?.name?.identifierOrNullIfSpecial
    }

    private fun KtAnalysisSession.findParentExpressionAtNullablePosition(expression: KtExpression?): KtExpression? {
        if (expression == null) return null
        var current = expression.surroundingExpression
        while (current != null && !isExpressionAtNullablePosition(current)) {
            current = current.surroundingExpression
        }
        return current
    }

    private fun KtAnalysisSession.isExpressionAtNullablePosition(expression: KtExpression): Boolean {
        val parent = expression.parent
        return when {
            parent is KtProperty && expression == parent.initializer -> {
                if (parent.typeReference == null) return true
                val symbol = parent.getSymbol()
                (symbol as? KtCallableSymbol)?.annotatedType?.type?.isMarkedNullable ?: true
            }
            parent is KtValueArgument && expression == parent.getArgumentExpression() -> {
                // In the following logic, if call is missing, unresolved, or contains error, we just stop here so the wrapped call would be
                // inserted here.
                val functionCall = parent.getParentOfType<KtCallExpression>(strict = true) ?: return true
                val resolvedCall = functionCall.resolveCall() ?: return true
                return doesFunctionAcceptNull(resolvedCall, parent.argumentIndex) ?: true
            }
            parent is KtBinaryExpression -> {
                if (parent.operationToken in KtTokens.ALL_ASSIGNMENTS && parent.left == expression) {
                    // If current expression is an l-value in an assignment, just keep going up because one cannot assign to a let call.
                    return false
                }
                val resolvedCall = parent.resolveCall()
                when {
                    resolvedCall != null -> {
                        // The binary expression is a call to some function
                        val isInExpression = parent.operationToken in OperatorConventions.IN_OPERATIONS
                        val expressionIsArg = when {
                            parent.left == expression -> isInExpression
                            parent.right == expression -> !isInExpression
                            else -> return true
                        }
                        doesFunctionAcceptNull(resolvedCall, if (expressionIsArg) 0 else -1) ?: true
                    }
                    parent.operationToken == KtTokens.EQ -> {
                        // The binary expression is a variable assignment
                        parent.left?.getKtType()?.isMarkedNullable ?: true
                    }
                    // The binary expression is some unrecognized constructs so we stop here.
                    else -> true
                }
            }
            // Qualified expression can always just be updated with a safe call operator to to make it accept nullable receiver. Hence we
            // don't want to offer the wrap with let call quickfix.
            parent is KtQualifiedExpression && parent.receiverExpression == expression -> true
            // Ideally we should do more analysis on the control structure to determine if the type can actually allow null here. But that
            // may be too fancy and can be counter-intuitive to user.
            parent is KtContainerNodeForControlStructureBody -> true
            // Again, for simplicity's sake, we treat block as a place that can accept expression of any type. This is not strictly true
            // for lambda expressions, but it results in a more deterministic behavior.
            parent is KtBlockExpression -> true
            else -> false
        }
    }

    /**
     * Checks if the called function can accept null for the argument at the given index. If the index is -1, then we check the receiver
     * type. The function returns null if any necessary assumptions are not met. For example, if the call is not resolved to a unique
     * function or the function doesn't have a parameter at the given index. Then caller can do whatever needed to cover such cases.
     */
    private fun KtAnalysisSession.doesFunctionAcceptNull(call: KtCall, index: Int): Boolean? {
        val symbol = call.targetFunction.candidates.singleOrNull() ?: return null
        if (index == -1) {
            // Null extension receiver means the function does not accept extension receiver and hence cannot be invoked on a nullable
            // value.
            return (symbol as? KtCallableSymbol)?.receiverType?.type?.isMarkedNullable == true
        }
        return symbol.valueParameters.getOrNull(index)?.annotatedType?.type?.isMarkedNullable
    }

    private val KtExpression.surroundingExpression: KtExpression?
        get() {
            var current: PsiElement? = parent
            while (true) {
                // Never go above declarations or control structure so that the wrap-with-let quickfix only applies to a "small" scope
                // around the nullable expression.
                if (current == null ||
                    current is KtContainerNodeForControlStructureBody ||
                    current is KtWhenEntry ||
                    current is KtParameter ||
                    current is KtProperty ||
                    current is KtReturnExpression ||
                    current is KtDeclaration ||
                    current is KtBlockExpression
                ) {
                    return null
                }
                val parent = current.parent
                if (current is KtExpression &&
                    // We skip parenthesized expression and labeled expressions.
                    current !is KtParenthesizedExpression && current !is KtLabeledExpression &&
                    // We skip KtCallExpression if it's the `selectorExpression` of a qualified expression because the selector expression is
                    // not an actual expression that can be swapped for any arbitrary expressions.
                    (parent !is KtQualifiedExpression || parent.selectorExpression != current)
                ) {
                    return current
                }
                current = parent
            }
        }

    private val PsiElement.wrappingExpressionOrSelf: KtExpression? get() = parentOfType(withSelf = true)
}