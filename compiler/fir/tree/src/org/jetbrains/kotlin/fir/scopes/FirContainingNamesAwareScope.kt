/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
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
