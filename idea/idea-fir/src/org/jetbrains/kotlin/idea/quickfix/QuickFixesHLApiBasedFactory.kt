/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi

abstract class QuickFixesHLApiBasedFactory<PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<PSI>> : QuickFixFactory {
    final override fun asKotlinIntentionActionsFactory(): KotlinIntentionActionsFactory {
        error("Should not be called. This function is not considered to bue used in FE10 plugin, from FIR plugin consider using createQuickFix")
    }

    abstract fun KtAnalysisSession.createQuickFix(diagnostic: DIAGNOSTIC): List<IntentionAction>
}


inline fun <PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<PSI>> quickFixesHLApiBasedFactory(
    crossinline createQuickFix: KtAnalysisSession.(DIAGNOSTIC) -> List<IntentionAction>
) = object : QuickFixesHLApiBasedFactory<PSI, DIAGNOSTIC>() {
    override fun KtAnalysisSession.createQuickFix(diagnostic: DIAGNOSTIC): List<IntentionAction> =
        createQuickFix(diagnostic)
}