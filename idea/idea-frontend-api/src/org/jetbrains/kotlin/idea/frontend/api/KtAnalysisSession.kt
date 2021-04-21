/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.idea.frontend.api.components.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.psi.*

/**
 * The entry point into all frontend-related work. Has the following contracts:
 * - Should not be accessed from event dispatch thread
 * - Should not be accessed outside read action
 * - Should not be leaked outside read action it was created in
 * - To be sure that session is not leaked it is forbidden to store it in a variable, consider working with it only in [analyse] context
 * - All entities retrieved from analysis session should not be leaked outside the read action KtAnalysisSession was created in
 *
 * To pass a symbol from one read action to another use [KtSymbolPointer] which can be created from a symbol by [KtSymbol.createPointer]
 *
 * To create analysis session consider using [analyse]
 */
abstract class KtAnalysisSession(final override val token: ValidityToken) : ValidityTokenOwner,
    KtSmartCastProviderMixIn,
    KtCallResolverMixIn,
    KtDiagnosticProviderMixIn,
    KtScopeProviderMixIn,
    KtCompletionCandidateCheckerMixIn,
    KtSymbolDeclarationOverridesProviderMixIn,
    KtExpressionTypeProviderMixIn,
    KtTypeProviderMixIn,
    KtTypeInfoProviderMixIn,
    KtSymbolProviderMixIn,
    KtSymbolContainingDeclarationProviderMixIn,
    KtSubtypingComponentMixIn,
    KtExpressionInfoProviderMixIn,
    KtSymbolsMixIn,
    KtReferenceResolveMixIn,
    KtReferenceShortenerMixIn,
    KtSymbolDeclarationRendererMixIn,
    KtVisibilityCheckerMixIn,
    KtMemberSymbolProviderMixin
{

    override val analysisSession: KtAnalysisSession get() = this

    abstract fun createContextDependentCopy(originalKtFile: KtFile, dependencyKtElement: KtElement): KtAnalysisSession

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

    internal val completionCandidateChecker: KtCompletionCandidateChecker get() = completionCandidateCheckerImpl
    protected abstract val completionCandidateCheckerImpl: KtCompletionCandidateChecker

    internal val symbolDeclarationOverridesProvider: KtSymbolDeclarationOverridesProvider get() = symbolDeclarationOverridesProviderImpl
    protected abstract val symbolDeclarationOverridesProviderImpl: KtSymbolDeclarationOverridesProvider

    internal val referenceShortener: KtReferenceShortener get() = referenceShortenerImpl
    protected abstract val referenceShortenerImpl: KtReferenceShortener

    internal val symbolDeclarationRendererProvider: KtSymbolDeclarationRendererProvider get() = symbolDeclarationRendererProviderImpl
    protected abstract val symbolDeclarationRendererProviderImpl: KtSymbolDeclarationRendererProvider

    internal val expressionTypeProvider: KtExpressionTypeProvider get() = expressionTypeProviderImpl
    protected abstract val expressionTypeProviderImpl: KtExpressionTypeProvider

    internal val typeProvider: KtTypeProvider get() = typeProviderImpl
    protected abstract val typeProviderImpl: KtTypeProvider

    internal val typeInfoProvider: KtTypeInfoProvider get() = typeInfoProviderImpl
    protected abstract val typeInfoProviderImpl: KtTypeInfoProvider

    internal val subtypingComponent: KtSubtypingComponent get() = subtypingComponentImpl
    protected abstract val subtypingComponentImpl: KtSubtypingComponent

    internal val expressionInfoProvider: KtExpressionInfoProvider get() = expressionInfoProviderImpl
    protected abstract val expressionInfoProviderImpl: KtExpressionInfoProvider

    internal val visibilityChecker: KtVisibilityChecker get() = visibilityCheckerImpl
    protected abstract val visibilityCheckerImpl: KtVisibilityChecker

    internal val overrideInfoProvider: KtOverrideInfoProvider get() = overrideInfoProviderImpl
    protected abstract val overrideInfoProviderImpl: KtOverrideInfoProvider
}