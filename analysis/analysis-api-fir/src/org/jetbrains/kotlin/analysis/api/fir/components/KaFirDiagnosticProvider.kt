/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.elementCanBeLazilyResolved
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*

internal class KaFirDiagnosticProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaDiagnosticProvider, KaFirSessionComponent {
    override fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> = withPsiValidityAssertion {
        getDiagnostics(resolutionFacade, filter.asLLFilter()).map { it.asKaDiagnostic() }
    }

    override fun KtFile.collectDiagnostics(
        filter: KaDiagnosticCheckerFilter,
    ): Collection<KaDiagnosticWithPsi<*>> = withPsiValidityAssertion {
        collectDiagnosticsForFile(resolutionFacade, filter.asLLFilter()).map { it.asKaDiagnostic() }
    }

    private fun KaDiagnosticCheckerFilter.asLLFilter() = when (this) {
        KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS
        KaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
        KaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS
        KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
    }

    override fun KtFile.analyzeEntirely(pool: (Runnable) -> Unit): Unit = withPsiValidityAssertion {
        val declarations = mutableListOf<KtDeclaration>()

        val visitor = object : KtTreeVisitorVoid() {
            override fun visitDeclaration(declaration: KtDeclaration) {
                if (declaration !is KtCallableDeclaration) {
                    super.visitDeclaration(declaration)
                }

                if (elementCanBeLazilyResolved(declaration)) {
                    declarations.add(declaration)
                }
            }
        }

        accept(visitor)

        if (declarations.isEmpty()) {
            return
        }

        ProgressManager.checkCanceled()

        val phases = FirResolvePhase.entries.filter { !it.noProcessor && it != FirResolvePhase.IMPORTS }

        for (phase in phases) {
            for (declaration in declarations) {
                pool { performAnalysis(declaration, phase) }
            }
        }
    }

    private fun performAnalysis(declaration: KtDeclaration, phase: FirResolvePhase) {
        runReadAction {
            if (declaration.isValid) {
                val firDeclaration = declaration.getOrBuildFirOfType<FirElementWithResolveState>(resolutionFacade)
                firDeclaration.lazyResolveToPhase(phase)
            }
        }
    }
}