/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacadeForCompletion
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
    private val project: Project,
    val firResolveState: FirModuleResolveState,
    internal val firSymbolBuilder: KtSymbolByFirBuilder,
    token: ValidityToken,
    val isContextSession: Boolean,
) : KtAnalysisSession(token) {
    init {
        assertIsValid()
    }

    override val smartCastProvider: KtSmartCastProvider = KtFirSmartcastProvider(this, token)
    override val typeProvider: KtTypeProvider = KtFirTypeProvider(this, token)
    override val diagnosticProvider: KtDiagnosticProvider = KtFirDiagnosticProvider(this, token)
    override val containingDeclarationProvider = KtFirSymbolContainingDeclarationProvider(this, token)
    override val callResolver: KtCallResolver = KtFirCallResolver(this, token)
    override val scopeProvider by threadLocal { KtFirScopeProvider(this, firSymbolBuilder, project, firResolveState, token) }
    override val symbolProvider: KtSymbolProvider =
        KtFirSymbolProvider(this, firResolveState.rootModuleSession.firSymbolProvider, firResolveState, firSymbolBuilder, token)
    override val completionCandidateChecker: KtCompletionCandidateChecker = KtFirCompletionCandidateChecker(this, token)
    override val symbolDeclarationOverridesProvider: KtSymbolDeclarationOverridesProvider =
        KtFirSymbolDeclarationOverridesProvider(this, token)

    override fun createContextDependentCopy(): KtAnalysisSession {
        check(!isContextSession) { "Cannot create context-dependent copy of KtAnalysis session from a context dependent one" }
        val contextResolveState = LowLevelFirApiFacadeForCompletion.getResolveStateForCompletion(firResolveState)
        return KtFirAnalysisSession(
            project,
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
            return createAnalysisSessionByResolveState(firResolveState)
        }

        @Deprecated("Please use org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProviderKt.analyze")
        internal fun createAnalysisSessionByResolveState(firResolveState: FirModuleResolveState): KtFirAnalysisSession {
            val project = firResolveState.project
            val token = ReadActionConfinementValidityToken(project)
            val firSymbolBuilder = KtSymbolByFirBuilder(
                firResolveState,
                project,
                token
            )
            return KtFirAnalysisSession(
                project,
                firResolveState,
                firSymbolBuilder,
                token,
                isContextSession = false
            )
        }
    }
}

