/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.fixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.quickfix.QuickFixActionBase
import kotlin.reflect.KClass

sealed class HLDiagnosticFixFactory<DIAGNOSTIC : KtDiagnosticWithPsi<*>> {
    abstract fun KtAnalysisSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<QuickFixActionBase<*>>
    abstract val diagnosticClass: KClass<DIAGNOSTIC>
}

private class HLDiagnosticFixFactoryWithFixedApplicator<DIAGNOSTIC : KtDiagnosticWithPsi<*>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput>(
    override val diagnosticClass: KClass<DIAGNOSTIC>,
    private val applicator: HLApplicator<TARGET_PSI, INPUT>,
    private val createTargets: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>,
) : HLDiagnosticFixFactory<DIAGNOSTIC>() {
    override fun KtAnalysisSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<HLQuickFix<TARGET_PSI, INPUT>> =
        createTargets.invoke(this, diagnostic).map { (target, input) -> HLQuickFix(target, input, applicator) }
}

private class HLDiagnosticFixFactoryUsingQuickFixActionBase<DIAGNOSTIC : KtDiagnosticWithPsi<*>>(
    override val diagnosticClass: KClass<DIAGNOSTIC>,
    private val createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<QuickFixActionBase<*>>
) : HLDiagnosticFixFactory<DIAGNOSTIC>() {
    override fun KtAnalysisSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<QuickFixActionBase<*>> =
        createQuickFixes.invoke(this, diagnostic)
}

internal fun <DIAGNOSTIC : KtDiagnosticWithPsi<PsiElement>> KtAnalysisSession.createPlatformQuickFixes(
    diagnostic: DIAGNOSTIC,
    factory: HLDiagnosticFixFactory<DIAGNOSTIC>
): List<IntentionAction> = with(factory) { createQuickFixes(diagnostic) }

/**
 * Returns a [HLDiagnosticFixFactory] that creates targets and inputs ([HLApplicatorTargetWithInput]) from a diagnostic.
 * The targets and inputs are consumed by the given applicator to apply fixes.
 */
fun <DIAGNOSTIC : KtDiagnosticWithPsi<*>, TARGET_PSI : PsiElement, INPUT : HLApplicatorInput> diagnosticFixFactory(
    diagnosticClass: KClass<DIAGNOSTIC>,
    applicator: HLApplicator<TARGET_PSI, INPUT>,
    createTargets: KtAnalysisSession.(DIAGNOSTIC) -> List<HLApplicatorTargetWithInput<TARGET_PSI, INPUT>>
): HLDiagnosticFixFactory<DIAGNOSTIC> =
    HLDiagnosticFixFactoryWithFixedApplicator(diagnosticClass, applicator, createTargets)

/**
 * Returns a [HLDiagnosticFixFactory] that creates [QuickFixActionBase]s from a diagnostic.
 */
fun <DIAGNOSTIC : KtDiagnosticWithPsi<*>> diagnosticFixFactory(
    diagnosticClass: KClass<DIAGNOSTIC>,
    createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<QuickFixActionBase<*>>
): HLDiagnosticFixFactory<DIAGNOSTIC> =
    HLDiagnosticFixFactoryUsingQuickFixActionBase(diagnosticClass, createQuickFixes)

/**
 * Returns a [Collection] of [HLDiagnosticFixFactory] that creates [QuickFixActionBase]s from diagnostics that have the same type of
 * [PsiElement].
 */
fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>> diagnosticFixFactories(
    vararg diagnosticClasses: KClass<out DIAGNOSTIC>,
    createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<QuickFixActionBase<*>>
): Collection<HLDiagnosticFixFactory<out DIAGNOSTIC>> =
    diagnosticClasses.map { HLDiagnosticFixFactoryUsingQuickFixActionBase(it, createQuickFixes) }

/**
 * Returns a [HLDiagnosticFixFactory] that creates [IntentionAction]s from a diagnostic.
 */
fun <DIAGNOSTIC : KtDiagnosticWithPsi<*>> diagnosticFixFactoryFromIntentionActions(
    diagnosticClass: KClass<DIAGNOSTIC>,
    createIntentionActions: KtAnalysisSession.(DIAGNOSTIC) -> List<IntentionAction>
): HLDiagnosticFixFactory<DIAGNOSTIC> {
    // Wrap the IntentionActions as QuickFixActionBase. This ensures all fixes are of type QuickFixActionBase.
    val createQuickFixes: KtAnalysisSession.(DIAGNOSTIC) -> List<QuickFixActionBase<*>> = { diagnostic ->
        val intentionActions = createIntentionActions.invoke(this, diagnostic)
        intentionActions.map { IntentionActionAsQuickFixWrapper(it, diagnostic.psi) }
    }
    return HLDiagnosticFixFactoryUsingQuickFixActionBase(diagnosticClass, createQuickFixes)
}

/**
 * Returns a [Collection] of [HLDiagnosticFixFactory] that creates [IntentionAction]s from diagnostics that have the same type of
 * [PsiElement].
 */
fun <DIAGNOSTIC_PSI : PsiElement, DIAGNOSTIC : KtDiagnosticWithPsi<DIAGNOSTIC_PSI>> diagnosticFixFactoriesFromIntentionActions(
    vararg diagnosticClasses: KClass<out DIAGNOSTIC>,
    createIntentionActions: KtAnalysisSession.(DIAGNOSTIC) -> List<IntentionAction>
): Collection<HLDiagnosticFixFactory<out DIAGNOSTIC>> =
    diagnosticClasses.map { diagnosticFixFactoryFromIntentionActions(it, createIntentionActions) }

private class IntentionActionAsQuickFixWrapper<T : PsiElement>(val intentionAction: IntentionAction, element: T) :
    QuickFixActionBase<T>(element) {
    override fun getText(): String = intentionAction.text
    override fun getFamilyName(): String = intentionAction.familyName
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = intentionAction.invoke(project, editor, file)
}
