/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.StandardClassIds

fun filterOutOverriddenFunctions(extractedOverridden: Collection<MemberWithBaseScope<FirNamedFunctionSymbol>>): Collection<MemberWithBaseScope<FirNamedFunctionSymbol>> {
    return filterOutOverridden(extractedOverridden, FirTypeScope::processDirectOverriddenFunctionsWithBaseScope)
}

fun filterOutOverriddenProperties(extractedOverridden: Collection<MemberWithBaseScope<FirPropertySymbol>>): Collection<MemberWithBaseScope<FirPropertySymbol>> {
    return filterOutOverridden(extractedOverridden, FirTypeScope::processDirectOverriddenPropertiesWithBaseScope)
}

fun <D : FirCallableSymbol<*>> filterOutOverridden(
    extractedOverridden: Collection<MemberWithBaseScope<D>>,
    processAllOverridden: ProcessOverriddenWithBaseScope<D>,
): Collection<MemberWithBaseScope<D>> {
    return extractedOverridden.filter { overridden1 ->
        extractedOverridden.none { overridden2 ->
            overridden1 !== overridden2 && overrides(
                overridden2,
                overridden1.member,
            ) { symbol: D, processor: (D) -> ProcessorAction ->
                processAllOverriddenCallables(symbol, processor, processAllOverridden)
            }
        }
    }
}

// Whether f overrides g
fun <D : FirCallableSymbol<*>> overrides(
    f: MemberWithBaseScope<D>,
    gMember: D,
    processAllOverridden: ProcessAllOverridden<D>,
): Boolean {
    val (fMember, fScope) = f

    var result = false

    fScope.processAllOverridden(fMember) { overridden ->
        if (overridden == gMember) {
            result = true
            ProcessorAction.STOP
        } else {
            ProcessorAction.NEXT
        }
    }

    return result
}

inline fun chooseIntersectionVisibilityOrNull(
    nonSubsumedOverrides: Collection<FirCallableSymbol<*>>,
    isAbstract: (FirCallableSymbol<*>) -> Boolean = FirCallableSymbol<*>::isAbstractAccordingToRawStatus,
): Visibility? = chooseIntersectionVisibilityOrNull(
    nonSubsumedOverrides,
    toSymbol = { it },
    isAbstract,
)

inline fun <D> chooseIntersectionVisibilityOrNull(
    nonSubsumedOverrides: Collection<D>,
    toSymbol: (D) -> FirCallableSymbol<*>,
    isAbstract: (D) -> Boolean,
): Visibility? {
    val nonAbstract = nonSubsumedOverrides.filter {
        // Kotlin's Cloneable interface contains phantom `protected open fun clone()`.
        !isAbstract(it) && toSymbol(it).callableId != StandardClassIds.Callables.clone
    }
    val allAreAbstract = nonAbstract.isEmpty()

    if (allAreAbstract) {
        return findMaxVisibilityOrNull(nonSubsumedOverrides, toSymbol)
    }

    return nonAbstract.singleOrNull()?.let(toSymbol)?.rawStatus?.visibility
}

val FirCallableSymbol<*>.isAbstractAccordingToRawStatus: Boolean
    get() {
        val responsibleDeclaration = when (this) {
            !is FirPropertyAccessorSymbol -> this
            else -> propertySymbol
        }
        // This function is expected to be called during FirResolvePhase.STATUS,
        // meaning we can't yet access `resolvedStatus`, because it would require
        // the same phase, but by this time we expect the statuses to have been
        // calculated de-facto.
        require(responsibleDeclaration.rawStatus is FirResolvedDeclarationStatus)
        return responsibleDeclaration.rawStatus.modality == Modality.ABSTRACT
    }

inline fun <D> findMaxVisibilityOrNull(
    extractedOverrides: Collection<D>,
    toSymbol: (D) -> FirCallableSymbol<*>,
): Visibility? {
    var maxVisibility: Visibility = Visibilities.Private

    for (override in extractedOverrides) {
        val visibility = (toSymbol(override).fir as FirMemberDeclaration).visibility
        val compare = Visibilities.compare(visibility, maxVisibility) ?: return null

        if (compare > 0) {
            maxVisibility = visibility
        }
    }

    return maxVisibility
}
