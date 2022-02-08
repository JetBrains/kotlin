/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirOverrideInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbolProvider
import org.jetbrains.kotlin.analysis.api.fir.utils.threadLocal
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KtFirAnalysisSession
private constructor(
    private val project: Project,
    val firResolveState: LLFirModuleResolveState,
    internal val firSymbolBuilder: KtSymbolByFirBuilder,
    token: ValidityToken,
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

    override val samResolverImpl = KtFirSamResolver(this, token)

    override val scopeProviderImpl by threadLocal { KtFirScopeProvider(this, firSymbolBuilder, project, firResolveState, token) }

    override val symbolProviderImpl =
        KtFirSymbolProvider(this, firResolveState.rootModuleSession.symbolProvider, firResolveState, firSymbolBuilder, token)

    override val completionCandidateCheckerImpl = KtFirCompletionCandidateChecker(this, token)

    override val symbolDeclarationOverridesProviderImpl =
        KtFirSymbolDeclarationOverridesProvider(this, token)

    override val referenceShortenerImpl = KtFirReferenceShortener(this, token, firResolveState)

    override val importOptimizerImpl: KtImportOptimizer = KtFirImportOptimizer(token, firResolveState)

    override val symbolDeclarationRendererProviderImpl: KtSymbolDeclarationRendererProvider =
        KtFirSymbolDeclarationRendererProvider(this, token)

    override val expressionInfoProviderImpl = KtFirExpressionInfoProvider(this, token)

    override val compileTimeConstantProviderImpl: KtCompileTimeConstantProvider = KtFirCompileTimeConstantProvider(this, token)

    override val overrideInfoProviderImpl = KtFirOverrideInfoProvider(this, token)

    override val visibilityCheckerImpl: KtVisibilityChecker = KtFirVisibilityChecker(this, token)

    override val psiTypeProviderImpl = KtFirPsiTypeProvider(this, token)

    override val jvmTypeMapperImpl = KtFirJvmTypeMapper(this, token)

    override val typeProviderImpl = KtFirTypeProvider(this, token)

    override val typeInfoProviderImpl = KtFirTypeInfoProvider(this, token)

    override val subtypingComponentImpl = KtFirSubtypingComponent(this, token)

    override val inheritorsProviderImpl: KtInheritorsProvider = KtFirInheritorsProvider(this, token)

    override val symbolInfoProviderImpl: KtSymbolInfoProvider = KtFirSymbolInfoProvider(this, token)

    override val typesCreatorImpl: KtTypeCreator = KtFirTypeCreator(this, token)

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
            AnalysisSessionMode.DEPENDENT_COPY
        )
    }

    val rootModuleSession: FirSession get() = firResolveState.rootModuleSession
    val firSymbolProvider: FirSymbolProvider get() = rootModuleSession.symbolProvider
    val targetPlatform: TargetPlatform get() = rootModuleSession.moduleData.platform

    companion object {
        @InvalidWayOfUsingAnalysisSession
        internal fun createAnalysisSessionByResolveState(
            firResolveState: LLFirModuleResolveState,
            token: ValidityToken,
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
                AnalysisSessionMode.REGULAR,
            )
        }
    }
}
