/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.components.*
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolProvider
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@Suppress("LeakingThis")
class KtFe10AnalysisSession(val analysisContext: Fe10AnalysisContext) : KtAnalysisSession(analysisContext.token) {
    constructor(contextElement: KtElement, token: ValidityToken) :
            this(Fe10AnalysisContext(Fe10AnalysisFacade.getInstance(contextElement.project), contextElement, token))

    override val smartCastProviderImpl: KtSmartCastProvider = KtFe10SmartCastProvider(this)
    override val diagnosticProviderImpl: KtDiagnosticProvider = KtFe10DiagnosticProvider(this)
    override val scopeProviderImpl: KtScopeProvider = KtFe10ScopeProvider(this)
    override val containingDeclarationProviderImpl: KtSymbolContainingDeclarationProvider = KtFe10SymbolContainingDeclarationProvider(this)
    override val symbolProviderImpl: KtSymbolProvider = KtFe10SymbolProvider(this)
    override val callResolverImpl: KtCallResolver = KtFe10CallResolver(this)
    override val completionCandidateCheckerImpl: KtCompletionCandidateChecker = KtFe10CompletionCandidateChecker(this)
    override val symbolDeclarationOverridesProviderImpl: KtSymbolDeclarationOverridesProvider = KtFe10SymbolDeclarationOverridesProvider(this)
    override val referenceShortenerImpl: KtReferenceShortener = KtFe10ReferenceShortener(this)
    override val symbolDeclarationRendererProviderImpl: KtSymbolDeclarationRendererProvider = KtFe10SymbolDeclarationRendererProvider(this)
    override val expressionTypeProviderImpl: KtExpressionTypeProvider = KtFe10ExpressionTypeProvider(this)
    override val psiTypeProviderImpl: KtPsiTypeProvider = KtFe10PsiTypeProvider(this)
    override val typeProviderImpl: KtTypeProvider = KtFe10TypeProvider(this)
    override val typeInfoProviderImpl: KtTypeInfoProvider = KtFe10TypeInfoProvider(this)
    override val subtypingComponentImpl: KtSubtypingComponent = KtFe10SubtypingComponent(this)
    override val expressionInfoProviderImpl: KtExpressionInfoProvider = KtFe10ExpressionInfoProvider(this)
    override val compileTimeConstantProviderImpl: KtCompileTimeConstantProvider = KtFe10CompileTimeConstantProvider(this)
    override val visibilityCheckerImpl: KtVisibilityChecker = KtFe10VisibilityChecker(this)
    override val overrideInfoProviderImpl: KtOverrideInfoProvider = KtFe10OverrideInfoProvider(this)
    override val inheritorsProviderImpl: KtInheritorsProvider = KtFe10InheritorsProvider(this)
    override val typesCreatorImpl: KtTypeCreator = KtFe10TypeCreator(this)
    override val samResolverImpl: KtSamResolver = KtFe10SamResolver(this)
    override val importOptimizerImpl: KtImportOptimizer = KtFe10ImportOptimizer(this)
    override val jvmTypeMapperImpl: KtJvmTypeMapper = KtFe10JvmTypeMapper(this)
    override val symbolInfoProviderImpl: KtSymbolInfoProvider = KtFe10SymbolInfoProvider(this)

    override fun createContextDependentCopy(originalKtFile: KtFile, elementToReanalyze: KtElement): KtAnalysisSession {
        return KtFe10AnalysisSession(elementToReanalyze, token)
    }
}
