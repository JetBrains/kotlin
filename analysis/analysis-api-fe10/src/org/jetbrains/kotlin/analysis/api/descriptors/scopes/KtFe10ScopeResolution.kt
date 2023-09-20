/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.scopes

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossiblyNamedSymbol
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*

internal abstract class KtFe10ScopeResolution : KtScope, KtLifetimeOwner {
    abstract val analysisContext: Fe10AnalysisContext
    abstract val scope: ResolutionScope

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        return scope
            .getContributedDescriptors(kindFilter = DescriptorKindFilter.ALL, nameFilter)
            .asSequence()
            .filter { nameFilter(it.name) }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KtCallableSymbol }
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KtCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return getCallableSymbols { it in namesSet }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        return scope
            .getContributedDescriptors(kindFilter = DescriptorKindFilter.CLASSIFIERS, nameFilter)
            .asSequence()
            .filter { nameFilter(it.name) }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KtClassifierSymbol }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return getClassifierSymbols { it in namesSet }
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override val token: KtLifetimeToken
        get() = analysisContext.token
}

internal class KtFe10ScopeLexical(
    override val scope: LexicalScope,
    override val analysisContext: Fe10AnalysisContext
) : KtFe10ScopeResolution(), KtLifetimeOwner {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        return scope
            .getContributedDescriptors(kindFilter = DescriptorKindFilter.FUNCTIONS)
            .asSequence()
            .filterIsInstance<ConstructorDescriptor>()
            .map { it.toKtConstructorSymbol(analysisContext) }
    }

}

internal open class KtFe10ScopeMember(
    override val scope: MemberScope,
    private val constructors: Collection<ConstructorDescriptor>,
    override val analysisContext: Fe10AnalysisContext
) : KtFe10ScopeResolution() {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return scope.getFunctionNames() + scope.getVariableNames()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return scope.getClassifierNames() ?: emptySet()
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = sequence {
        constructors.forEach { yield(it.toKtConstructorSymbol(analysisContext)) }
    }
}

internal open class KtFe10ScopeNonStaticMember(
    scope: MemberScope,
    constructors: Collection<ConstructorDescriptor>,
    analysisContext: Fe10AnalysisContext
) : KtFe10ScopeMember(scope, constructors, analysisContext) {
    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> =
        super.getClassifierSymbols(nameFilter).filter { it is KtNamedClassOrObjectSymbol && it.isInner }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        super.getCallableSymbols(nameFilter).filter { symbol ->
            when (symbol) {
                is KtFunctionSymbol -> !symbol.isStatic
                is KtPropertySymbol -> !symbol.isStatic
                else -> true
            }
        }
    }
}

internal class KtFe10ScopeImporting(
    override val scope: ImportingScope,
    override val analysisContext: Fe10AnalysisContext
) : KtFe10ScopeResolution() {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return getCallableSymbols().mapNotNullTo(mutableSetOf()) { (it as? KtPossiblyNamedSymbol)?.name }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return getClassifierSymbols().mapNotNullTo(mutableSetOf()) { (it as? KtPossiblyNamedSymbol)?.name }
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion { emptySequence() }
}