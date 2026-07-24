/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.components.bridges.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbolProvider
import org.jetbrains.kotlin.analysis.api.impl.base.KaBaseSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaRendererImpl
import org.jetbrains.kotlin.analysis.api.impl.base.util.createSession
import org.jetbrains.kotlin.analysis.api.internals.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValid
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCompositePackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.llResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * The lifetime and validity of a cached [KaFirSession] depends on the lifetime of the underlying
 * [LLFirSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession]. This requires the [KaFirSession] to keep a
 * strong reference to the `LLFirSession`. See the documentation of [LLFirSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession]
 * for more information.
 */
@Suppress("AnalysisApiMissingLifetimeCheck")
internal class KaFirSession
private constructor(
    val project: Project,
    val resolutionFacade: LLResolutionFacade,
    val extensionTools: List<LLFirResolveExtensionTool>,
    token: KaLifetimeToken,
    analysisSessionProvider: () -> KaFirSession,
    useSiteScope: KaResolutionScope,
) : KaBaseSession(
    token,
    resolver = KaResolverBridge(analysisSessionProvider),
    symbolRelationProvider = KaSymbolRelationProviderBridge(analysisSessionProvider),
    diagnosticProvider = KaDiagnosticProviderBridge(analysisSessionProvider),
    scopeProvider = KaScopeProviderBridge(analysisSessionProvider),
    completionCandidateChecker = KaCompletionCandidateCheckerBridge(analysisSessionProvider),
    expressionTypeProvider = KaExpressionTypeProviderBridge(analysisSessionProvider),
    typeProvider = KaTypeProviderBridge(analysisSessionProvider),
    typeInformationProvider = KaTypeInformationProviderBridge(analysisSessionProvider),
    symbolProvider = KaSymbolProviderBridge(analysisSessionProvider),
    javaInteroperabilityComponent = KaJavaInteroperabilityComponentBridge(analysisSessionProvider),
    symbolInformationProvider = KaSymbolInformationProviderBridge(analysisSessionProvider),
    typeRelationChecker = KaTypeRelationCheckerBridge(analysisSessionProvider),
    expressionInformationProvider = KaExpressionInformationProviderBridge(analysisSessionProvider),
    evaluator = KaEvaluatorBridge(analysisSessionProvider),
    referenceShortener = KaReferenceShortenerBridge(analysisSessionProvider),
    renderer = KaRendererBridge(analysisSessionProvider),
    visibilityChecker = KaVisibilityCheckerBridge(analysisSessionProvider),
    typeCreator = KaTypeCreatorBridge(analysisSessionProvider),
    typeCreatorProvider = KaTypeCreatorProviderBridge(analysisSessionProvider),
    analysisScopeProvider = KaAnalysisScopeProviderBridge(analysisSessionProvider),
    signatureSubstitutor = KaSignatureSubstitutorBridge(analysisSessionProvider),
    resolveExtensionInfoProvider = KaResolveExtensionInfoProviderBridge(analysisSessionProvider),
    compilerPluginGeneratedDeclarationsProvider = KaCompilerPluginGeneratedDeclarationsProviderBridge(analysisSessionProvider),
    compilerFacility = KaCompilerFacilityBridge(analysisSessionProvider),
    substitutorProvider = KaSubstitutorProviderBridge(analysisSessionProvider),
    dataFlowProvider = KaDataFlowProviderBridge(analysisSessionProvider),
    sourceProvider = KaSourceProviderBridge(analysisSessionProvider),
    kDocProvider = KaKDocProviderBridge(analysisSessionProvider),
) {
    override val resolver: KaInternalsResolver =
        KaFirResolver(analysisSessionProvider)

    override val diagnosticProvider: KaInternalsDiagnosticProvider =
        KaFirDiagnosticProvider(analysisSessionProvider)

    override val typeRelationChecker: KaInternalsTypeRelationChecker =
        KaFirTypeRelationChecker(analysisSessionProvider)

    override val javaInteroperabilityComponent: KaInternalsJavaInteroperabilityComponent =
        KaFirJavaInteroperabilityComponent(analysisSessionProvider)

    override val visibilityChecker: KaInternalsVisibilityChecker =
        KaFirVisibilityChecker(analysisSessionProvider)

    override val kDocProvider: KaInternalsKDocProvider =
        KaFirKDocProvider(analysisSessionProvider)

    override val renderer: KaInternalsRenderer =
        KaRendererImpl(analysisSessionProvider)

    override val expressionInformationProvider: KaInternalsExpressionInformationProvider =
        KaFirExpressionInformationProvider(analysisSessionProvider)

    override val expressionTypeProvider: KaInternalsExpressionTypeProvider =
        KaFirExpressionTypeProvider(analysisSessionProvider)

    override val sourceProvider: KaInternalsSourceProvider =
        KaFirSourceProvider(analysisSessionProvider)

    override val signatureSubstitutor: KaInternalsSignatureSubstitutor =
        KaFirSignatureSubstitutor(analysisSessionProvider)

    override val resolveExtensionInfoProvider: KaInternalsResolveExtensionInfoProvider =
        KaFirResolveExtensionInfoProvider(analysisSessionProvider)

    override val evaluator: KaInternalsEvaluator =
        KaFirEvaluator(analysisSessionProvider)

    override val referenceShortener: KaInternalsReferenceShortener =
        KaFirReferenceShortener(analysisSessionProvider)

    override val typeCreatorProvider: KaInternalsTypeCreatorProvider =
        KaFirTypeCreatorProvider(analysisSessionProvider)

    override val legacyTypeCreator: KaInternalsTypeCreator =
        KaFirTypeCreator(analysisSessionProvider)

    override val analysisScopeProvider: KaInternalsAnalysisScopeProvider =
        KaBaseAnalysisScopeProviderImpl(analysisSessionProvider, useSiteScope)

    override val completionCandidateChecker: KaInternalsCompletionCandidateChecker =
        KaFirCompletionCandidateChecker(analysisSessionProvider)

    override val symbolProvider: KaInternalsSymbolProvider =
        KaFirSymbolProvider(analysisSessionProvider, resolutionFacade.useSiteFirSession.symbolProvider)

    override val symbolRelationProvider: KaInternalsSymbolRelationProvider =
        KaFirSymbolRelationProvider(analysisSessionProvider)

    override val symbolInformationProvider: KaInternalsSymbolInformationProvider =
        KaFirSymbolInformationProvider(analysisSessionProvider)

    override val typeProvider: KaInternalsTypeProvider =
        KaFirTypeProvider(analysisSessionProvider)

    override val typeInformationProvider: KaInternalsTypeInformationProvider =
        KaFirTypeInformationProvider(analysisSessionProvider)

    override val substitutorProvider: KaInternalsSubstitutorProvider =
        KaFirSubstitutorProvider(analysisSessionProvider)

    override val compilerPluginGeneratedDeclarationsProvider: KaInternalsCompilerPluginGeneratedDeclarationsProvider =
        KaFirCompilerPluginGeneratedDeclarationsProvider(analysisSessionProvider)

    override val compilerFacility: KaInternalsCompilerFacility =
        KaFirCompilerFacility(analysisSessionProvider)

    override val scopeProvider: KaInternalsScopeProvider =
        KaFirScopeProvider(analysisSessionProvider)

    override val dataFlowProvider: KaInternalsDataFlowProvider =
        KaFirDataFlowProvider(analysisSessionProvider)

    internal val firSymbolBuilder: KaSymbolByFirBuilder by lazy {
        KaSymbolByFirBuilder(project, this, token)
    }

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override val useSiteModule: KaModule get() = resolutionFacade.useSiteModule

    internal val firSession: LLFirSession get() = resolutionFacade.useSiteFirSession
    internal val targetPlatform: TargetPlatform get() = firSession.moduleData.platform

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

    val cacheStorage: KaFirInternalCacheStorage by lazy {
        KaFirInternalCacheStorage(this)
    }

    fun getScopeSessionFor(session: FirSession): ScopeSession = withValidityAssertion { resolutionFacade.getScopeSessionFor(session) }

    companion object {
        internal fun createAnalysisSessionByResolutionFacade(
            resolutionFacade: LLResolutionFacade,
            token: KaLifetimeToken,
        ): KaFirSession {
            token.assertIsValid()
            val useSiteModule = resolutionFacade.useSiteModule
            val useSiteSession = resolutionFacade.useSiteFirSession

            val extensionTools = buildList {
                addIfNotNull(useSiteSession.llResolveExtensionTool)
                useSiteModule.allDirectDependencies().mapNotNullTo(this) { dependency ->
                    resolutionFacade.getDependencySessionFor(dependency)?.llResolveExtensionTool
                }
            }

            val resolutionScope = KaResolutionScope.forModule(useSiteModule)

            return createSession {
                KaFirSession(
                    resolutionFacade.project,
                    resolutionFacade,
                    extensionTools,
                    token,
                    analysisSessionProvider,
                    resolutionScope
                )
            }
        }
    }
}
