/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirOverrideInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbolProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KtAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@OptIn(KtAnalysisApiInternals::class)
@Suppress("AnalysisApiMissingLifetimeCheck")
internal class KtFirAnalysisSession
private constructor(
    val project: Project,
    val firResolveSession: LLFirResolveSession,
    token: KtLifetimeToken,
    private val mode: AnalysisSessionMode,
) : KtAnalysisSession(token) {

    internal val firSymbolBuilder: KtSymbolByFirBuilder = KtSymbolByFirBuilder(project, this, token)

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override val useSiteModule: KtModule get() = firResolveSession.useSiteKtModule

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

    override val scopeProviderImpl = KtFirScopeProvider(this, firSymbolBuilder, project, firResolveSession)

    override val symbolProviderImpl =
        KtFirSymbolProvider(this, firResolveSession.useSiteFirSession.symbolProvider)

    override val completionCandidateCheckerImpl = KtFirCompletionCandidateChecker(this, token)

    override val symbolDeclarationOverridesProviderImpl =
        KtFirSymbolDeclarationOverridesProvider(this, token)

    override val referenceShortenerImpl = KtFirReferenceShortener(this, token, firResolveSession)

    override val importOptimizerImpl: KtImportOptimizer = KtFirImportOptimizer(token, firResolveSession)

    override val symbolDeclarationRendererProviderImpl: KtSymbolDeclarationRendererProvider = KtFirRendererProvider(this, token)

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

    override val multiplatformInfoProviderImpl: KtMultiplatformInfoProvider = KtFirMultiplatformInfoProvider(this, token)

    override val symbolInfoProviderImpl: KtSymbolInfoProvider = KtFirSymbolInfoProvider(this, token)

    override val typesCreatorImpl: KtTypeCreator = KtFirTypeCreator(this, token)

    override val analysisScopeProviderImpl: KtAnalysisScopeProvider = KtAnalysisScopeProviderImpl(this, token)

    override val referenceResolveProviderImpl: KtReferenceResolveProvider = KtFirReferenceResolveProvider(this)

    override val signatureSubstitutorImpl: KtSignatureSubstitutor = KtFirSignatureSubstitutor(this)

    override val scopeSubstitutionImpl: KtScopeSubstitution = KtFirScopeSubstitution(this)

    override val substitutorFactoryImpl: KtSubstitutorFactory = KtFirSubstitutorFactory(this)

    override val symbolProviderByJavaPsiImpl = KtFirSymbolProviderByJavaPsi(this)

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override fun createContextDependentCopy(originalKtFile: KtFile, elementToReanalyze: KtElement): KtAnalysisSession {
        check(mode == AnalysisSessionMode.REGULAR) {
            "Cannot create context-dependent copy of KtAnalysis session from a context dependent one"
        }
        require(!elementToReanalyze.isPhysical) { "Depended context should be build only for non-physical elements" }

        val contextFirResolveSession = LowLevelFirApiFacadeForResolveOnAir.getFirResolveSessionForDependentCopy(
            originalFirResolveSession = firResolveSession,
            originalKtFile = originalKtFile,
            elementToAnalyze = elementToReanalyze
        )

        return KtFirAnalysisSession(
            project,
            contextFirResolveSession,
            token,
            AnalysisSessionMode.DEPENDENT_COPY
        )
    }

    internal val useSiteSession: FirSession get() = firResolveSession.useSiteFirSession
    internal val firSymbolProvider: FirSymbolProvider get() = useSiteSession.symbolProvider
    internal val targetPlatform: TargetPlatform get() = useSiteSession.moduleData.platform

    val useSiteAnalysisScope: GlobalSearchScope = analysisScopeProviderImpl.getAnalysisScope()
    val useSiteScopeDeclarationProvider: KotlinDeclarationProvider = project.createDeclarationProvider(useSiteAnalysisScope)

    fun getScopeSessionFor(session: FirSession): ScopeSession = withValidityAssertion { firResolveSession.getScopeSessionFor(session) }

    companion object {
        internal fun createAnalysisSessionByFirResolveSession(
            firResolveSession: LLFirResolveSession,
            token: KtLifetimeToken,
        ): KtFirAnalysisSession {
            val project = firResolveSession.project

            return KtFirAnalysisSession(
                project,
                firResolveSession,
                token,
                AnalysisSessionMode.REGULAR,
            )
        }
    }
}
