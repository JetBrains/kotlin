/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * [KtAnalysisSession] is the entry point into all frontend-related work. It has the following contracts:
 *
 * - It should not be accessed from event dispatch thread.
 * - It should not be accessed outside a read action.
 * - It should not be leaked outside the read action it was created in.
 * - To be sure that an analysis session is not leaked, it is forbidden to store it in a variable. Consider working with it only inside
 *   [analyze] blocks, and pass it to functions via context receivers (e.g. `context(KtAnalysisSession)`).
 * - All entities retrieved from an analysis session should not be leaked outside the read action the analysis session was created in.
 *
 * To pass a symbol from one read action to another use [KtSymbolPointer], which can be created from a symbol by [KtSymbol.createPointer].
 *
 * To create a [KtAnalysisSession], please use [analyze] or one of its siblings.
 */
@OptIn(KtAnalysisApiInternals::class, KtAnalysisNonPublicApi::class)
@Suppress("AnalysisApiMissingLifetimeCheck")
public abstract class KtAnalysisSession(final override val token: KtLifetimeToken) : KtLifetimeOwner,
    KtSmartCastProviderMixIn,
    KtCallResolverMixIn,
    KtSamResolverMixIn,
    KtDiagnosticProviderMixIn,
    KtScopeProviderMixIn,
    KtCompletionCandidateCheckerMixIn,
    KtSymbolDeclarationOverridesProviderMixIn,
    KtExpressionTypeProviderMixIn,
    KtPsiTypeProviderMixIn,
    KtJvmTypeMapperMixIn,
    KtTypeProviderMixIn,
    KtTypeInfoProviderMixIn,
    KtSymbolProviderMixIn,
    KtSymbolContainingDeclarationProviderMixIn,
    KtSymbolInfoProviderMixIn,
    KtSubtypingComponentMixIn,
    KtExpressionInfoProviderMixIn,
    KtCompileTimeConstantProviderMixIn,
    KtSymbolsMixIn,
    KtReferenceResolveMixIn,
    KtReferenceShortenerMixIn,
    KtImportOptimizerMixIn,
    KtSymbolDeclarationRendererMixIn,
    KtVisibilityCheckerMixIn,
    KtMemberSymbolProviderMixin,
    KtMultiplatformInfoProviderMixin,
    KtOriginalPsiProviderMixIn,
    KtInheritorsProviderMixIn,
    KtTypeCreatorMixIn,
    KtAnalysisScopeProviderMixIn,
    KtSignatureSubstitutorMixIn,
    KtScopeSubstitutionMixIn,
    KtSymbolProviderByJavaPsiMixIn,
    KtResolveExtensionInfoProviderMixIn,
    KtCompilerFacilityMixIn,
    KtMetadataCalculatorMixIn {

    public abstract val useSiteModule: KtModule

    override val analysisSession: KtAnalysisSession get() = this

    /**
     * Creates a new [KtAnalysisSession] which depends on this analysis session, but additionally provides its own symbols derived from
     * analyzing [elementToReanalyze].
     *
     * @see analyzeInDependedAnalysisSession
     */
    public abstract fun createContextDependentCopy(originalKtFile: KtFile, elementToReanalyze: KtElement): KtAnalysisSession

    internal val smartCastProvider: KtSmartCastProvider get() = smartCastProviderImpl
    protected abstract val smartCastProviderImpl: KtSmartCastProvider

    internal val diagnosticProvider: KtDiagnosticProvider get() = diagnosticProviderImpl
    protected abstract val diagnosticProviderImpl: KtDiagnosticProvider

    internal val scopeProvider: KtScopeProvider get() = scopeProviderImpl
    protected abstract val scopeProviderImpl: KtScopeProvider

    internal val containingDeclarationProvider: KtSymbolContainingDeclarationProvider get() = containingDeclarationProviderImpl
    protected abstract val containingDeclarationProviderImpl: KtSymbolContainingDeclarationProvider

    internal val symbolProvider: KtSymbolProvider get() = symbolProviderImpl
    protected abstract val symbolProviderImpl: KtSymbolProvider

    internal val callResolver: KtCallResolver get() = callResolverImpl
    protected abstract val callResolverImpl: KtCallResolver

    internal val samResolver: KtSamResolver get() = samResolverImpl
    protected abstract val samResolverImpl: KtSamResolver

    internal val completionCandidateChecker: KtCompletionCandidateChecker get() = completionCandidateCheckerImpl
    protected abstract val completionCandidateCheckerImpl: KtCompletionCandidateChecker

    internal val symbolDeclarationOverridesProvider: KtSymbolDeclarationOverridesProvider get() = symbolDeclarationOverridesProviderImpl
    protected abstract val symbolDeclarationOverridesProviderImpl: KtSymbolDeclarationOverridesProvider

    internal val referenceShortener: KtReferenceShortener get() = referenceShortenerImpl
    protected abstract val referenceShortenerImpl: KtReferenceShortener

    internal val importOptimizer: KtImportOptimizer get() = importOptimizerImpl
    protected abstract val importOptimizerImpl: KtImportOptimizer

    internal val symbolDeclarationRendererProvider: KtSymbolDeclarationRendererProvider get() = symbolDeclarationRendererProviderImpl
    protected abstract val symbolDeclarationRendererProviderImpl: KtSymbolDeclarationRendererProvider

    internal val expressionTypeProvider: KtExpressionTypeProvider get() = expressionTypeProviderImpl
    protected abstract val expressionTypeProviderImpl: KtExpressionTypeProvider

    internal val psiTypeProvider: KtPsiTypeProvider get() = psiTypeProviderImpl
    protected abstract val psiTypeProviderImpl: KtPsiTypeProvider

    internal val jvmTypeMapper: KtJvmTypeMapper get() = jvmTypeMapperImpl
    protected abstract val jvmTypeMapperImpl: KtJvmTypeMapper

    internal val typeProvider: KtTypeProvider get() = typeProviderImpl
    protected abstract val typeProviderImpl: KtTypeProvider

    internal val typeInfoProvider: KtTypeInfoProvider get() = typeInfoProviderImpl
    protected abstract val typeInfoProviderImpl: KtTypeInfoProvider

    internal val subtypingComponent: KtSubtypingComponent get() = subtypingComponentImpl
    protected abstract val subtypingComponentImpl: KtSubtypingComponent

    internal val expressionInfoProvider: KtExpressionInfoProvider get() = expressionInfoProviderImpl
    protected abstract val expressionInfoProviderImpl: KtExpressionInfoProvider

    internal val compileTimeConstantProvider: KtCompileTimeConstantProvider get() = compileTimeConstantProviderImpl
    protected abstract val compileTimeConstantProviderImpl: KtCompileTimeConstantProvider

    internal val visibilityChecker: KtVisibilityChecker get() = visibilityCheckerImpl
    protected abstract val visibilityCheckerImpl: KtVisibilityChecker

    internal val overrideInfoProvider: KtOverrideInfoProvider get() = overrideInfoProviderImpl
    protected abstract val overrideInfoProviderImpl: KtOverrideInfoProvider

    internal val inheritorsProvider: KtInheritorsProvider get() = inheritorsProviderImpl
    protected abstract val inheritorsProviderImpl: KtInheritorsProvider

    internal val multiplatformInfoProvider: KtMultiplatformInfoProvider get() = multiplatformInfoProviderImpl
    protected abstract val multiplatformInfoProviderImpl: KtMultiplatformInfoProvider

    internal val originalPsiProvider: KtOriginalPsiProvider get() = originalPsiProviderImpl
    protected abstract val originalPsiProviderImpl: KtOriginalPsiProvider

    internal val symbolInfoProvider: KtSymbolInfoProvider get() = symbolInfoProviderImpl
    protected abstract val symbolInfoProviderImpl: KtSymbolInfoProvider

    internal val analysisScopeProvider: KtAnalysisScopeProvider get() = analysisScopeProviderImpl
    protected abstract val analysisScopeProviderImpl: KtAnalysisScopeProvider

    internal val referenceResolveProvider: KtReferenceResolveProvider get() = referenceResolveProviderImpl
    protected abstract val referenceResolveProviderImpl: KtReferenceResolveProvider

    internal val signatureSubstitutor: KtSignatureSubstitutor get() = signatureSubstitutorImpl
    protected abstract val signatureSubstitutorImpl: KtSignatureSubstitutor

    internal val scopeSubstitution: KtScopeSubstitution get() = scopeSubstitutionImpl
    protected abstract val scopeSubstitutionImpl: KtScopeSubstitution

    internal val resolveExtensionInfoProvider: KtResolveExtensionInfoProvider get() = resolveExtensionInfoProviderImpl
    protected abstract val resolveExtensionInfoProviderImpl: KtResolveExtensionInfoProvider

    internal val compilerFacility: KtCompilerFacility get() = compilerFacilityImpl
    protected abstract val compilerFacilityImpl: KtCompilerFacility

    @KtAnalysisApiInternals
    public val substitutorFactory: KtSubstitutorFactory get() = substitutorFactoryImpl
    protected abstract val substitutorFactoryImpl: KtSubstitutorFactory

    @KtAnalysisApiInternals
    public val symbolProviderByJavaPsi: KtSymbolProviderByJavaPsi get() = symbolProviderByJavaPsiImpl
    @KtAnalysisApiInternals
    protected abstract val symbolProviderByJavaPsiImpl: KtSymbolProviderByJavaPsi

    internal val metadataCalculator: KtMetadataCalculator get() = metadataCalculatorImpl
    protected abstract val metadataCalculatorImpl: KtMetadataCalculator

    @PublishedApi
    internal val typesCreator: KtTypeCreator
        get() = typesCreatorImpl
    protected abstract val typesCreatorImpl: KtTypeCreator
}

public fun KtAnalysisSession.getModule(element: PsiElement): KtModule {
    return ProjectStructureProvider.getModule(useSiteModule.project, element, useSiteModule)
}
