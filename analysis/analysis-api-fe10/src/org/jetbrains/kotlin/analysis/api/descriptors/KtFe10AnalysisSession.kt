/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.NotSupportedForK1Exception
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.components.*
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAnalysisScopeProviderImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaRendererProviderImpl
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaGlobalSearchScope
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
    analysisSessionProvider: () -> KaFe10Session,
    resolutionScope: KaGlobalSearchScope
) : KaSession(
    token,
    resolver = KaFe10Resolver(analysisSessionProvider, token),
    symbolRelationProvider = KaFe10SymbolRelationProvider(analysisSessionProvider, token),
    diagnosticProvider = KaFe10DiagnosticProvider(analysisSessionProvider, token),
    scopeProvider = KaFe10ScopeProvider(analysisSessionProvider, token),
    completionCandidateChecker = KaFe10CompletionCandidateChecker(analysisSessionProvider, token),
    evaluator = KaFe10Evaluator(analysisSessionProvider, token),
    referenceShortener = KaFe10ReferenceShortener(analysisSessionProvider, token),
    importOptimizer = KaFe10ImportOptimizer(analysisSessionProvider, token),
    originalPsiProvider = KaFe10OriginalPsiProvider(analysisSessionProvider, token),
    typeCreator = KaFe10TypeCreator(analysisSessionProvider, token),
    analysisScopeProvider = KaAnalysisScopeProviderImpl(analysisSessionProvider, token, resolutionScope),
    compilerFacility = KaFe10CompilerFacility(analysisSessionProvider, token),
    metadataCalculator = KaFe10MetadataCalculator(analysisSessionProvider, token),
    dataFlowProvider = KaFe10DataFlowProvider(analysisSessionProvider, token),
    sourceProvider = KaFe10SourceProvider(analysisSessionProvider, token)
) {
    override val containingDeclarationProviderImpl: KaSymbolContainingDeclarationProvider = KaFe10SymbolContainingDeclarationProvider(this)
    override val symbolProviderImpl: KaSymbolProvider = KaFe10SymbolProvider(this)
    override val symbolDeclarationOverridesProviderImpl: KaSymbolDeclarationOverridesProvider =
        KaFe10SymbolDeclarationOverridesProvider(this)
    override val symbolDeclarationRendererProviderImpl: KaSymbolDeclarationRendererProvider = KaRendererProviderImpl(this, token)
    override val expressionTypeProviderImpl: KaExpressionTypeProvider = KaFe10ExpressionTypeProvider(this)
    override val psiTypeProviderImpl: KaPsiTypeProvider = KaFe10PsiTypeProvider(this)
    override val typeProviderImpl: KaTypeProvider = KaFe10TypeProvider(this)
    override val typeInfoProviderImpl: KaTypeInfoProvider = KaFe10TypeInfoProvider(this)
    override val subtypingComponentImpl: KaSubtypingComponent = KaFe10SubtypingComponent(this)
    override val expressionInfoProviderImpl: KaExpressionInfoProvider = KaFe10ExpressionInfoProvider(this)
    override val visibilityCheckerImpl: KaVisibilityChecker = KaFe10VisibilityChecker(this)
    override val overrideInfoProviderImpl: KaOverrideInfoProvider = KaFe10OverrideInfoProvider(this)
    override val multiplatformInfoProviderImpl: KaMultiplatformInfoProvider = KaFe10MultiplatformInfoProvider(this)
    override val inheritorsProviderImpl: KaInheritorsProvider = KaFe10InheritorsProvider(this)
    override val samResolverImpl: KaSamResolver = KaFe10SamResolver(this)
    override val jvmTypeMapperImpl: KaJvmTypeMapper = KaFe10JvmTypeMapper(this)
    override val symbolInfoProviderImpl: KaSymbolInfoProvider = KaFe10SymbolInfoProvider(this)
    override val signatureSubstitutorImpl: KaSignatureSubstitutor = KaFe10SignatureSubstitutor(this)
    override val substitutorFactoryImpl: KaSubstitutorFactory = KaFe10SubstitutorFactory(this)
    override val symbolProviderByJavaPsiImpl: KaSymbolProviderByJavaPsi = KaFe10SymbolProviderByJavaPsi(this)
    override val resolveExtensionInfoProviderImpl: KaResolveExtensionInfoProvider = KaFe10ResolveExtensionInfoProvider(this)

    @Suppress("AnalysisApiMissingLifetimeCheck")
    override val substitutorProviderImpl: KaSubstitutorProvider
        get() = throw NotSupportedForK1Exception()
}
