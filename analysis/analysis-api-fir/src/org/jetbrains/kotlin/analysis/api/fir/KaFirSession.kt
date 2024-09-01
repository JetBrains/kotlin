/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.components.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbolProvider
import org.jetbrains.kotlin.analysis.api.impl.base.KaBaseSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaRendererImpl
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaGlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.createSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.llResolveExtensionTool
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCompositePackageProvider
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
    val firResolveSession: LLFirResolveSession,
    val extensionTools: List<LLFirResolveExtensionTool>,
    token: KaLifetimeToken,
    analysisSessionProvider: () -> KaFirSession,
    useSiteScope: KaGlobalSearchScope
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
    symbolProvider = KaFirSymbolProvider(analysisSessionProvider, firResolveSession.useSiteFirSession.symbolProvider),
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
    analysisScopeProvider = KaAnalysisScopeProviderImpl(analysisSessionProvider, useSiteScope),
    signatureSubstitutor = KaFirSignatureSubstitutor(analysisSessionProvider),
    resolveExtensionInfoProvider = KaFirResolveExtensionInfoProvider(analysisSessionProvider),
    compilerPluginGeneratedDeclarationsProvider = KaFirCompilerPluginGeneratedDeclarationsProvider(analysisSessionProvider),
    compilerFacility = KaFirCompilerFacility(analysisSessionProvider),
    metadataCalculator = KaFirMetadataCalculator(analysisSessionProvider),
    substitutorProvider = KaFirSubstitutorProvider(analysisSessionProvider),
    dataFlowProvider = KaFirDataFlowProvider(analysisSessionProvider),
    sourceProvider = KaFirSourceProvider(analysisSessionProvider)
) {
    internal val firSymbolBuilder: KaSymbolByFirBuilder by lazy {
        KaSymbolByFirBuilder(project, this, token)
    }

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override val useSiteModule: KaModule get() = firResolveSession.useSiteKtModule

    internal val firSession: FirSession get() = firResolveSession.useSiteFirSession
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
