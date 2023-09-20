/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.*
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

fun FirContainingNamesAwareScope.processAllCallables(processor: (FirCallableSymbol<*>) -> Unit) {
    for (name in getCallableNames()) {
        processFunctionsByName(name, processor)
        processPropertiesByName(name, processor)
    }
}

fun FirContainingNamesAwareScope.processAllClassifiers(processor: (FirClassifierSymbol<*>) -> Unit) {
    for (name in getClassifierNames()) {
        processClassifiersByName(name, processor)
    }
}

inline fun <reified T : FirCallableSymbol<*>> collectLeafCallablesByName(
    name: Name,
    processCallablesByName: (Name, (T) -> Unit) -> Unit,
    crossinline processDirectlyOverriddenCallables: (T, (T) -> ProcessorAction) -> Unit,
): List<T> {
    val collected = mutableSetOf<T>()
    val bases = mutableSetOf<T>()

    processCallablesByName(name) { function ->
        processDirectlyOverriddenCallables(function) {
            bases.add(it)
            ProcessorAction.NEXT
        }

        if (function !in bases) {
            collected.add(function)
        }
    }

    return collected.filter { it !in bases }
}

fun FirTypeScope.collectLeafFunctionsByName(name: Name): List<FirNamedFunctionSymbol> =
    collectLeafCallablesByName(name, ::processFunctionsByName, ::processDirectlyOverriddenFunctions)

fun FirTypeScope.collectLeafPropertiesByName(name: Name): List<FirVariableSymbol<*>> =
    collectLeafCallablesByName(name, ::processPropertiesByName) { variable, process ->
        if (variable is FirPropertySymbol) {
            processDirectlyOverriddenProperties(variable, process)
        }
    }

fun FirTypeScope.collectLeafFunctions(): List<FirNamedFunctionSymbol> = buildList {
    for (name in getCallableNames()) {
        this += collectLeafFunctionsByName(name)
    }
}

fun FirTypeScope.collectLeafProperties(): List<FirVariableSymbol<*>> = buildList {
    for (name in getCallableNames()) {
        this += collectLeafPropertiesByName(name)
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
