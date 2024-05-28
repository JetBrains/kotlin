/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirOverrideInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbolProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.llResolveExtensionTool
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependencies
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.analysis.providers.impl.declarationProviders.CompositeKotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.impl.packageProviders.CompositeKotlinPackageProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * The lifetime and validity of a cached [KaFirSession] depends on the lifetime of the underlying
 * [LLFirSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession]. This requires the [KaFirSession] to keep a
 * strong reference to the `LLFirSession`. See the documentation of [LLFirSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession]
 * for more information.
 */
@OptIn(KaAnalysisApiInternals::class, KaAnalysisNonPublicApi::class)
@Suppress("AnalysisApiMissingLifetimeCheck")
internal class KaFirSession
private constructor(
    val project: Project,
    val firResolveSession: LLFirResolveSession,
    token: KaLifetimeToken,
) : KaSession(token) {

    internal val firSymbolBuilder: KaSymbolByFirBuilder = KaSymbolByFirBuilder(project, this, token)

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override val useSiteModule: KtModule get() = firResolveSession.useSiteKtModule

    override val smartCastProviderImpl = KaFirSmartcastProvider(this, token)

    override val expressionTypeProviderImpl = KaFirExpressionTypeProvider(this, token)

    override val diagnosticProviderImpl = KaFirDiagnosticProvider(this, token)

    override val containingDeclarationProviderImpl = KaFirSymbolContainingDeclarationProvider(this, token)

    override val resolverImpl = KaFirResolver(this)

    override val samResolverImpl = KaFirSamResolver(this)

    override val scopeProviderImpl = KaFirScopeProvider(this, firSymbolBuilder, firResolveSession)

    override val symbolProviderImpl =
        KaFirSymbolProvider(this, firResolveSession.useSiteFirSession.symbolProvider)

    override val completionCandidateCheckerImpl = KaFirCompletionCandidateChecker(this, token)

    override val symbolDeclarationOverridesProviderImpl =
        KaFirSymbolDeclarationOverridesProvider(this, token)

    override val referenceShortenerImpl = KaFirReferenceShortener(this, token, firResolveSession)

    override val importOptimizerImpl: KaImportOptimizer = KaFirImportOptimizer(this, token, firResolveSession)

    override val symbolDeclarationRendererProviderImpl: KaSymbolDeclarationRendererProvider = KaFirRendererProvider(this, token)

    override val expressionInfoProviderImpl = KaFirExpressionInfoProvider(this, token)

    override val compileTimeConstantProviderImpl: KaCompileTimeConstantProvider = KaFirCompileTimeConstantProvider(this, token)

    override val overrideInfoProviderImpl = KaFirOverrideInfoProvider(this, token)

    override val visibilityCheckerImpl: KaVisibilityChecker = KaFirVisibilityChecker(this, token)

    override val psiTypeProviderImpl = KaFirPsiTypeProvider(this, token)

    override val jvmTypeMapperImpl = KaFirJvmTypeMapper(this, token)

    override val typeProviderImpl = KaFirTypeProvider(this, token)

    override val typeInfoProviderImpl = KaFirTypeInfoProvider(this, token)

    override val subtypingComponentImpl = KaFirSubtypingComponent(this, token)

    override val inheritorsProviderImpl: KaInheritorsProvider = KaFirInheritorsProvider(this, token)

    override val multiplatformInfoProviderImpl: KaMultiplatformInfoProvider = KaFirMultiplatformInfoProvider(this, token)

    override val originalPsiProviderImpl: KaOriginalPsiProvider = KaFirOriginalPsiProvider(this, token)

    override val symbolInfoProviderImpl: KaSymbolInfoProvider = KaFirSymbolInfoProvider(this, token)

    override val typesCreatorImpl: KaTypeCreator = KaFirTypeCreator(this, token)

    override val analysisScopeProviderImpl: KaAnalysisScopeProvider

    override val referenceResolveProviderImpl: KaReferenceResolveProvider = KaFirReferenceResolveProvider(this)

    override val signatureSubstitutorImpl: KaSignatureSubstitutor = KaFirSignatureSubstitutor(this)

    override val scopeSubstitutionImpl: KaScopeSubstitution = KaFirScopeSubstitution(this)

    override val substitutorFactoryImpl: KaSubstitutorFactory = KaFirSubstitutorFactory(this)

    override val symbolProviderByJavaPsiImpl = KaFirSymbolProviderByJavaPsi(this)

    override val resolveExtensionInfoProviderImpl: KaResolveExtensionInfoProvider = KaFirResolveExtensionInfoProvider(this)

    override val compilerFacilityImpl: KaCompilerFacility = KaFirCompilerFacility(this)

    override val metadataCalculatorImpl: KaMetadataCalculator = KaFirMetadataCalculator(this)

    override val substitutorProviderImpl: KaSubstitutorProvider = KaFirSubstitutorProvider(this)

    override val dataFlowInfoProviderImpl: KaDataFlowInfoProvider = KaFirDataFlowInfoProvider(this)

    override val klibSourceFileProviderImpl: KaKlibSourceFileNameProvider = KaFirKlibSourceFileNameProvider(this)

    internal val useSiteSession: FirSession get() = firResolveSession.useSiteFirSession
    internal val firSymbolProvider: FirSymbolProvider get() = useSiteSession.symbolProvider
    internal val targetPlatform: TargetPlatform get() = useSiteSession.moduleData.platform

    val extensionTools: List<LLFirResolveExtensionTool>

    val useSiteAnalysisScope: GlobalSearchScope

    val useSiteScopeDeclarationProvider: KotlinDeclarationProvider
    val useSitePackageProvider: KotlinPackageProvider


    init {
        extensionTools = buildList {
            addIfNotNull(useSiteSession.llResolveExtensionTool)
            useSiteModule.allDirectDependencies().mapNotNullTo(this) { dependency ->
                firResolveSession.getSessionFor(dependency).llResolveExtensionTool
            }
        }

        val shadowedScope = GlobalSearchScope.union(
            buildSet {
                // Add an empty scope to the shadowed set to give GlobalSearchScope.union something
                // to work with if there are no extension tools.
                // If there are extension tools, any empty scopes, whether from shadowedSearchScope
                // on the extension tools or from this add() call, will be ignored.
                add(GlobalSearchScope.EMPTY_SCOPE)
                extensionTools.mapTo(this) { it.shadowedSearchScope }
            }
        )
        analysisScopeProviderImpl = KaAnalysisScopeProviderImpl(this, token, shadowedScope)
        useSiteAnalysisScope = analysisScopeProviderImpl.getAnalysisScope()

        useSiteScopeDeclarationProvider = CompositeKotlinDeclarationProvider.create(
            buildList {
                add(project.createDeclarationProvider(useSiteAnalysisScope, useSiteModule))
                extensionTools.mapTo(this) { it.declarationProvider }
            }
        )

        useSitePackageProvider = CompositeKotlinPackageProvider.create(
            buildList {
                add(project.createPackageProvider(useSiteAnalysisScope))
                extensionTools.mapTo(this) { it.packageProvider }
            }
        )
    }

    fun getScopeSessionFor(session: FirSession): ScopeSession = withValidityAssertion { firResolveSession.getScopeSessionFor(session) }

    companion object {
        internal fun createAnalysisSessionByFirResolveSession(
            firResolveSession: LLFirResolveSession,
            token: KaLifetimeToken,
        ): KaFirSession {
            return KaFirSession(firResolveSession.project, firResolveSession, token)
        }
    }
}
