/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.scopes

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaPossiblyNamedSymbol
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*

internal abstract class KaFe10ScopeResolution : KaScope, KaLifetimeOwner {
    abstract val analysisContext: Fe10AnalysisContext
    abstract val scope: ResolutionScope

    override fun getCallableSymbols(nameFilter: KaScopeNameFilter): Sequence<KaCallableSymbol> = withValidityAssertion {
        return scope
            .getContributedDescriptors(kindFilter = DescriptorKindFilter.ALL, nameFilter)
            .asSequence()
            .filter { nameFilter(it.name) }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KaCallableSymbol }
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return getCallableSymbols { it in namesSet }
    }

    override fun getClassifierSymbols(nameFilter: KaScopeNameFilter): Sequence<KaClassifierSymbol> = withValidityAssertion {
        return scope
            .getContributedDescriptors(kindFilter = DescriptorKindFilter.CLASSIFIERS, nameFilter)
            .asSequence()
            .filter { nameFilter(it.name) }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KaClassifierSymbol }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return getClassifierSymbols { it in namesSet }
    }

    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override val token: KaLifetimeToken
        get() = analysisContext.token
}

internal class KaFe10ScopeLexical(
    override val scope: LexicalScope,
    override val analysisContext: Fe10AnalysisContext
) : KaFe10ScopeResolution(), KaLifetimeOwner {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion {
        return scope
            .getContributedDescriptors(kindFilter = DescriptorKindFilter.FUNCTIONS)
            .asSequence()
            .filterIsInstance<ConstructorDescriptor>()
            .map { it.toKtConstructorSymbol(analysisContext) }
    }

}

internal open class KaFe10ScopeMember(
    override val scope: MemberScope,
    private val constructors: Collection<ConstructorDescriptor>,
    override val analysisContext: Fe10AnalysisContext
) : KaFe10ScopeResolution() {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return scope.getFunctionNames() + scope.getVariableNames()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return scope.getClassifierNames() ?: emptySet()
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = sequence {
        constructors.forEach { yield(it.toKtConstructorSymbol(analysisContext)) }
    }
}

internal open class KaFe10ScopeNonStaticMember(
    scope: MemberScope,
    constructors: Collection<ConstructorDescriptor>,
    analysisContext: Fe10AnalysisContext
) : KaFe10ScopeMember(scope, constructors, analysisContext) {
    override fun getClassifierSymbols(nameFilter: KaScopeNameFilter): Sequence<KaClassifierSymbol> =
        super.getClassifierSymbols(nameFilter).filter { it is KaNamedClassOrObjectSymbol && it.isInner }

    override fun getCallableSymbols(nameFilter: KaScopeNameFilter): Sequence<KaCallableSymbol> = withValidityAssertion {
        super.getCallableSymbols(nameFilter).filter { symbol ->
            when (symbol) {
                is KaFunctionSymbol -> !symbol.isStatic
                is KaPropertySymbol -> !symbol.isStatic
                else -> true
            }
        }
    }
}

internal class KaFe10ScopeImporting(
    override val scope: ImportingScope,
    override val analysisContext: Fe10AnalysisContext
) : KaFe10ScopeResolution() {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return getCallableSymbols().mapNotNullTo(mutableSetOf()) { (it as? KaPossiblyNamedSymbol)?.name }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return getClassifierSymbols().mapNotNullTo(mutableSetOf()) { (it as? KaPossiblyNamedSymbol)?.name }
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion { emptySequence() }
}