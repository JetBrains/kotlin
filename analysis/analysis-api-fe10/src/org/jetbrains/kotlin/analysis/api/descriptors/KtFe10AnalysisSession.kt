/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.descriptors.components.*
import org.jetbrains.kotlin.analysis.api.impl.base.KaBaseSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaRendererImpl
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaGlobalSearchScope
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.project.structure.KtModule

@OptIn(KaIdeApi::class, KaExperimentalApi::class, KaNonPublicApi::class)
class KaFe10Session(
    val analysisContext: Fe10AnalysisContext,
    override val useSiteModule: KtModule,
    token: KaLifetimeToken,
    analysisSessionProvider: () -> KaFe10Session,
    resolutionScope: KaGlobalSearchScope
) : KaBaseSession(
    token,
    resolver = KaFe10Resolver(analysisSessionProvider),
    symbolRelationProvider = KaFe10SymbolRelationProvider(analysisSessionProvider),
    diagnosticProvider = KaFe10DiagnosticProvider(analysisSessionProvider),
    scopeProvider = KaFe10ScopeProvider(analysisSessionProvider),
    completionCandidateChecker = KaFe10CompletionCandidateChecker(analysisSessionProvider),
    expressionTypeProvider = KaFe10ExpressionTypeProvider(analysisSessionProvider),
    typeProvider = KaFe10TypeProvider(analysisSessionProvider),
    typeInformationProvider = KaFe10TypeInformationProvider(analysisSessionProvider),
    symbolProvider = KaFe10SymbolProvider(analysisSessionProvider),
    javaInteroperabilityComponent = KaFe10JavaInteroperabilityComponent(analysisSessionProvider),
    symbolInformationProvider = KaFe10SymbolInformationProvider(analysisSessionProvider),
    typeRelationChecker = KaFe10TypeRelationChecker(analysisSessionProvider),
    expressionInformationProvider = KaFe10ExpressionInformationProvider(analysisSessionProvider),
    evaluator = KaFe10Evaluator(analysisSessionProvider),
    referenceShortener = KaFe10ReferenceShortener(analysisSessionProvider),
    importOptimizer = KaFe10ImportOptimizer(analysisSessionProvider),
    renderer = KaRendererImpl(analysisSessionProvider),
    visibilityChecker = KaFe10VisibilityChecker(analysisSessionProvider),
    originalPsiProvider = KaFe10OriginalPsiProvider(analysisSessionProvider),
    typeCreator = KaFe10TypeCreator(analysisSessionProvider),
    analysisScopeProvider = KaAnalysisScopeProviderImpl(analysisSessionProvider, resolutionScope),
    signatureSubstitutor = KaFe10SignatureSubstitutor(analysisSessionProvider),
    resolveExtensionInfoProvider = KaFe10ResolveExtensionInfoProvider(analysisSessionProvider),
    compilerFacility = KaFe10CompilerFacility(analysisSessionProvider),
    metadataCalculator = KaFe10MetadataCalculator(analysisSessionProvider),
    substitutorProvider = KaFe10SubstitutorProvider(analysisSessionProvider),
    dataFlowProvider = KaFe10DataFlowProvider(analysisSessionProvider),
    sourceProvider = KaFe10SourceProvider(analysisSessionProvider)
)
