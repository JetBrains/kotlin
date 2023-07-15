/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.Name

abstract class FirContainingNamesAwareScope : FirScope() {
    abstract fun getCallableNames(): Set<Name>

    abstract fun getClassifierNames(): Set<Name>
}

fun FirContainingNamesAwareScope.processAllFunctions(processor: (FirNamedFunctionSymbol) -> Unit) {
    for (name in getCallableNames()) {
        processFunctionsByName(name, processor)
    }
}

fun FirContainingNamesAwareScope.processAllProperties(processor: (FirVariableSymbol<*>) -> Unit) {
    for (name in getCallableNames()) {
        processPropertiesByName(name, processor)
    }
}

/** Processes functions in order, similar to MemberComparator.NameAndTypeMemberComparator:
 * 1) non-extension functions, sorted by name
 * 2) extension functions, sorted by name
 */
fun FirContainingNamesAwareScope.processAllFunctionsSortedByTypeAndName(processor: (FirNamedFunctionSymbol) -> Unit) {
    getCallableNames().sorted()
        .flatMap { getFunctions(it) }
        .partition { !it.isExtension }
        .toList().flatten()
        .forEach {
            processFunctionsByName(it.name, processor)
        }
}

/** Processes properties in order, similar to ClassGenerator.generateClass() does using MemberComparator.NameAndTypeMemberComparator:
 * 1) primary constructor value parameters, sorted by name
 * 2) non-extension properties, sorted by name
 * 3) extension properties, sorted by name
 */
fun FirContainingNamesAwareScope.processAllPropertiesSortedByTypeAndName(
    primaryConstructorValueParamNames: List<Name>,
    processor: (FirVariableSymbol<*>) -> Unit
) {
    val (primaryConstructorValueParams, otherProperties) = getCallableNames().sorted()
        .flatMap { getProperties(it) }
        .partition { it.name in primaryConstructorValueParamNames }
    val otherPropertiesSortedByTypeAndName = otherProperties
        .partition { !it.isExtension }
        .toList().flatten()
    (primaryConstructorValueParams + otherPropertiesSortedByTypeAndName).forEach {
        processPropertiesByName(it.name, processor)
    }
}

fun FirContainingNamesAwareScope.processAllCallables(processor: (FirCallableSymbol<*>) -> Unit) {
    for (name in getCallableNames()) {
        processFunctionsByName(name, processor)
        processPropertiesByName(name, processor)
    }
}

fun FirContainingNamesAwareScope.collectAllProperties(): Collection<FirVariableSymbol<*>> {
    return mutableListOf<FirVariableSymbol<*>>().apply {
        processAllProperties(this::add)
    }
}

fun FirContainingNamesAwareScope.collectAllFunctions(): Collection<FirNamedFunctionSymbol> {
    return mutableListOf<FirNamedFunctionSymbol>().apply {
        processAllFunctions(this::add)
    }
}
