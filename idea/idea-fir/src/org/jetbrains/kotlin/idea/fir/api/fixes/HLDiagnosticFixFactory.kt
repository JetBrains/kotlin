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

sealed class HLDiagnosticFixFactory<DIAGNOSTIC_PSI : PsiElement, in DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput> {
    abstract fun KtAnalysisSession.createTargets(diagnostic: DIAGNOSTIC): List<HLApplicatorWithTargetAndInput<TARGET_PSI, INPUT>>
}

private class HLDiagnosticFixFactoryWithFixedApplicator<DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput>(
    private val applicator: HLApplicator<TARGET_PSI, INPUT>,
    private val createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>
) : HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC, TARGET_PSI, INPUT>() {
    override fun KtAnalysisSession.createTargets(diagnostic: DIAGNOSTIC): List<HLApplicatorWithTargetAndInput<TARGET_PSI, INPUT>> =
        createQuickFixes.invoke(this, diagnostic).map { targetAndInput -> HLApplicatorWithTargetAndInput(applicator, targetAndInput) }
}

private class HLDiagnosticFixFactoryWithVariableApplicator<DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput>(
    private val createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorWithTargetAndInput<TARGET_PSI, INPUT>>
) : HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC, TARGET_PSI, INPUT>() {
    override fun KtAnalysisSession.createTargets(diagnostic: DIAGNOSTIC): List<HLApplicatorWithTargetAndInput<TARGET_PSI, INPUT>> =
        createQuickFixes.invoke(this, diagnostic)
}

internal fun <DIAGNOSTIC : KtDiagnosticWithPsi<PsiElement>> KtAnalysisSession.createPlatformQuickFixes(
    diagnostic: DIAGNOSTIC,
    factory: HLDiagnosticFixFactory<PsiElement, DIAGNOSTIC, PsiElement, HLApplicatorInput>
): List<IntentionAction> = with(factory) {
    createTargets(diagnostic).map { (applicator, target, input) -> HLQuickFix(target, input, applicator) }
}

/**
 * Returns a [HLDiagnosticFixFactory] that creates targets and inputs ([HLApplicatorTargetWithInput]) from a diagnostic.
 * The targets and inputs are consumed by the given applicator to apply fixes.
 */
fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput> diagnosticFixFactory(
    applicator: HLApplicator<TARGET_PSI, INPUT>,
    createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>
): HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC, TARGET_PSI, INPUT> =
    HLDiagnosticFixFactoryWithFixedApplicator(applicator, createQuickFixes)

/**
 * Returns a [HLDiagnosticFixFactory] that creates applicators, targets, and inputs ([HLApplicatorWithTargetAndInput]) from a diagnostic.
 * The targets and inputs are consumed by the corresponding applicator to apply fixes.
 */
fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput> diagnosticFixFactory(
    createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorWithTargetAndInput<TARGET_PSI, INPUT>>
): HLDiagnosticFixFactory<DIAGNOSTIC_PSI, DIAGNOSTIC, TARGET_PSI, INPUT> =
    HLDiagnosticFixFactoryWithVariableApplicator(createQuickFixes)
