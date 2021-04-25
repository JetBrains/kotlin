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
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeNullability

object TypeMismatchFactories {
    val argumentTypeMismatchFactory = diagnosticFixFactory<KtFirDiagnostic.ArgumentTypeMismatch> { diagnostic ->
        getFixesForTypeMismatch(diagnostic.psi, diagnostic.expectedType, diagnostic.actualType)
    }

    val returnTypeMismatchFactory = diagnosticFixFactory<KtFirDiagnostic.ReturnTypeMismatch> { diagnostic ->
        getFixesForTypeMismatch(diagnostic.psi, diagnostic.expectedType, diagnostic.actualType)
    }

    private fun KtAnalysisSession.getFixesForTypeMismatch(
        psi: PsiElement,
        expectedType: KtType,
        actualType: KtType
    ): List<IntentionAction> {
        // TODO: Add more fixes than just AddExclExclCallFix when available.
        if (!expectedType.canBeNull && actualType.canBeNull) {
            val nullableExpectedType = expectedType.withNullability(KtTypeNullability.NULLABLE)
            if (actualType isSubTypeOf nullableExpectedType) {
                return listOfNotNull(psi.asAddExclExclCallFix())
            }
        }
        return emptyList()
    }
}
