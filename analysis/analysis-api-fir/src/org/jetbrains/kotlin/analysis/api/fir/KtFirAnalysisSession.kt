/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirOverrideInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbolProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KtAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
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

@OptIn(KtAnalysisApiInternals::class, KtAnalysisNonPublicApi::class)
@Suppress("AnalysisApiMissingLifetimeCheck")
internal class KtFirAnalysisSession
private constructor(
    val project: Project,
    val firResolveSession: LLFirResolveSession,
    token: KtLifetimeToken,
) : KtAnalysisSession(token) {

    internal val firSymbolBuilder: KtSymbolByFirBuilder = KtSymbolByFirBuilder(project, this, token)

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override val useSiteModule: KtModule get() = firResolveSession.useSiteKtModule

    override val smartCastProviderImpl = KtFirSmartcastProvider(this, token)

    override val expressionTypeProviderImpl = KtFirExpressionTypeProvider(this, token)

    override val diagnosticProviderImpl = KtFirDiagnosticProvider(this, token)

    override val containingDeclarationProviderImpl = KtFirSymbolContainingDeclarationProvider(this, token)

    override val callResolverImpl = KtFirCallResolver(this, token)

    override val samResolverImpl = KtFirSamResolver(this, token)

    override val scopeProviderImpl = KtFirScopeProvider(this, firSymbolBuilder, firResolveSession)

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

    override val originalPsiProviderImpl: KtOriginalPsiProvider = KtFirOriginalPsiProvider(this, token)

    override val symbolInfoProviderImpl: KtSymbolInfoProvider = KtFirSymbolInfoProvider(this, token)

    override val typesCreatorImpl: KtTypeCreator = KtFirTypeCreator(this, token)

    override val analysisScopeProviderImpl: KtAnalysisScopeProvider

    override val referenceResolveProviderImpl: KtReferenceResolveProvider = KtFirReferenceResolveProvider(this)

    override val signatureSubstitutorImpl: KtSignatureSubstitutor = KtFirSignatureSubstitutor(this)

    override val scopeSubstitutionImpl: KtScopeSubstitution = KtFirScopeSubstitution(this)

    override val substitutorFactoryImpl: KtSubstitutorFactory = KtFirSubstitutorFactory(this)

    override val symbolProviderByJavaPsiImpl = KtFirSymbolProviderByJavaPsi(this)

    override val resolveExtensionInfoProviderImpl: KtResolveExtensionInfoProvider = KtFirResolveExtensionInfoProvider(this)

    override val compilerFacilityImpl: KtCompilerFacility = KtFirCompilerFacility(this)

    override val metadataCalculatorImpl: KtMetadataCalculator = KtFirMetadataCalculator(this)

    override val substitutorProviderImpl: KtSubstitutorProvider = KtFirSubstitutorProvider(this)

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
        analysisScopeProviderImpl = KtAnalysisScopeProviderImpl(this, token, shadowedScope)
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
            token: KtLifetimeToken,
        ): KtFirAnalysisSession {
            return KtFirAnalysisSession(firResolveSession.project, firResolveSession, token)
        }
    }
}
