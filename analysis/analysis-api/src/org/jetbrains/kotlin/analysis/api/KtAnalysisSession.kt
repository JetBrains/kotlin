/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
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
public abstract class KaSession(final override val token: KaLifetimeToken) : KaLifetimeOwner,
    KaSmartCastProviderMixIn,
    KaResolverMixIn,
    KaSamResolverMixIn,
    KaDiagnosticProviderMixIn,
    KaScopeProviderMixIn,
    KaCompletionCandidateCheckerMixIn,
    KaSymbolDeclarationOverridesProviderMixIn,
    KaExpressionTypeProviderMixIn,
    KaPsiTypeProviderMixIn,
    KaJvmTypeMapperMixIn,
    KaTypeProviderMixIn,
    KaTypeInfoProviderMixIn,
    KaSymbolProviderMixIn,
    KaSymbolContainingDeclarationProviderMixIn,
    KaSymbolInfoProviderMixIn,
    KaSubtypingComponentMixIn,
    KaExpressionInfoProviderMixIn,
    KaCompileTimeConstantProviderMixIn,
    KaSymbolsMixIn,
    KaReferenceResolveMixIn,
    KaReferenceShortenerMixIn,
    KaImportOptimizerMixIn,
    KaSymbolDeclarationRendererMixIn,
    KaVisibilityCheckerMixIn,
    KaMemberSymbolProviderMixin,
    KaMultiplatformInfoProviderMixin,
    KaOriginalPsiProviderMixIn,
    KaInheritorsProviderMixIn,
    KaTypeCreatorMixIn,
    KaAnalysisScopeProviderMixIn,
    KaSignatureSubstitutorMixIn,
    KaScopeSubstitutionMixIn,
    KaSymbolProviderByJavaPsiMixIn,
    KaResolveExtensionInfoProviderMixIn,
    KaCompilerFacilityMixIn,
    KaMetadataCalculatorMixIn,
    KaSubstitutorProviderMixIn,
    KaDataFlowInfoProviderMixin,
    KaKlibSourceFileProviderMixIn {

    public abstract val useSiteModule: KtModule

    override val analysisSession: KaSession get() = this

    internal val smartCastProvider: KaSmartCastProvider get() = smartCastProviderImpl
    protected abstract val smartCastProviderImpl: KaSmartCastProvider

    internal val diagnosticProvider: KaDiagnosticProvider get() = diagnosticProviderImpl
    protected abstract val diagnosticProviderImpl: KaDiagnosticProvider

    internal val scopeProvider: KaScopeProvider get() = scopeProviderImpl
    protected abstract val scopeProviderImpl: KaScopeProvider

    internal val containingDeclarationProvider: KaSymbolContainingDeclarationProvider get() = containingDeclarationProviderImpl
    protected abstract val containingDeclarationProviderImpl: KaSymbolContainingDeclarationProvider

    internal val symbolProvider: KaSymbolProvider get() = symbolProviderImpl
    protected abstract val symbolProviderImpl: KaSymbolProvider

    @KtAnalysisNonPublicApi
    internal val resolver: KaResolver get() = resolverImpl

    @KtAnalysisNonPublicApi
    protected abstract val resolverImpl: KaResolver

    internal val samResolver: KaSamResolver get() = samResolverImpl
    protected abstract val samResolverImpl: KaSamResolver

    internal val completionCandidateChecker: KaCompletionCandidateChecker get() = completionCandidateCheckerImpl
    protected abstract val completionCandidateCheckerImpl: KaCompletionCandidateChecker

    internal val symbolDeclarationOverridesProvider: KaSymbolDeclarationOverridesProvider get() = symbolDeclarationOverridesProviderImpl
    protected abstract val symbolDeclarationOverridesProviderImpl: KaSymbolDeclarationOverridesProvider

    internal val referenceShortener: KaReferenceShortener get() = referenceShortenerImpl
    protected abstract val referenceShortenerImpl: KaReferenceShortener

    internal val importOptimizer: KaImportOptimizer get() = importOptimizerImpl
    protected abstract val importOptimizerImpl: KaImportOptimizer

    internal val symbolDeclarationRendererProvider: KaSymbolDeclarationRendererProvider get() = symbolDeclarationRendererProviderImpl
    protected abstract val symbolDeclarationRendererProviderImpl: KaSymbolDeclarationRendererProvider

    internal val expressionTypeProvider: KaExpressionTypeProvider get() = expressionTypeProviderImpl
    protected abstract val expressionTypeProviderImpl: KaExpressionTypeProvider

    internal val psiTypeProvider: KaPsiTypeProvider get() = psiTypeProviderImpl
    protected abstract val psiTypeProviderImpl: KaPsiTypeProvider

    internal val jvmTypeMapper: KaJvmTypeMapper get() = jvmTypeMapperImpl
    protected abstract val jvmTypeMapperImpl: KaJvmTypeMapper

    internal val typeProvider: KaTypeProvider get() = typeProviderImpl
    protected abstract val typeProviderImpl: KaTypeProvider

    internal val typeInfoProvider: KaTypeInfoProvider get() = typeInfoProviderImpl
    protected abstract val typeInfoProviderImpl: KaTypeInfoProvider

    internal val subtypingComponent: KaSubtypingComponent get() = subtypingComponentImpl
    protected abstract val subtypingComponentImpl: KaSubtypingComponent

    internal val expressionInfoProvider: KaExpressionInfoProvider get() = expressionInfoProviderImpl
    protected abstract val expressionInfoProviderImpl: KaExpressionInfoProvider

    internal val compileTimeConstantProvider: KaCompileTimeConstantProvider get() = compileTimeConstantProviderImpl
    protected abstract val compileTimeConstantProviderImpl: KaCompileTimeConstantProvider

    internal val visibilityChecker: KaVisibilityChecker get() = visibilityCheckerImpl
    protected abstract val visibilityCheckerImpl: KaVisibilityChecker

    internal val overrideInfoProvider: KaOverrideInfoProvider get() = overrideInfoProviderImpl
    protected abstract val overrideInfoProviderImpl: KaOverrideInfoProvider

    internal val inheritorsProvider: KaInheritorsProvider get() = inheritorsProviderImpl
    protected abstract val inheritorsProviderImpl: KaInheritorsProvider

    internal val multiplatformInfoProvider: KaMultiplatformInfoProvider get() = multiplatformInfoProviderImpl
    protected abstract val multiplatformInfoProviderImpl: KaMultiplatformInfoProvider

    internal val originalPsiProvider: KaOriginalPsiProvider get() = originalPsiProviderImpl
    protected abstract val originalPsiProviderImpl: KaOriginalPsiProvider

    internal val symbolInfoProvider: KaSymbolInfoProvider get() = symbolInfoProviderImpl
    protected abstract val symbolInfoProviderImpl: KaSymbolInfoProvider

    internal val analysisScopeProvider: KaAnalysisScopeProvider get() = analysisScopeProviderImpl
    protected abstract val analysisScopeProviderImpl: KaAnalysisScopeProvider

    internal val referenceResolveProvider: KaReferenceResolveProvider get() = referenceResolveProviderImpl
    protected abstract val referenceResolveProviderImpl: KaReferenceResolveProvider

    internal val signatureSubstitutor: KaSignatureSubstitutor get() = signatureSubstitutorImpl
    protected abstract val signatureSubstitutorImpl: KaSignatureSubstitutor

    internal val scopeSubstitution: KaScopeSubstitution get() = scopeSubstitutionImpl
    protected abstract val scopeSubstitutionImpl: KaScopeSubstitution

    internal val resolveExtensionInfoProvider: KaResolveExtensionInfoProvider get() = resolveExtensionInfoProviderImpl
    protected abstract val resolveExtensionInfoProviderImpl: KaResolveExtensionInfoProvider

    internal val compilerFacility: KaCompilerFacility get() = compilerFacilityImpl
    protected abstract val compilerFacilityImpl: KaCompilerFacility

    @KaAnalysisApiInternals
    public val substitutorFactory: KaSubstitutorFactory get() = substitutorFactoryImpl
    protected abstract val substitutorFactoryImpl: KaSubstitutorFactory

    @KaAnalysisApiInternals
    public val symbolProviderByJavaPsi: KaSymbolProviderByJavaPsi get() = symbolProviderByJavaPsiImpl
    @KaAnalysisApiInternals
    protected abstract val symbolProviderByJavaPsiImpl: KaSymbolProviderByJavaPsi

    internal val metadataCalculator: KaMetadataCalculator get() = metadataCalculatorImpl
    protected abstract val metadataCalculatorImpl: KaMetadataCalculator

    @PublishedApi
    internal val typesCreator: KaTypeCreator
        get() = typesCreatorImpl
    protected abstract val typesCreatorImpl: KaTypeCreator

    internal val substitutorProvider: KaSubstitutorProvider get() = substitutorProviderImpl
    protected abstract val substitutorProviderImpl: KaSubstitutorProvider

    @KaAnalysisNonPublicApi
    internal val dataFlowInfoProvider: KaDataFlowInfoProvider get() = dataFlowInfoProviderImpl
    @KaAnalysisNonPublicApi
    protected abstract val dataFlowInfoProviderImpl: KaDataFlowInfoProvider

    internal val klibSourceFileProvider: KaKlibSourceFileNameProvider get() = klibSourceFileProviderImpl
    protected abstract val klibSourceFileProviderImpl: KaKlibSourceFileNameProvider
}

public typealias KtAnalysisSession = KaSession

public fun KaSession.getModule(element: PsiElement): KtModule {
    return ProjectStructureProvider.getModule(useSiteModule.project, element, useSiteModule)
}
