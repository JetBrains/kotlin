/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProvider

@Suppress("DEPRECATION")
@KaImplementationDetail
abstract class KaBaseSession(
    final override val token: KaLifetimeToken,
    resolver: KaResolver,
    symbolRelationProvider: KaSymbolRelationProvider,
    diagnosticProvider: KaDiagnosticProvider,
    scopeProvider: KaScopeProvider,
    completionCandidateChecker: KaCompletionCandidateChecker,
    expressionTypeProvider: KaExpressionTypeProvider,
    typeProvider: KaTypeProvider,
    typeInformationProvider: KaTypeInformationProvider,
    symbolProvider: KaSymbolProvider,
    javaInteroperabilityComponent: KaJavaInteroperabilityComponent,
    symbolInformationProvider: KaSymbolInformationProvider,
    typeRelationChecker: KaTypeRelationChecker,
    expressionInformationProvider: KaExpressionInformationProvider,
    evaluator: KaEvaluator,
    referenceShortener: KaReferenceShortener,
    importOptimizer: KaImportOptimizer,
    renderer: KaRenderer,
    visibilityChecker: KaVisibilityChecker,
    originalPsiProvider: KaOriginalPsiProvider,
    typeCreator: KaTypeCreator,
    analysisScopeProvider: KaAnalysisScopeProvider,
    signatureSubstitutor: KaSignatureSubstitutor,
    resolveExtensionInfoProvider: KaResolveExtensionInfoProvider,
    compilerPluginGeneratedDeclarationsProvider: KaCompilerPluginGeneratedDeclarationsProvider,
    compilerFacility: KaCompilerFacility,
    metadataCalculator: KaMetadataCalculator,
    substitutorProvider: KaSubstitutorProvider,
    dataFlowProvider: KaDataFlowProvider,
    sourceProvider: KaSourceProvider,
) : KaSession,
    KaResolver by resolver,
    KaSymbolRelationProvider by symbolRelationProvider,
    KaDiagnosticProvider by diagnosticProvider,
    KaScopeProvider by scopeProvider,
    KaCompletionCandidateChecker by completionCandidateChecker,
    KaExpressionTypeProvider by expressionTypeProvider,
    KaTypeProvider by typeProvider,
    KaTypeInformationProvider by typeInformationProvider,
    KaSymbolProvider by symbolProvider,
    KaJavaInteroperabilityComponent by javaInteroperabilityComponent,
    KaSymbolInformationProvider by symbolInformationProvider,
    KaTypeRelationChecker by typeRelationChecker,
    KaExpressionInformationProvider by expressionInformationProvider,
    KaEvaluator by evaluator,
    KaReferenceShortener by referenceShortener,
    KaImportOptimizer by importOptimizer,
    KaRenderer by renderer,
    KaVisibilityChecker by visibilityChecker,
    KaOriginalPsiProvider by originalPsiProvider,
    KaTypeCreator by typeCreator,
    KaAnalysisScopeProvider by analysisScopeProvider,
    KaSignatureSubstitutor by signatureSubstitutor,
    KaResolveExtensionInfoProvider by resolveExtensionInfoProvider,
    KaCompilerPluginGeneratedDeclarationsProvider by compilerPluginGeneratedDeclarationsProvider,
    KaCompilerFacility by compilerFacility,
    KaMetadataCalculator by metadataCalculator,
    KaSubstitutorProvider by substitutorProvider,
    KaDataFlowProvider by dataFlowProvider,
    KaSourceProvider by sourceProvider