/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLResolutionFacadeService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.elementCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.getFirForNonKtFileElementNoAnalysis
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*

private val PHASES_TO_TRIGGER = listOf(
    FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS,
    FirResolvePhase.SUPER_TYPES,
    FirResolvePhase.TYPES,
    FirResolvePhase.STATUS,
    FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
)

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

    @OptIn(LLFirInternals::class)
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

        val modificationTracker = PsiModificationTracker.getInstance(project)
        val initialModificationCount = modificationTracker.modificationCount

        val module = resolutionFacade.getModule(this)
        val session = resolutionFacade.sessionProvider.getResolvableSession(module)

        for (phase in PHASES_TO_TRIGGER) {
            for (declaration in declarations) {
                pool {
                    runReadAction {
                        if (!declaration.isValid) {
                            return@runReadAction
                        }

                        val firElement = if (modificationTracker.modificationCount == initialModificationCount) {
                            session.getFirForNonKtFileElementNoAnalysis(declaration)
                        } else {
                            val project = module.project
                            val newModule = KotlinProjectStructureProvider.getModule(project, declaration, useSiteModule = null)
                            val newResolutionFacade = LLResolutionFacadeService.getInstance(project).getResolutionFacade(newModule)
                            val newSession = newResolutionFacade.sessionProvider.getResolvableSession(newModule)
                            newSession.getFirForNonKtFileElementNoAnalysis(declaration)
                        }

                        if (firElement is FirElementWithResolveState) {
                            firElement.lazyResolveToPhase(phase)
                        }
                    }
                }
            }
        }

        val checkerFilter = DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS

        for (declaration in declarations) {
            pool {
                runReadAction {
                    if (declaration.isValid) {
                        declaration.getDiagnostics(resolutionFacade, checkerFilter)
                    }
                }
            }
        }
    }
}