/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.fir.api.applicator.applicatorByQuickFix
import org.jetbrains.kotlin.idea.fir.api.fixes.HLApplicatorWithTargetAndInput
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.fir.api.fixes.withInput
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeNullability
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeWithNullability
import org.jetbrains.kotlin.idea.quickfix.ReplaceCallFix
import org.jetbrains.kotlin.idea.quickfix.ReplaceImplicitReceiverCallFix
import org.jetbrains.kotlin.idea.quickfix.ReplaceWithSafeCallFix
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

object ReplaceCallFixFactories {
    private val replaceWithSafeCallFixApplicator =
        applicatorByQuickFix<KtExpression, Input, ReplaceCallFix>(
            getFamilyName = KotlinBundle.lazyMessage("replace.with.safe.call"),
            isApplicableByPsi = { psi -> psi is KtDotQualifiedExpression }
        ) { psi, input ->
            ReplaceWithSafeCallFix(psi as KtDotQualifiedExpression, input.notNullNeeded)
        }

    private val replaceImplicitReceiverCallFixApplicator =
        applicatorByQuickFix<KtExpression, Input, ReplaceImplicitReceiverCallFix>(
            getFamilyName = KotlinBundle.lazyMessage("replace.with.safe.this.call")
        ) { psi, input ->
            ReplaceImplicitReceiverCallFix(psi, input.notNullNeeded)
        }

    class Input(val notNullNeeded: Boolean) : HLApplicatorInput

    val unsafeCallFactory =
        diagnosticFixFactory<PsiElement, KtFirDiagnostic.UnsafeCall, KtExpression, Input> { diagnostic ->
            fun KtExpression.shouldHaveNotNullType(): Boolean {
                // This function is used to determine if we may need to add an elvis operator after the safe call. For example, to replace
                // `s.length` in `val x: Int = s.length` with a safe call, it should be replaced with `s.length ?: <caret>`.
                val expectedType = getExpectedType() as? KtTypeWithNullability
                return expectedType?.nullability == KtTypeNullability.NON_NULLABLE
            }

            when (val psi = diagnostic.psi) {
                is KtDotQualifiedExpression -> listOf(
                    HLApplicatorWithTargetAndInput(replaceWithSafeCallFixApplicator, psi withInput Input(psi.shouldHaveNotNullType()))
                )
                is KtNameReferenceExpression -> {
                    // TODO: As a safety precaution, resolve the expression to determine if it is a call with an implicit receiver.
                    // This is a defensive check to ensure that the diagnostic was reported on such a call and not some other name reference.
                    // This isn't strictly needed because FIR checkers aren't reporting on wrong elements, but ReplaceWithSafeCallFixFactory
                    // in FE1.0 does so.
                    listOf(
                        HLApplicatorWithTargetAndInput(
                            replaceImplicitReceiverCallFixApplicator,
                            psi withInput Input(psi.shouldHaveNotNullType())
                        )
                    )
                }
                else -> emptyList()
            }
        }
}
