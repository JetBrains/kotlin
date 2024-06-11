/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.components.*
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaRendererImpl
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaGlobalSearchScope
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.project.structure.KtModule

@OptIn(KaAnalysisApiInternals::class, KaAnalysisNonPublicApi::class)
@Suppress("LeakingThis")
class KaFe10Session(
    val analysisContext: Fe10AnalysisContext,
    override val useSiteModule: KtModule,
    token: KaLifetimeToken,
    analysisSessionProvider: () -> KaFe10Session,
    resolutionScope: KaGlobalSearchScope
) : KaSession(
    token,
    resolver = KaFe10Resolver(analysisSessionProvider, token),
    symbolRelationProvider = KaFe10SymbolRelationProvider(analysisSessionProvider, token),
    diagnosticProvider = KaFe10DiagnosticProvider(analysisSessionProvider, token),
    scopeProvider = KaFe10ScopeProvider(analysisSessionProvider, token),
    completionCandidateChecker = KaFe10CompletionCandidateChecker(analysisSessionProvider, token),
    expressionTypeProvider = KaFe10ExpressionTypeProvider(analysisSessionProvider, token),
    typeInformationProvider = KaFe10TypeInformationProvider(analysisSessionProvider, token),
    symbolProvider = KaFe10SymbolProvider(analysisSessionProvider, token),
    javaInteroperabilityComponent = KaFe10JavaInteroperabilityComponent(analysisSessionProvider, token),
    typeRelationChecker = KaFe10TypeRelationChecker(analysisSessionProvider, token),
    expressionInformationProvider = KaFe10ExpressionInformationProvider(analysisSessionProvider, token),
    evaluator = KaFe10Evaluator(analysisSessionProvider, token),
    referenceShortener = KaFe10ReferenceShortener(analysisSessionProvider, token),
    importOptimizer = KaFe10ImportOptimizer(analysisSessionProvider, token),
    renderer = KaRendererImpl(analysisSessionProvider, token),
    visibilityChecker = KaFe10VisibilityChecker(analysisSessionProvider, token),
    originalPsiProvider = KaFe10OriginalPsiProvider(analysisSessionProvider, token),
    typeCreator = KaFe10TypeCreator(analysisSessionProvider, token),
    analysisScopeProvider = KaAnalysisScopeProviderImpl(analysisSessionProvider, token, resolutionScope),
    signatureSubstitutor = KaFe10SignatureSubstitutor(analysisSessionProvider, token),
    resolveExtensionInfoProvider = KaFe10ResolveExtensionInfoProvider(analysisSessionProvider, token),
    compilerFacility = KaFe10CompilerFacility(analysisSessionProvider, token),
    metadataCalculator = KaFe10MetadataCalculator(analysisSessionProvider, token),
    substitutorProvider = KaFe10SubstitutorProvider(analysisSessionProvider, token),
    dataFlowProvider = KaFe10DataFlowProvider(analysisSessionProvider, token),
    sourceProvider = KaFe10SourceProvider(analysisSessionProvider, token)
) {
    override val typeProviderImpl: KaTypeProvider = KaFe10TypeProvider(this)
    override val symbolInfoProviderImpl: KaSymbolInfoProvider = KaFe10SymbolInfoProvider(this)
}
