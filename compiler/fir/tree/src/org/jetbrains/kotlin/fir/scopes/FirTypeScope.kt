/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.isIntersectionOverride
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

abstract class FirTypeScope : FirScope(), FirContainingNamesAwareScope {
    // If the scope instance is the same as the one from which the symbol was originated, this function supplies
    // all direct overridden members (each of them comes a base scope where grand-parents [overridden of this overridden] may be obtained from)
    //
    // For example:
    //
    // interface A { fun foo() }
    // interface B : A { override fun foo() }
    // interface C : B { override fun foo() }
    //
    // Here, for override C::foo from a scope of C, processor will receiver B::foo and scope for B
    // Then, for B::foo from scope for B one may receive override A::foo and scope for A
    //
    // Currently, this function and its property brother both have very weak guarantees
    // - It may silently do nothing on symbols originated from different scope instance
    // - It may return the same overridden symbols more then once in case of substitution
    abstract fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction

    // ------------------------------------------------------------------------------------

    abstract fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction

    // ------------------------------------------------------------------------------------

    object Empty : FirTypeScope() {
        override fun processDirectOverriddenFunctionsWithBaseScope(
            functionSymbol: FirNamedFunctionSymbol,
            processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT

        override fun processDirectOverriddenPropertiesWithBaseScope(
            propertySymbol: FirPropertySymbol,
            processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT

        override fun getCallableNames(): Set<Name> = emptySet()

        override fun getClassifierNames(): Set<Name> = emptySet()
    }

    protected companion object {
        fun <S : FirCallableSymbol<*>> doProcessDirectOverriddenCallables(
            callableSymbol: S,
            processor: (S, FirTypeScope) -> ProcessorAction,
            directOverriddenMap: Map<S, Collection<S>>,
            baseScope: FirTypeScope,
            processDirectOverriddenCallables: FirTypeScope.(S, (S, FirTypeScope) -> ProcessorAction) -> ProcessorAction
        ): ProcessorAction {
            val directOverridden = directOverriddenMap[callableSymbol]?.takeIf { it.isNotEmpty() }
                ?: return baseScope.processDirectOverriddenCallables(callableSymbol, processor)

            for (overridden in directOverridden) {
                if (overridden.fir.isIntersectionOverride) {
                    if (!baseScope.processDirectOverriddenCallables(overridden, processor)) return ProcessorAction.STOP
                }
                if (!processor(overridden, baseScope)) return ProcessorAction.STOP
            }

            return ProcessorAction.NONE
        }
    }
}

typealias ProcessOverriddenWithBaseScope<D> = FirTypeScope.(D, (D, FirTypeScope) -> ProcessorAction) -> ProcessorAction

fun FirTypeScope.processOverriddenFunctions(
    functionSymbol: FirNamedFunctionSymbol,
    processor: (FirNamedFunctionSymbol) -> ProcessorAction
): ProcessorAction =
    doProcessAllOverriddenCallables(
        functionSymbol,
        processor,
        FirTypeScope::processDirectOverriddenFunctionsWithBaseScope,
        mutableSetOf()
    )

fun FirTypeScope.processOverriddenProperties(
    propertySymbol: FirPropertySymbol,
    processor: (FirPropertySymbol) -> ProcessorAction
): ProcessorAction =
    doProcessAllOverriddenCallables(
        propertySymbol,
        processor,
        FirTypeScope::processDirectOverriddenPropertiesWithBaseScope,
        mutableSetOf()
    )

private fun <S : FirCallableSymbol<*>> FirTypeScope.doProcessAllOverriddenCallables(
    callableSymbol: S,
    processor: (S, FirTypeScope) -> ProcessorAction,
    processDirectOverriddenCallablesWithBaseScope: FirTypeScope.(S, (S, FirTypeScope) -> ProcessorAction) -> ProcessorAction,
    visited: MutableSet<S>
): ProcessorAction {
    if (!visited.add(callableSymbol)) return ProcessorAction.NONE
    return processDirectOverriddenCallablesWithBaseScope(callableSymbol) { overridden, baseScope ->
        if (!processor(overridden, baseScope)) return@processDirectOverriddenCallablesWithBaseScope ProcessorAction.STOP

        baseScope.doProcessAllOverriddenCallables(overridden, processor, processDirectOverriddenCallablesWithBaseScope, visited)
    }
}

private fun <S : FirCallableSymbol<*>> FirTypeScope.doProcessAllOverriddenCallables(
    callableSymbol: S,
    processor: (S) -> ProcessorAction,
    processDirectOverriddenCallablesWithBaseScope: FirTypeScope.(S, (S, FirTypeScope) -> ProcessorAction) -> ProcessorAction,
    visited: MutableSet<S>
): ProcessorAction =
    doProcessAllOverriddenCallables(callableSymbol, { s, _ -> processor(s) }, processDirectOverriddenCallablesWithBaseScope, visited)

inline fun FirTypeScope.processDirectlyOverriddenFunctions(
    functionSymbol: FirNamedFunctionSymbol,
    crossinline processor: (FirNamedFunctionSymbol) -> ProcessorAction
): ProcessorAction = processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { overridden, _ ->
    processor(overridden)
}

inline fun FirTypeScope.processDirectlyOverriddenProperties(
    propertySymbol: FirPropertySymbol,
    crossinline processor: (FirPropertySymbol) -> ProcessorAction
): ProcessorAction = processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { overridden, _ ->
    processor(overridden)
}

fun FirTypeScope.getDirectOverriddenMembers(
    member: FirCallableSymbol<*>,
    unwrapIntersectionAndSubstitutionOverride: Boolean = false,
): List<FirCallableSymbol<out FirCallableDeclaration>> =
    when (member) {
        is FirNamedFunctionSymbol -> getDirectOverriddenFunctions(member, unwrapIntersectionAndSubstitutionOverride)
        is FirPropertySymbol -> getDirectOverriddenProperties(member, unwrapIntersectionAndSubstitutionOverride)
        else -> emptyList()
    }

fun FirTypeScope.getDirectOverriddenFunctions(
    function: FirNamedFunctionSymbol,
    unwrapIntersectionAndSubstitutionOverride: Boolean = false,
): List<FirNamedFunctionSymbol> {
    val overriddenFunctions = mutableSetOf<FirNamedFunctionSymbol>()

    processDirectlyOverriddenFunctions(function) {
        overriddenFunctions.addOverridden(it, unwrapIntersectionAndSubstitutionOverride)
        ProcessorAction.NEXT
    }

    return overriddenFunctions.toList()
}

fun FirTypeScope.getDirectOverriddenProperties(
    property: FirPropertySymbol,
    unwrapIntersectionAndSubstitutionOverride: Boolean = false,
): List<FirPropertySymbol> {
    val overriddenProperties = mutableSetOf<FirPropertySymbol>()

    processDirectlyOverriddenProperties(property) {
        overriddenProperties.addOverridden(it, unwrapIntersectionAndSubstitutionOverride)
        ProcessorAction.NEXT
    }

    return overriddenProperties.toList()
}

private inline fun <reified D : FirCallableSymbol<*>> MutableCollection<D>.addOverridden(
    symbol: D,
    unwrapIntersectionAndSubstitutionOverride: Boolean
) {
    if (unwrapIntersectionAndSubstitutionOverride) {
        if (symbol is FirIntersectionCallableSymbol) {
            @Suppress("UNCHECKED_CAST")
            addAll(symbol.intersections as Collection<D>)
        } else {
            add(symbol.originalForSubstitutionOverride ?: symbol)
        }
    } else {
        add(symbol)
    }
}
