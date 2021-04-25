/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.quickfix.AddExclExclCallFix
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.OperatorNameConventions

object AddExclExclCallFixFactories {
    val unsafeCallFactory = diagnosticFixFactory<KtFirDiagnostic.UnsafeCall> { diagnostic ->
        getFixForUnsafeCall(diagnostic.psi)
    }

    val unsafeInfixCallFactory = diagnosticFixFactory<KtFirDiagnostic.UnsafeInfixCall> { diagnostic ->
        getFixForUnsafeCall(diagnostic.psi)
    }

    val unsafeOperatorCallFactory = diagnosticFixFactory<KtFirDiagnostic.UnsafeOperatorCall> { diagnostic ->
        getFixForUnsafeCall(diagnostic.psi)
    }

    private fun KtAnalysisSession.getFixForUnsafeCall(psi: PsiElement): List<IntentionAction> {
        val (target, hasImplicitReceiver) = when (psi) {
            // `foo.bar` -> `foo!!.bar`
            is KtDotQualifiedExpression -> psi.receiverExpression to false

            // `foo[bar]` -> `foo!![bar]`
            is KtArrayAccessExpression -> psi.arrayExpression to false

            is KtCallableReferenceExpression -> psi.lhs.let { lhs ->
                if (lhs != null) {
                    // `foo::bar` -> `foo!!::bar`
                    lhs to false
                } else {
                    // `::bar -> this!!::bar`
                    psi to true
                }
            }

            // `bar` -> `this!!.bar`
            is KtNameReferenceExpression -> psi to true

            // `bar()` -> `this!!.bar()`
            is KtCallExpression -> psi to true

            // `-foo` -> `-foo!!`
            // NOTE: Unsafe unary operator call is reported as UNSAFE_CALL, _not_ UNSAFE_OPERATOR_CALL
            is KtUnaryExpression -> psi.baseExpression to false

            is KtBinaryExpression -> {
                val receiver = if (KtPsiUtil.isInOrNotInOperation(psi)) {
                    // `bar in foo` -> `bar in foo!!`
                    psi.right
                } else {
                    // `foo + bar` -> `foo!! + bar` OR `foo infixFun bar` -> `foo!! infixFun bar`
                    psi.left
                }
                receiver to false
            }

            // UNSAFE_INFIX_CALL/UNSAFE_OPERATOR_CALL on KtBinaryExpression is reported on the child KtOperationReferenceExpression
            is KtOperationReferenceExpression -> return getFixForUnsafeCall(psi.parent)

            else -> return emptyList()
        }

        return listOfNotNull(target.asAddExclExclCallFix(hasImplicitReceiver = hasImplicitReceiver))
    }

    val iteratorOnNullableFactory = diagnosticFixFactory<KtFirDiagnostic.IteratorOnNullable> { diagnostic ->
        val expression = diagnostic.psi as? KtExpression ?: return@diagnosticFixFactory emptyList()
        val type = expression.getKtType()
        if (!type.canBeNull) return@diagnosticFixFactory emptyList()

        val typeScope = type.getTypeScope() ?: return@diagnosticFixFactory emptyList()
        val hasValidIterator = typeScope.getCallableSymbols { it == OperatorNameConventions.ITERATOR }
            .filter { it is KtFunctionSymbol && it.isOperator && it.valueParameters.isEmpty() }.singleOrNull() != null
        if (hasValidIterator) {
            listOfNotNull(expression.asAddExclExclCallFix())
        } else {
            emptyList()
        }
    }
}

internal fun PsiElement?.asAddExclExclCallFix(hasImplicitReceiver: Boolean = false) =
    this?.let { AddExclExclCallFix(it, hasImplicitReceiver) } ?: null
