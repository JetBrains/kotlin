/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.api.*
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtInheritorsProvider
import org.jetbrains.kotlin.idea.frontend.api.components.KtVisibilityChecker
import org.jetbrains.kotlin.idea.frontend.api.components.KtSymbolDeclarationRendererProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.components.*
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirOverrideInfoProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbolProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.threadLocal
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirAnalysisSession
private constructor(
    private val project: Project,
    val firResolveState: FirModuleResolveState,
    internal val firSymbolBuilder: KtSymbolByFirBuilder,
    token: ValidityToken,
    element: KtElement,
    private val mode: AnalysisSessionMode,
) : KtAnalysisSession(token) {

    private enum class AnalysisSessionMode {
        REGULAR,
        DEPENDENT_COPY
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

    override val symbolDeclarationRendererProviderImpl: KtSymbolDeclarationRendererProvider =
        KtFirSymbolDeclarationRendererProvider(this, token)

    override val expressionInfoProviderImpl = KtFirExpressionInfoProvider(this, token)

    override val overrideInfoProviderImpl = KtFirOverrideInfoProvider(this, token)

    override val visibilityCheckerImpl: KtVisibilityChecker = KtFirVisibilityChecker(this, token)

    override val psiTypeProviderImpl = KtFirPsiTypeProvider(this, token)

    override val typeProviderImpl = KtFirTypeProvider(this, token)

    override val typeInfoProviderImpl = KtFirTypeInfoProvider(this, token)

    override val subtypingComponentImpl = KtFirSubtypingComponent(this, token)

    override val inheritorsProviderImpl: KtInheritorsProvider = KtFirInheritorsProvider(this, token)

    override fun createContextDependentCopy(originalKtFile: KtFile, elementToReanalyze: KtElement): KtAnalysisSession {
        check(mode == AnalysisSessionMode.REGULAR) {
            "Cannot create context-dependent copy of KtAnalysis session from a context dependent one"
        }
        require(!elementToReanalyze.isPhysical) { "Depended context should be build only for non-physical elements" }

        val contextResolveState = LowLevelFirApiFacadeForResolveOnAir.getResolveStateForDependentCopy(
            originalState = firResolveState,
            originalKtFile = originalKtFile,
            elementToAnalyze = elementToReanalyze
        )

        return KtFirAnalysisSession(
            project,
            contextResolveState,
            firSymbolBuilder.createReadOnlyCopy(contextResolveState),
            token,
            originalKtFile,
            AnalysisSessionMode.DEPENDENT_COPY
        )
    }

    val rootModuleSession: FirSession get() = firResolveState.rootModuleSession
    val firSymbolProvider: FirSymbolProvider get() = rootModuleSession.symbolProvider
    val targetPlatform: TargetPlatform get() = rootModuleSession.moduleData.platform
    val searchScope: GlobalSearchScope = KotlinSourceFilterScope.projectSourceAndClassFiles(element.resolveScope, project)

    companion object {
        @InvalidWayOfUsingAnalysisSession
        @Deprecated("Please use org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProviderKt.analyze")
        internal fun createAnalysisSessionByResolveState(
            firResolveState: FirModuleResolveState,
            token: ValidityToken,
            element: KtElement,
        ): KtFirAnalysisSession {
            val project = firResolveState.project
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
                element,
                AnalysisSessionMode.REGULAR,
            )
        }
    }
}
