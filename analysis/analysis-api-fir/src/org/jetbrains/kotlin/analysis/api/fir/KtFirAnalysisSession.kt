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
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaGlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.createSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.llResolveExtensionTool
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCompositePackageProvider
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
    val extensionTools: List<LLFirResolveExtensionTool>,
    token: KaLifetimeToken,
    analysisSessionProvider: () -> KaFirSession,
    useSiteScope: KaGlobalSearchScope
) : KaSession(
    token,
    resolver = KaFirResolver(analysisSessionProvider, token),
    symbolRelationProvider = KaFirSymbolRelationProvider(analysisSessionProvider, token),
    diagnosticProvider = KaFirDiagnosticProvider(analysisSessionProvider, token),
    scopeProvider = KaFirScopeProvider(analysisSessionProvider, token),
    completionCandidateChecker = KaFirCompletionCandidateChecker(analysisSessionProvider, token),
    javaInteroperabilityComponent = KaFirJavaInteroperabilityComponent(analysisSessionProvider, token),
    typeRelationChecker = KaFirTypeRelationChecker(analysisSessionProvider, token),
    evaluator = KaFirEvaluator(analysisSessionProvider, token),
    referenceShortener = KaFirReferenceShortener(analysisSessionProvider, token),
    importOptimizer = KaFirImportOptimizer(analysisSessionProvider, token),
    visibilityChecker = KaFirVisibilityChecker(analysisSessionProvider, token),
    originalPsiProvider = KaFirOriginalPsiProvider(analysisSessionProvider, token),
    typeCreator = KaFirTypeCreator(analysisSessionProvider, token),
    analysisScopeProvider = KaAnalysisScopeProviderImpl(analysisSessionProvider, token, useSiteScope),
    signatureSubstitutor = KaFirSignatureSubstitutor(analysisSessionProvider, token),
    resolveExtensionInfoProvider = KaFirResolveExtensionInfoProvider(analysisSessionProvider, token),
    compilerFacility = KaFirCompilerFacility(analysisSessionProvider, token),
    metadataCalculator = KaFirMetadataCalculator(analysisSessionProvider, token),
    dataFlowProvider = KaFirDataFlowProvider(analysisSessionProvider, token),
    sourceProvider = KaFirSourceProvider(analysisSessionProvider, token)
) {
    internal val firSymbolBuilder: KaSymbolByFirBuilder by lazy {
        KaSymbolByFirBuilder(project, this, token)
    }

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override val useSiteModule: KtModule get() = firResolveSession.useSiteKtModule

    override val expressionTypeProviderImpl = KaFirExpressionTypeProvider(this, token)

    override val symbolProviderImpl =
        KaFirSymbolProvider(this, firResolveSession.useSiteFirSession.symbolProvider)

    override val symbolDeclarationRendererProviderImpl: KaSymbolDeclarationRendererProvider = KaFirRendererProvider(this, token)

    override val expressionInfoProviderImpl = KaFirExpressionInfoProvider(this, token)

    override val overrideInfoProviderImpl = KaFirOverrideInfoProvider(this, token)

    override val typeProviderImpl = KaFirTypeProvider(this, token)

    override val typeInfoProviderImpl = KaFirTypeInfoProvider(this, token)

    override val symbolInfoProviderImpl: KaSymbolInfoProvider = KaFirSymbolInfoProvider(this, token)

    override val substitutorFactoryImpl: KaSubstitutorFactory = KaFirSubstitutorFactory(this)

    override val substitutorProviderImpl: KaSubstitutorProvider = KaFirSubstitutorProvider(this)

    internal val useSiteSession: FirSession get() = firResolveSession.useSiteFirSession
    internal val firSymbolProvider: FirSymbolProvider get() = useSiteSession.symbolProvider
    internal val targetPlatform: TargetPlatform get() = useSiteSession.moduleData.platform

    val useSiteScopeDeclarationProvider: KotlinDeclarationProvider
    val useSitePackageProvider: KotlinPackageProvider


    init {
        useSiteScopeDeclarationProvider = KotlinCompositeDeclarationProvider.create(
            buildList {
                add(project.createDeclarationProvider(useSiteScope, useSiteModule))
                extensionTools.mapTo(this) { it.declarationProvider }
            }
        )

        useSitePackageProvider = KotlinCompositePackageProvider.create(
            buildList {
                add(project.createPackageProvider(useSiteScope))
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
            val useSiteModule = firResolveSession.useSiteKtModule
            val useSiteSession = firResolveSession.useSiteFirSession

            val extensionTools = buildList {
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

            val resolutionScope = KaGlobalSearchScope(shadowedScope, useSiteModule)

            return createSession {
                KaFirSession(
                    firResolveSession.project,
                    firResolveSession,
                    extensionTools,
                    token,
                    analysisSessionProvider,
                    resolutionScope
                )
            }
        }
    }
}
