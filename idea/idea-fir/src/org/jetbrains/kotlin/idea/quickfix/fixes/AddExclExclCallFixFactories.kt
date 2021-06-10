/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.quickfix.AddExclExclCallFix
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object AddExclExclCallFixFactories {
    val unsafeCallFactory = diagnosticFixFactory(KtFirDiagnostic.UnsafeCall::class) { diagnostic ->
        getFixForUnsafeCall(diagnostic.psi)
    }

    val unsafeInfixCallFactory = diagnosticFixFactory(KtFirDiagnostic.UnsafeInfixCall::class) { diagnostic ->
        getFixForUnsafeCall(diagnostic.psi)
    }

    val unsafeOperatorCallFactory = diagnosticFixFactory(KtFirDiagnostic.UnsafeOperatorCall::class) { diagnostic ->
        getFixForUnsafeCall(diagnostic.psi)
    }

    private fun KtAnalysisSession.getFixForUnsafeCall(psi: PsiElement): List<AddExclExclCallFix> {
        val (target, hasImplicitReceiver) = when (val unwrapped = psi.unwrapParenthesesLabelsAndAnnotations()) {
            // `foo.bar` -> `foo!!.bar`
            is KtDotQualifiedExpression -> unwrapped.receiverExpression to false

            // `foo[bar]` -> `foo!![bar]`
            is KtArrayAccessExpression -> unwrapped.arrayExpression to false

            is KtCallableReferenceExpression -> unwrapped.lhs.let { lhs ->
                if (lhs != null) {
                    // `foo::bar` -> `foo!!::bar`
                    lhs to false
                } else {
                    // `::bar -> this!!::bar`
                    unwrapped to true
                }
            }

            // `bar` -> `this!!.bar`
            is KtNameReferenceExpression -> unwrapped to true

            // `bar()` -> `this!!.bar()`
            is KtCallExpression -> unwrapped to true

            // `-foo` -> `-foo!!`
            // NOTE: Unsafe unary operator call is reported as UNSAFE_CALL, _not_ UNSAFE_OPERATOR_CALL
            is KtUnaryExpression -> unwrapped.baseExpression to false

            is KtBinaryExpression -> {
                val receiver = when {
                    KtPsiUtil.isInOrNotInOperation(unwrapped) ->
                        // `bar in foo` -> `bar in foo!!`
                        unwrapped.right
                    KtPsiUtil.isAssignment(unwrapped) ->
                        // UNSAFE_CALL for assignments (e.g., `foo.bar = value`) is reported on the entire statement (KtBinaryExpression).
                        // The unsafe call is on the LHS of the assignment.
                        return getFixForUnsafeCall(unwrapped.left ?: return emptyList())
                    else ->
                        // `foo + bar` -> `foo!! + bar` OR `foo infixFun bar` -> `foo!! infixFun bar`
                        unwrapped.left
                }
                receiver to false
            }

            // UNSAFE_INFIX_CALL/UNSAFE_OPERATOR_CALL on KtBinaryExpression is reported on the child KtOperationReferenceExpression
            is KtOperationReferenceExpression -> return getFixForUnsafeCall(unwrapped.parent)

            else -> return emptyList()
        }

        // We don't want to offer AddExclExclCallFix if we know the expression is definitely null, e.g.:
        //
        //   if (nullableInt == null) {
        //     val x = nullableInt.length  // No AddExclExclCallFix here
        //   }
        if (target?.safeAs<KtExpression>()?.isDefinitelyNull() == true) {
            return emptyList()
        }

        return listOfNotNull(target.asAddExclExclCallFix(hasImplicitReceiver = hasImplicitReceiver))
    }

    val iteratorOnNullableFactory = diagnosticFixFactory(KtFirDiagnostic.IteratorOnNullable::class) { diagnostic ->
        val expression = diagnostic.psi as? KtExpression ?: return@diagnosticFixFactory emptyList()
        val type = expression.getKtType()
        if (!type.canBeNull) return@diagnosticFixFactory emptyList()

        // NOTE: This is different from FE1.0 in that we offer the fix even if the function does NOT have the `operator` modifier.
        // Adding `!!` will then surface the error that `operator` should be added (with corresponding fix).
        val typeScope = type.getTypeScope() ?: return@diagnosticFixFactory emptyList()
        val hasValidIterator = typeScope.getCallableSymbols { it == OperatorNameConventions.ITERATOR }
            .filter { it is KtFunctionSymbol && it.valueParameters.isEmpty() }.singleOrNull() != null
        if (hasValidIterator) {
            listOfNotNull(expression.asAddExclExclCallFix())
        } else {
            emptyList()
        }
    }
}

internal fun PsiElement?.asAddExclExclCallFix(hasImplicitReceiver: Boolean = false) =
    this?.let { AddExclExclCallFix(it, hasImplicitReceiver) }
