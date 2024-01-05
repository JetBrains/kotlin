/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

abstract class FirTypeScope : FirContainingNamesAwareScope() {
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
    // - It may return the same overridden symbols more than once in case of substitution or intersection
    //     (but with different base scope)
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

        override fun toString(): String {
            return "Empty scope"
        }
    }
}

data class MemberWithBaseScope<out D : FirCallableSymbol<*>>(val member: D, val baseScope: FirTypeScope)

typealias ProcessOverriddenWithBaseScope<D> = FirTypeScope.(D, (D, FirTypeScope) -> ProcessorAction) -> ProcessorAction
typealias ProcessAllOverridden<D> = FirTypeScope.(D, (D) -> ProcessorAction) -> ProcessorAction

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

fun FirTypeScope.anyOverriddenOf(
    functionSymbol: FirNamedFunctionSymbol,
    predicate: (FirNamedFunctionSymbol) -> Boolean
): Boolean {
    var result = false
    processOverriddenFunctions(functionSymbol) {
        if (predicate(it)) {
            result = true
            return@processOverriddenFunctions ProcessorAction.STOP
        }

        return@processOverriddenFunctions ProcessorAction.NEXT
    }

    return result
}

private fun FirTypeScope.processOverriddenFunctionsWithVisited(
    functionSymbol: FirNamedFunctionSymbol,
    visited: MutableSet<Pair<FirTypeScope, FirNamedFunctionSymbol>>,
    processor: (FirNamedFunctionSymbol) -> ProcessorAction
): ProcessorAction =
    doProcessAllOverriddenCallables(
        functionSymbol,
        processor,
        FirTypeScope::processDirectOverriddenFunctionsWithBaseScope,
        visited
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

private fun FirTypeScope.processOverriddenPropertiesWithVisited(
    propertySymbol: FirPropertySymbol,
    visited: MutableSet<Pair<FirTypeScope, FirPropertySymbol>> = mutableSetOf(),
    processor: (FirPropertySymbol) -> ProcessorAction
): ProcessorAction =
    doProcessAllOverriddenCallables(
        propertySymbol,
        processor,
        FirTypeScope::processDirectOverriddenPropertiesWithBaseScope,
        visited
    )

fun List<FirTypeScope>.processOverriddenFunctions(
    functionSymbol: FirNamedFunctionSymbol,
    processor: (FirNamedFunctionSymbol) -> ProcessorAction
) {
    val visited = mutableSetOf<Pair<FirTypeScope, FirNamedFunctionSymbol>>()
    for (scope in this) {
        if (!scope.processOverriddenFunctionsWithVisited(functionSymbol, visited, processor)) return
    }
}

fun List<FirTypeScope>.processOverriddenProperties(
    propertySymbol: FirPropertySymbol,
    processor: (FirPropertySymbol) -> ProcessorAction
) {
    val visited = mutableSetOf<Pair<FirTypeScope, FirPropertySymbol>>()
    for (scope in this) {
        if (!scope.processOverriddenPropertiesWithVisited(propertySymbol, visited, processor)) return
    }
}

private fun <S : FirCallableSymbol<*>> FirTypeScope.doProcessAllOverriddenCallables(
    callableSymbol: S,
    processor: (S, FirTypeScope) -> ProcessorAction,
    processDirectOverriddenCallablesWithBaseScope: FirTypeScope.(S, (S, FirTypeScope) -> ProcessorAction) -> ProcessorAction,
    visited: MutableSet<Pair<FirTypeScope, S>>
): ProcessorAction {
    if (!visited.add(this to callableSymbol)) return ProcessorAction.NONE
    return processDirectOverriddenCallablesWithBaseScope(callableSymbol) { overridden, baseScope ->
        if (!processor(overridden, baseScope)) return@processDirectOverriddenCallablesWithBaseScope ProcessorAction.STOP

        baseScope.doProcessAllOverriddenCallables(overridden, processor, processDirectOverriddenCallablesWithBaseScope, visited)
    }
}

fun <S : FirCallableSymbol<*>> FirTypeScope.processAllOverriddenCallables(
    callableSymbol: S,
    processor: (S) -> ProcessorAction,
    processDirectOverriddenCallablesWithBaseScope: ProcessOverriddenWithBaseScope<S>,
): ProcessorAction =
    doProcessAllOverriddenCallables(callableSymbol, processor, processDirectOverriddenCallablesWithBaseScope, mutableSetOf())

private fun <S : FirCallableSymbol<*>> FirTypeScope.doProcessAllOverriddenCallables(
    callableSymbol: S,
    processor: (S) -> ProcessorAction,
    processDirectOverriddenCallablesWithBaseScope: FirTypeScope.(S, (S, FirTypeScope) -> ProcessorAction) -> ProcessorAction,
    visited: MutableSet<Pair<FirTypeScope, S>>
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

fun FirTypeScope.getDirectOverriddenMembersWithBaseScope(member: FirCallableSymbol<*>): List<MemberWithBaseScope<FirCallableSymbol<*>>> {
    return when (member) {
        is FirNamedFunctionSymbol -> getDirectOverriddenFunctionsWithBaseScope(member)
        is FirPropertySymbol -> getDirectOverriddenPropertiesWithBaseScope(member)
        else -> emptyList()
    }
}

fun FirTypeScope.getDirectOverriddenFunctionsWithBaseScope(
    function: FirNamedFunctionSymbol,
): List<MemberWithBaseScope<FirNamedFunctionSymbol>> {
    val overriddenFunctions = mutableSetOf<MemberWithBaseScope<FirNamedFunctionSymbol>>()

    processDirectOverriddenFunctionsWithBaseScope(function) { symbol, baseScope ->

        overriddenFunctions += MemberWithBaseScope(symbol, baseScope)
        ProcessorAction.NEXT
    }

    return overriddenFunctions.toList()
}

fun FirTypeScope.getDirectOverriddenPropertiesWithBaseScope(
    property: FirPropertySymbol,
): List<MemberWithBaseScope<FirPropertySymbol>> {
    val overriddenProperties = mutableSetOf<MemberWithBaseScope<FirPropertySymbol>>()

    processDirectOverriddenPropertiesWithBaseScope(property) { symbol, baseScope ->
        overriddenProperties += MemberWithBaseScope(symbol, baseScope)
        ProcessorAction.NEXT
    }

    return overriddenProperties.toList()
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

    /*
     * The original symbol may appear in `processOverriddenFunctions`, so it should be removed from the resulting
     *   list to not confuse the caller with a situation when the function directly overrides itself
     *
     * For details see FirTypeScope.processDirectOverriddenFunctionsWithBaseScope
     */
    overriddenFunctions -= function

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

    /*
     * See comment in `getDirectOverriddenFunctions` function above
     */
    overriddenProperties -= property

    return overriddenProperties.toList()
}

/**
 * Provides a list of callables which are directly overridden by the given symbol
 *
 * Please be very accurate with using this function.
 * It can be convenient if the only thing you need is to get directly overridden symbols and nothing more,
 * but even in this case please check that you are using a correct scope.
 * E.g. if you want to get overridden symbols of some Foo.bar,
 * the scope in use must be built from the Foo-based type or Foo class itself.
 *
 * If you need to traverse some complex overridden hierarchy,
 * please consider using processDirectOverriddenFunctions(Properties)WithBaseScope instead.
 *
 * @param memberSymbol A callable symbol to find its directly overridden symbols
 * @receiver Must be an owner scope of the callable symbol to work properly
 * @return A list of callable symbols which are directly overridden by the given symbol
 */
fun FirTypeScope.retrieveDirectOverriddenOf(memberSymbol: FirCallableSymbol<*>): List<FirCallableSymbol<*>> {
    return when (memberSymbol) {
        is FirNamedFunctionSymbol -> {
            processFunctionsByName(memberSymbol.name) {}
            getDirectOverriddenFunctions(memberSymbol)
        }

        is FirPropertySymbol -> {
            processPropertiesByName(memberSymbol.name) {}
            getDirectOverriddenProperties(memberSymbol)
        }

        else -> throw IllegalArgumentException("unexpected member kind $memberSymbol")
    }
}

private inline fun <reified D : FirCallableSymbol<*>> MutableSet<D>.addOverridden(
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
