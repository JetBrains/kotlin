/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi

sealed class HLDiagnosticFixFactory<DIAGNOSTIC_PSI : PsiElement, in DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>> {
    abstract fun KtAnalysisSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<HLQuickFix<*, *>>
}

private class HLDiagnosticFixFactoryWithFixedApplicator<DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput>(
    private val applicator: HLApplicator<TARGET_PSI, INPUT>,
    private val createTargets: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>
) : HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC>() {
    override fun KtAnalysisSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<HLQuickFix<TARGET_PSI, INPUT>> =
        createTargets.invoke(this, diagnostic).map { (target, input) -> HLQuickFix(target, input, applicator) }
}

private class HLDiagnosticFixFactoryWithVariableApplicator<DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>>(
    private val createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<HLQuickFix<*, *>>
) : HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC>() {
    override fun KtAnalysisSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<HLQuickFix<*, *>> =
        createQuickFixes.invoke(this, diagnostic)
}

internal fun <DIAGNOSTIC : KtDiagnosticWithPsi<PsiElement>> KtAnalysisSession.createPlatformQuickFixes(
    diagnostic: DIAGNOSTIC,
    factory: HLDiagnosticFixFactory<PsiElement, DIAGNOSTIC>
): List<IntentionAction> = with(factory) { createQuickFixes(diagnostic) }

/**
 * Returns a [HLDiagnosticFixFactory] that creates targets and inputs ([HLApplicatorTargetWithInput]) from a diagnostic.
 * The targets and inputs are consumed by the given applicator to apply fixes.
 */
fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput> diagnosticFixFactory(
    applicator: HLApplicator<TARGET_PSI, INPUT>,
    createTargets: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>
): HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC> =
    HLDiagnosticFixFactoryWithFixedApplicator(applicator, createTargets)

/**
 * Returns a [HLDiagnosticFixFactory] that creates [HLQuickFix]es from a diagnostic.
 */
fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>> diagnosticFixFactory(
    createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<HLQuickFix<*, *>>
): HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC> =
    HLDiagnosticFixFactoryWithVariableApplicator(createQuickFixes)
