/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbolProvider
import org.jetbrains.kotlin.analysis.api.impl.base.KaBaseSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaRendererImpl
import org.jetbrains.kotlin.analysis.api.impl.base.util.createSession
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
    useSiteScope: KaResolutionScope
) : KaBaseSession(
    token,
    resolver = KaFirResolver(analysisSessionProvider),
    symbolRelationProvider = KaFirSymbolRelationProvider(analysisSessionProvider),
    diagnosticProvider = KaFirDiagnosticProvider(analysisSessionProvider),
    scopeProvider = KaFirScopeProvider(analysisSessionProvider),
    completionCandidateChecker = KaFirCompletionCandidateChecker(analysisSessionProvider),
    expressionTypeProvider = KaFirExpressionTypeProvider(analysisSessionProvider),
    typeProvider = KaFirTypeProvider(analysisSessionProvider),
    typeInformationProvider = KaFirTypeInformationProvider(analysisSessionProvider),
    symbolProvider = KaFirSymbolProvider(analysisSessionProvider, resolutionFacade.useSiteFirSession.symbolProvider),
    javaInteroperabilityComponent = KaFirJavaInteroperabilityComponent(analysisSessionProvider),
    symbolInformationProvider = KaFirSymbolInformationProvider(analysisSessionProvider),
    typeRelationChecker = KaFirTypeRelationChecker(analysisSessionProvider),
    expressionInformationProvider = KaFirExpressionInformationProvider(analysisSessionProvider),
    evaluator = KaFirEvaluator(analysisSessionProvider),
    referenceShortener = KaFirReferenceShortener(analysisSessionProvider),
    importOptimizer = KaFirImportOptimizer(analysisSessionProvider),
    renderer = KaRendererImpl(analysisSessionProvider),
    visibilityChecker = KaFirVisibilityChecker(analysisSessionProvider),
    originalPsiProvider = KaFirOriginalPsiProvider(analysisSessionProvider),
    typeCreator = KaFirTypeCreator(analysisSessionProvider),
    analysisScopeProvider = KaBaseAnalysisScopeProviderImpl(analysisSessionProvider, useSiteScope),
    signatureSubstitutor = KaFirSignatureSubstitutor(analysisSessionProvider),
    resolveExtensionInfoProvider = KaFirResolveExtensionInfoProvider(analysisSessionProvider),
    compilerPluginGeneratedDeclarationsProvider = KaFirCompilerPluginGeneratedDeclarationsProvider(analysisSessionProvider),
    compilerFacility = KaFirCompilerFacility(analysisSessionProvider),
    substitutorProvider = KaFirSubstitutorProvider(analysisSessionProvider),
    dataFlowProvider = KaFirDataFlowProvider(analysisSessionProvider),
    sourceProvider = KaFirSourceProvider(analysisSessionProvider)
) {
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
        internal fun createAnalysisSessionByFirResolveSession(
            resolutionFacade: LLResolutionFacade,
            token: KaLifetimeToken,
        ): KaFirSession {
            token.assertIsValid()
            val useSiteModule = resolutionFacade.useSiteModule
            val useSiteSession = resolutionFacade.useSiteFirSession

            val extensionTools = buildList {
                addIfNotNull(useSiteSession.llResolveExtensionTool)
                useSiteModule.allDirectDependencies().mapNotNullTo(this) { dependency ->
                    resolutionFacade.getSessionFor(dependency).llResolveExtensionTool
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
