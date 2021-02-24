/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacadeForCompletion
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getFirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getResolveState
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.ReadActionConfinementValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.assertIsValidAndAccessible
import org.jetbrains.kotlin.idea.frontend.api.fir.components.*
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbolProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.EnclosingDeclarationContext
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.recordCompletionContext
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.threadLocal
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirAnalysisSession
private constructor(
    private val project: Project,
    val firResolveState: FirModuleResolveState,
    internal val firSymbolBuilder: KtSymbolByFirBuilder,
    token: ValidityToken,
    val context: KtFirAnalysisSessionContext,
) : KtAnalysisSession(token) {
    init {
        assertIsValidAndAccessible()
    }

    override val smartCastProviderImpl = KtFirSmartcastProvider(this, token)

    override val expressionTypeProviderImpl = KtFirExpressionTypeProvider(this, token)

    override val diagnosticProviderImpl = KtFirDiagnosticProvider(this, token)

    override val containingDeclarationProviderImpl = KtFirSymbolContainingDeclarationProvider(this, token)

    override val callResolverImpl = KtFirCallResolver(this, token)

    override val scopeProviderImpl by threadLocal { KtFirScopeProvider(this, firSymbolBuilder, project, firResolveState, token) }

    override val symbolProviderImpl =
        KtFirSymbolProvider(this, firResolveState.rootModuleSession.symbolProvider, firResolveState, firSymbolBuilder, token)

    override val completionCandidateCheckerImpl = KtFirCompletionCandidateChecker(this, token)

    override val symbolDeclarationOverridesProviderImpl =
        KtFirSymbolDeclarationOverridesProvider(this, token)

    override val referenceShortenerImpl = KtFirReferenceShortener(this, token, firResolveState)

    override val expressionInfoProviderImpl = KtFirExpressionInfoProvider(this, token)

    override val typeProviderImpl = KtFirTypeProvider(this, token)

    override val subtypingComponentImpl = KtFirSubtypingComponent(this, token)

    override fun createContextDependentCopy(originalKtFile: KtFile, fakeKtElement: KtElement): KtAnalysisSession {
        check(context == KtFirAnalysisSessionContext.DefaultContext) {
            "Cannot create context-dependent copy of KtAnalysis session from a context dependent one"
        }
        val contextResolveState = LowLevelFirApiFacadeForCompletion.getResolveStateForCompletion(firResolveState)
        val originalFirFile = originalKtFile.getFirFile(firResolveState)
        val context = KtFirAnalysisSessionContext.FakeFileContext(originalKtFile, originalFirFile, fakeKtElement, contextResolveState)
        return KtFirAnalysisSession(
            project,
            contextResolveState,
            firSymbolBuilder.createReadOnlyCopy(contextResolveState),
            token,
            context
        )
    }

    companion object {
        @Deprecated("Please use org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProviderKt.analyze")
        internal fun createForElement(element: KtElement): KtFirAnalysisSession {
            val firResolveState = element.getResolveState()
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
                KtFirAnalysisSessionContext.DefaultContext
            )
        }
    }
}

internal sealed class KtFirAnalysisSessionContext {
    object DefaultContext : KtFirAnalysisSessionContext()

    class FakeFileContext(
        originalFile: KtFile,
        firFile: FirFile,
        fakeContextElement: KtElement,
        fakeModuleResolveState: FirModuleResolveState
    ) : KtFirAnalysisSessionContext() {
        init {
            require(!fakeContextElement.isPhysical)

            val enclosingContext = EnclosingDeclarationContext.detect(originalFile, fakeContextElement)
            enclosingContext.recordCompletionContext(firFile, fakeModuleResolveState)
        }

        val fakeKtFile = fakeContextElement.containingKtFile
    }
}

