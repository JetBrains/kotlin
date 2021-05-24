/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeNullability
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeWithNullability
import org.jetbrains.kotlin.idea.quickfix.ReplaceImplicitReceiverCallFix
import org.jetbrains.kotlin.idea.quickfix.ReplaceInfixOrOperatorCallFix
import org.jetbrains.kotlin.idea.quickfix.ReplaceWithSafeCallFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object ReplaceCallFixFactories {
    val unsafeCallFactory =
        diagnosticFixFactory<KtFirDiagnostic.UnsafeCall> { diagnostic ->
            val psi = diagnostic.psi
            val target = if (psi is KtBinaryExpression && psi.operationToken in KtTokens.ALL_ASSIGNMENTS) {
                // UNSAFE_CALL for assignments (e.g., `foo.bar = value`) is reported on the entire statement (KtBinaryExpression).
                // The unsafe call is on the LHS of the assignment.
                psi.left
            } else {
                psi
            }.unwrapParenthesesLabelsAndAnnotations()

            val shouldHaveNotNullType = target.safeAs<KtExpression>()?.let { shouldHaveNotNullType(it) } ?: false
            when (target) {
                is KtDotQualifiedExpression -> listOf(ReplaceWithSafeCallFix(target, shouldHaveNotNullType))
                is KtNameReferenceExpression -> {
                    // TODO: As a safety precaution, resolve the expression to determine if it is a call with an implicit receiver.
                    // This is a defensive check to ensure that the diagnostic was reported on such a call and not some other name reference.
                    // This isn't strictly needed because FIR checkers aren't reporting on wrong elements, but ReplaceWithSafeCallFixFactory
                    // in FE1.0 does so.
                    listOf(ReplaceImplicitReceiverCallFix(target, shouldHaveNotNullType))
                }
                is KtArrayAccessExpression -> listOf(ReplaceInfixOrOperatorCallFix(target, shouldHaveNotNullType))
                else -> emptyList()
            }
        }

    val unsafeInfixCallFactory =
        diagnosticFixFactory<KtFirDiagnostic.UnsafeInfixCall> { diagnostic ->
            val psi = diagnostic.psi
            val target = psi.parent as? KtBinaryExpression ?: return@diagnosticFixFactory emptyList()
            listOf(ReplaceInfixOrOperatorCallFix(target, shouldHaveNotNullType(target), diagnostic.operator))
        }

    val unsafeOperatorCallFactory =
        diagnosticFixFactory<KtFirDiagnostic.UnsafeOperatorCall> { diagnostic ->
            val psi = diagnostic.psi
            val operationToken = psi.safeAs<KtOperationReferenceExpression>()?.getReferencedNameElementType()
            if (operationToken == KtTokens.EQ || operationToken in OperatorConventions.COMPARISON_OPERATIONS) {
                // This matches FE1.0 behavior; see ReplaceInfixOrOperatorCallFixFactory.kt
                return@diagnosticFixFactory emptyList()
            }

            val target = psi.parent as? KtBinaryExpression ?: return@diagnosticFixFactory emptyList()
            listOf(ReplaceInfixOrOperatorCallFix(target, shouldHaveNotNullType(target), diagnostic.operator))
        }

    private fun KtAnalysisSession.shouldHaveNotNullType(expression: KtExpression): Boolean {
        // This function is used to determine if we may need to add an elvis operator after the safe call. For example, to replace
        // `s.length` in `val x: Int = s.length` with a safe call, it should be replaced with `s.length ?: <caret>`.
        val expectedType = expression.getExpectedType() as? KtTypeWithNullability
        return expectedType?.nullability == KtTypeNullability.NON_NULLABLE
    }
}
