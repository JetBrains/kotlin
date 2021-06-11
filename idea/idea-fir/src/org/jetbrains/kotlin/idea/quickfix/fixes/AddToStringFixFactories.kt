/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.quickfix.AddToStringFix
import org.jetbrains.kotlin.psi.KtExpression

object AddToStringFixFactories {
    @OptIn(ExperimentalStdlibApi::class)
    private fun KtAnalysisSession.getFixes(element: PsiElement?, expectedType: KtType, actualType: KtType): List<AddToStringFix> {
        if (element !is KtExpression) return emptyList()
        return buildList {
            if (expectedType.isString || expectedType.isCharSequence) {
                add(AddToStringFix(element, false))
                if (expectedType.isMarkedNullable && actualType.isMarkedNullable) {
                    add(AddToStringFix(element, true))
                }
            }
        }
    }

    val typeMismatch = diagnosticFixFactory(KtFirDiagnostic.TypeMismatch::class) { diagnostic ->
        getFixes(diagnostic.psi, diagnostic.expectedType, diagnostic.actualType)
    }

    val argumentTypeMismatch = diagnosticFixFactory(KtFirDiagnostic.ArgumentTypeMismatch::class) { diagnostic ->
        getFixes(diagnostic.psi, diagnostic.expectedType, diagnostic.actualType)
    }

    val assignmentTypeMismatch = diagnosticFixFactory(KtFirDiagnostic.AssignmentTypeMismatch::class) { diagnostic ->
        getFixes(diagnostic.psi, diagnostic.expectedType, diagnostic.actualType)
    }

    val returnTypeMismatch = diagnosticFixFactory(KtFirDiagnostic.ReturnTypeMismatch::class) { diagnostic ->
        getFixes(diagnostic.psi, diagnostic.expectedType, diagnostic.actualType)
    }

    val initializerTypeMismatch = diagnosticFixFactory(KtFirDiagnostic.InitializerTypeMismatch::class) { diagnostic ->
        getFixes(diagnostic.psi.initializer, diagnostic.expectedType, diagnostic.actualType)
    }
}