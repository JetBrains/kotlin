/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*

internal abstract class KaFe10ScopeResolution : KaScope, KaLifetimeOwner {
    abstract val analysisContext: Fe10AnalysisContext
    abstract val scope: ResolutionScope

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = withValidityAssertion {
        return scope
            .getContributedDescriptors(kindFilter = DescriptorKindFilter.ALL, nameFilter)
            .asSequence()
            .filter { nameFilter(it.name) }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KaCallableSymbol }
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return callables { it in namesSet }
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = withValidityAssertion {
        return scope
            .getContributedDescriptors(kindFilter = DescriptorKindFilter.CLASSIFIERS, nameFilter)
            .asSequence()
            .filter { nameFilter(it.name) }
            .mapNotNull { it.toKtSymbol(analysisContext) as? KaClassifierSymbol }
    }

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return classifiers { it in namesSet }
    }

    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = withValidityAssertion {
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

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion {
            return scope
                .getContributedDescriptors(kindFilter = DescriptorKindFilter.FUNCTIONS)
                .asSequence()
                .filterIsInstance<ConstructorDescriptor>()
                .map { it.toKtConstructorSymbol(analysisContext) }
        }
}

internal open class KaFe10ScopeMember(
    override val scope: MemberScope,
    private val constructorDescriptors: Collection<ConstructorDescriptor>,
    override val analysisContext: Fe10AnalysisContext
) : KaFe10ScopeResolution() {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return scope.getFunctionNames() + scope.getVariableNames()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return scope.getClassifierNames() ?: emptySet()
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion {
            sequence {
                constructorDescriptors.forEach { yield(it.toKtConstructorSymbol(analysisContext)) }
            }
        }
}

internal open class KaFe10ScopeNonStaticMember(
    scope: MemberScope,
    constructors: Collection<ConstructorDescriptor>,
    analysisContext: Fe10AnalysisContext
) : KaFe10ScopeMember(scope, constructors, analysisContext) {
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> =
        super.classifiers(nameFilter).filter { it is KaNamedClassSymbol && it.isInner }

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = withValidityAssertion {
        super.callables(nameFilter).filter { symbol ->
            when (symbol) {
                is KaNamedFunctionSymbol -> !symbol.isStatic
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
        return callables.mapNotNullTo(mutableSetOf()) { it.name }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return classifiers.mapNotNullTo(mutableSetOf()) { it.name }
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }
}
