/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.scopes

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

internal abstract class KtFe10ResolutionScope(protected val analysisContext: Fe10AnalysisContext) : KtScope, KtLifetimeOwner {
    protected abstract fun getRawCallableSymbols(nameFilter: KtScopeNameFilter): Collection<DeclarationDescriptor>
    protected abstract fun getRawClassifierSymbols(nameFilter: KtScopeNameFilter): Collection<DeclarationDescriptor>
    protected abstract fun getRawConstructorSymbols(): Collection<DeclarationDescriptor>

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> {
        return getRawCallableSymbols(nameFilter)
            .asSequence()
            .filter { nameFilter(it.name) }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KtCallableSymbol }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        return getRawClassifierSymbols(nameFilter)
            .asSequence()
            .filter { nameFilter(it.name) }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KtClassifierSymbol }
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        return getRawConstructorSymbols()
            .asSequence()
            .filterIsInstance<ConstructorDescriptor>()
            .map { it.toKtConstructorSymbol(analysisContext) }
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> {
        return emptySequence()
    }

    override val token: KtLifetimeToken
        get() = analysisContext.token
}