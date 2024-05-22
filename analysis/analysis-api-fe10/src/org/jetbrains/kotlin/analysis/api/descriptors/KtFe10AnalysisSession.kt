/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.NotSupportedForK1Exception
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.components.*
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaRendererProviderImpl
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProviderByJavaPsi
import org.jetbrains.kotlin.analysis.project.structure.KtModule

@OptIn(KaAnalysisApiInternals::class, KaAnalysisNonPublicApi::class)
@Suppress("LeakingThis")
class KaFe10Session(
    val analysisContext: Fe10AnalysisContext,
    override val useSiteModule: KtModule,
    token: KaLifetimeToken,
) : KaSession(token) {

    override val smartCastProviderImpl: KaSmartCastProvider = KaFe10SmartCastProvider(this)
    override val diagnosticProviderImpl: KaDiagnosticProvider = KaFe10DiagnosticProvider(this)
    override val scopeProviderImpl: KaScopeProvider = KaFe10ScopeProvider(this)
    override val containingDeclarationProviderImpl: KaSymbolContainingDeclarationProvider = KaFe10SymbolContainingDeclarationProvider(this)
    override val symbolProviderImpl: KaSymbolProvider = KaFe10SymbolProvider(this)
    override val resolverImpl: KaResolver = KaFe10Resolver(this)
    override val completionCandidateCheckerImpl: KaCompletionCandidateChecker = KaFe10CompletionCandidateChecker(this)
    override val symbolDeclarationOverridesProviderImpl: KaSymbolDeclarationOverridesProvider =
        KaFe10SymbolDeclarationOverridesProvider(this)
    override val referenceShortenerImpl: KaReferenceShortener = KaFe10ReferenceShortener(this)
    override val symbolDeclarationRendererProviderImpl: KaSymbolDeclarationRendererProvider = KaRendererProviderImpl(this, token)
    override val expressionTypeProviderImpl: KaExpressionTypeProvider = KaFe10ExpressionTypeProvider(this)
    override val psiTypeProviderImpl: KaPsiTypeProvider = KaFe10PsiTypeProvider(this)
    override val typeProviderImpl: KaTypeProvider = KaFe10TypeProvider(this)
    override val typeInfoProviderImpl: KaTypeInfoProvider = KaFe10TypeInfoProvider(this)
    override val subtypingComponentImpl: KaSubtypingComponent = KaFe10SubtypingComponent(this)
    override val expressionInfoProviderImpl: KaExpressionInfoProvider = KaFe10ExpressionInfoProvider(this)
    override val compileTimeConstantProviderImpl: KaCompileTimeConstantProvider = KaFe10CompileTimeConstantProvider(this)
    override val visibilityCheckerImpl: KaVisibilityChecker = KaFe10VisibilityChecker(this)
    override val overrideInfoProviderImpl: KaOverrideInfoProvider = KaFe10OverrideInfoProvider(this)
    override val multiplatformInfoProviderImpl: KaMultiplatformInfoProvider = KaFe10MultiplatformInfoProvider(this)
    override val originalPsiProviderImpl: KaOriginalPsiProvider = KaFe10OriginalPsiProvider(this)
    override val inheritorsProviderImpl: KaInheritorsProvider = KaFe10InheritorsProvider(this)
    override val typesCreatorImpl: KaTypeCreator = KaFe10TypeCreator(this)
    override val samResolverImpl: KaSamResolver = KaFe10SamResolver(this)
    override val importOptimizerImpl: KaImportOptimizer = KaFe10ImportOptimizer(this)
    override val jvmTypeMapperImpl: KaJvmTypeMapper = KaFe10JvmTypeMapper(this)
    override val symbolInfoProviderImpl: KaSymbolInfoProvider = KaFe10SymbolInfoProvider(this)
    override val analysisScopeProviderImpl: KaAnalysisScopeProvider =
        KaAnalysisScopeProviderImpl(this, token, shadowedScope = GlobalSearchScope.EMPTY_SCOPE)
    override val referenceResolveProviderImpl: KaReferenceResolveProvider = KaFe10ReferenceResolveProvider(this)
    override val signatureSubstitutorImpl: KaSignatureSubstitutor = KaFe10SignatureSubstitutor(this)
    override val scopeSubstitutionImpl: KaScopeSubstitution = KaFe10ScopeSubstitution(this)
    override val substitutorFactoryImpl: KaSubstitutorFactory = KaFe10SubstitutorFactory(this)
    override val symbolProviderByJavaPsiImpl: KaSymbolProviderByJavaPsi = KaFe10SymbolProviderByJavaPsi(this)
    override val resolveExtensionInfoProviderImpl: KaResolveExtensionInfoProvider = KaFe10ResolveExtensionInfoProvider(this)
    override val compilerFacilityImpl: KaCompilerFacility = KaFe10CompilerFacility(this)
    override val dataFlowInfoProviderImpl: KaDataFlowInfoProvider = KaFe10DataFlowInfoProvider(this)
    override val klibSourceFileProviderImpl: KaKlibSourceFileNameProvider = KaFe10KlibSourceFileNameProvider(this)

    override val metadataCalculatorImpl: KaMetadataCalculator
        get() = throw NotSupportedForK1Exception()

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override val substitutorProviderImpl: KaSubstitutorProvider
        get() = throw NotSupportedForK1Exception()
}
