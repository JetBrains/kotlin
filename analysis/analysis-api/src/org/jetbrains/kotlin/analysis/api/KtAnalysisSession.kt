/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider

/**
 * [KaSession] is the entry point to all frontend-related work. It has the following contracts:
 *
 * - It should not be accessed from the event dispatch thread or outside a read action.
 * - It should not be leaked outside the read action it was created in. To ensure that an analysis session isn't leaked, there are
 *   additional conventions, explained further below.
 * - All entities retrieved from an analysis session should not be leaked outside the read action the analysis session was created in.
 *
 * To pass a symbol from one read action to another, use [KaSymbolPointer], which can be created from a symbol by [KaSymbol.createPointer].
 *
 * To create a [KaSession], please use [analyze] or one of its siblings.
 *
 * ### Conventions to avoid leakage
 *
 * It is crucial to avoid leaking the analysis session outside the read action it was created in, as the analysis session itself and all
 * entities retrieved from it will become invalid. An analysis session also shouldn't be leaked from the [analyze] call it was created in.
 *
 * It is forbidden to store an analysis session in a variable, parameter, or property. From the [analyze] block which provides the analysis
 * session, the analysis session should be passed to functions via an extension receiver, or as an ordinary parameter. For example:
 *
 * ```kotlin
 * fun KaSession.foo() { ... }
 * ```
 *
 * **Class context receivers** should not be used to pass analysis sessions. While a context receiver on a class will make the analysis
 * session available in the constructor, it will also be captured by the class as a property. This behavior is easy to miss and a high risk
 * for unintentional leakage. For example:
 *
 * ```kotlin
 * // DO NOT DO THIS
 * context(KaSession)
 * class Usage {
 *     fun foo() {
 *         // The `KaSession` is available here.
 *     }
 * }
 * ```
 */
@OptIn(KaAnalysisApiInternals::class, KaAnalysisNonPublicApi::class)
@Suppress("AnalysisApiMissingLifetimeCheck")
public abstract class KaSession(
    final override val token: KaLifetimeToken,
    resolver: KaResolver,
    symbolRelationProvider: KaSymbolRelationProvider,
    diagnosticProvider: KaDiagnosticProvider,
    scopeProvider: KaScopeProvider,
    completionCandidateChecker: KaCompletionCandidateChecker,
    expressionTypeProvider: KaExpressionTypeProvider,
    symbolProvider: KaSymbolProvider,
    javaInteroperabilityComponent: KaJavaInteroperabilityComponent,
    typeRelationChecker: KaTypeRelationChecker,
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
    compilerFacility: KaCompilerFacility,
    metadataCalculator: KaMetadataCalculator,
    substitutorProvider: KaSubstitutorProvider,
    dataFlowProvider: KaDataFlowProvider,
    sourceProvider: KaSourceProvider,
) : KaLifetimeOwner,
    KaResolver by resolver,
    KaSymbolRelationProvider by symbolRelationProvider,
    KaDiagnosticProvider by diagnosticProvider,
    KaScopeProvider by scopeProvider,
    KaCompletionCandidateChecker by completionCandidateChecker,
    KaExpressionTypeProvider by expressionTypeProvider,
    KaTypeProviderMixIn,
    KaTypeInfoProviderMixIn,
    KaSymbolProvider by symbolProvider,
    KaJavaInteroperabilityComponent by javaInteroperabilityComponent,
    KaSymbolInfoProviderMixIn,
    KaTypeRelationChecker by typeRelationChecker,
    KaExpressionInfoProviderMixIn,
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
    KaCompilerFacility by compilerFacility,
    KaMetadataCalculator by metadataCalculator,
    KaSubstitutorProvider by substitutorProvider,
    KaDataFlowProvider by dataFlowProvider,
    KaSourceProvider by sourceProvider
{

    public abstract val useSiteModule: KtModule

    override val analysisSession: KaSession get() = this

    internal val typeProvider: KaTypeProvider get() = typeProviderImpl
    protected abstract val typeProviderImpl: KaTypeProvider

    internal val typeInfoProvider: KaTypeInfoProvider get() = typeInfoProviderImpl
    protected abstract val typeInfoProviderImpl: KaTypeInfoProvider

    internal val expressionInfoProvider: KaExpressionInfoProvider get() = expressionInfoProviderImpl
    protected abstract val expressionInfoProviderImpl: KaExpressionInfoProvider

    internal val symbolInfoProvider: KaSymbolInfoProvider get() = symbolInfoProviderImpl
    protected abstract val symbolInfoProviderImpl: KaSymbolInfoProvider

    public fun <S : KaSymbol> KaSymbolPointer<S>.restoreSymbol(): S? = withValidityAssertion {
        @OptIn(KaImplementationDetail::class)
        restoreSymbol(analysisSession)
    }
}

public typealias KtAnalysisSession = KaSession

public fun KaSession.getModule(element: PsiElement): KtModule {
    return ProjectStructureProvider.getModule(useSiteModule.project, element, useSiteModule)
}
