/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

/**
 * Special type scope for unstable smartcast. The purpose of this scope is only to report "SMARTCAST_IMPOSSIBLE" diagnostics.
 *
 * This scope will serve all candidates available in the original scope. In addition, it also serve all additional members that are
 * available from the smartcast type. This way, these additional members can be resolved. Later in
 * [org.jetbrains.kotlin.fir.resolve.calls.CheckDispatchReceiver], these additional members are rejected with "UnstableSmartcast"
 * diagnostic, which surfaces as "SMARTCAST_IMPOSSIBLE" diagnostic.
 */
class FirUnstableSmartcastTypeScope(
    private val smartcastType: ConeKotlinType,
    private val smartcastScope: FirTypeScope,
    private val originalScope: FirTypeScope
) : FirTypeScope(), FirContainingNamesAwareScope {
    private val scopes = listOf(smartcastScope, originalScope)
    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        for (scope in scopes) {
            scope.processClassifiersByNameWithSubstitution(name, processor)
        }
    }

    private inline fun <T> processComposite(
        process: FirTypeScope.(Name, (T) -> Unit) -> Unit,
        name: Name,
        noinline processor: (T) -> Unit
    ) {
        val unique = mutableSetOf<T>()
        for (scope in scopes) {
            scope.process(name) {
                if (unique.add(it)) {
                    processor(it)
                }
            }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        return processComposite(FirScope::processFunctionsByName, name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return processComposite(FirScope::processPropertiesByName, name, processor)
    }

    private inline fun <N, T : FirCallableSymbol<*>> processTypedComposite(
        process: FirTypeScope.(N, (T, FirTypeScope) -> ProcessorAction) -> ProcessorAction,
        name: N,
        noinline processor: (T, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        originalScope.process(name) { symbol, firTypeScope ->
            processor(symbol, firTypeScope)
        }.let { if (it == ProcessorAction.STOP) return ProcessorAction.STOP }

        smartcastScope.process(name) { symbol, firTypeScope ->
            // Only process the symbol if the dispatcher type is exactly the smartcast type. This way, we don't add any additional
            // symbols that already exists in the original scope.
            if ((symbol.fir as? FirCallableMemberDeclaration)?.dispatchReceiverType == smartcastType) {
                processor(symbol, firTypeScope)
            } else {
                ProcessorAction.NEXT
            }
        }.let { if (it == ProcessorAction.STOP) return ProcessorAction.STOP }
        return ProcessorAction.NEXT
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return processTypedComposite(FirTypeScope::processDirectOverriddenFunctionsWithBaseScope, functionSymbol, processor)
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return processTypedComposite(FirTypeScope::processDirectOverriddenPropertiesWithBaseScope, propertySymbol, processor)
    }

    override fun getCallableNames(): Set<Name> {
        return scopes.flatMapTo(hashSetOf()) { it.getContainingCallableNamesIfPresent() }
    }

    override fun getClassifierNames(): Set<Name> {
        return scopes.flatMapTo(hashSetOf()) { it.getContainingClassifierNamesIfPresent() }
    }

    override val scopeOwnerLookupNames: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scopes.flatMap { it.scopeOwnerLookupNames }
    }
}
