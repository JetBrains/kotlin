/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.ReadActionConfinementValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.assertIsValid
import org.jetbrains.kotlin.idea.frontend.api.components.*
import org.jetbrains.kotlin.idea.frontend.api.fir.components.*
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbolProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.threadLocal
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolProvider
import org.jetbrains.kotlin.psi.KtElement

internal class KtFirAnalysisSession
private constructor(
    private val element: KtElement,
    val firResolveState: FirModuleResolveState,
    internal val firSymbolBuilder: KtSymbolByFirBuilder,
    token: ValidityToken,
    val isContextSession: Boolean,
) : KtAnalysisSession(token) {
    init {
        assertIsValid()
    }

    private val project = element.project

    override val smartCastProvider: KtSmartCastProvider = KtFirSmartcastProvider(this, token)
    override val typeProvider: KtTypeProvider = KtFirTypeProvider(this, token)
    override val diagnosticProvider: KtDiagnosticProvider = KtFirDiagnosticProvider(this, token)
    override val containingDeclarationProvider = KtFirSymbolContainingDeclarationProvider(this, token)
    override val callResolver: KtCallResolver = KtFirCallResolver(this, token)
    override val scopeProvider by threadLocal { KtFirScopeProvider(this, firSymbolBuilder, project, firResolveState, token) }
    override val symbolProvider: KtSymbolProvider =
        KtFirSymbolProvider(this, firResolveState.firIdeLibrariesSession.firSymbolProvider, firResolveState, firSymbolBuilder, token)
    override val completionCandidateChecker: KtCompletionCandidateChecker by threadLocal { KtFirCompletionCandidateChecker(this, token) }
    override val symbolDeclarationOverridesProvider: KtSymbolDeclarationOverridesProvider
            by threadLocal { KtFirSymbolDeclarationOverridesProvider(this, token) }

    override fun createContextDependentCopy(): KtAnalysisSession {
        check(!isContextSession) { "Cannot create context-dependent copy of KtAnalysis session from a context dependent one" }
        val contextResolveState = LowLevelFirApiFacade.getResolveStateForCompletion(element, firResolveState)
        return KtFirAnalysisSession(
            element,
            contextResolveState,
            firSymbolBuilder.createReadOnlyCopy(contextResolveState),
            token,
            isContextSession = true
        )
    }

    companion object {
        @Deprecated("Please use org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProviderKt.analyze")
        internal fun createForElement(element: KtElement): KtFirAnalysisSession {
            val firResolveState = LowLevelFirApiFacade.getResolveStateFor(element)
            val project = element.project
            val token = ReadActionConfinementValidityToken(project)
            val firSymbolBuilder = KtSymbolByFirBuilder(
                firResolveState,
                project,
                token
            )
            return KtFirAnalysisSession(
                element,
                firResolveState,
                firSymbolBuilder,
                token,
                isContextSession = false
            )
        }
    }
}

