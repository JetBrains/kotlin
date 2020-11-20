/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

interface FirContainingNamesAwareScope {
    fun getCallableNames(): Set<Name>

    fun getClassifierNames(): Set<Name>
}

fun FirScope.getContainingCallableNamesIfPresent(): Set<Name> =
    if (this is FirContainingNamesAwareScope) getCallableNames() else emptySet()

fun FirScope.getContainingClassifierNamesIfPresent(): Set<Name> =
    if (this is FirContainingNamesAwareScope) getClassifierNames() else emptySet()

fun <S> S.processAllFunctions(processor: (FirFunctionSymbol<*>) -> Unit) where S : FirScope, S : FirContainingNamesAwareScope {
    for (name in getCallableNames()) {
        processFunctionsByName(name, processor)
    }
}

fun <S> S.processAllProperties(processor: (FirVariableSymbol<*>) -> Unit) where S : FirScope, S : FirContainingNamesAwareScope {
    for (name in getCallableNames()) {
        processPropertiesByName(name, processor)
    }
}

fun <S> S.collectAllFunctions(): Collection<FirFunctionSymbol<*>> where S : FirScope, S : FirContainingNamesAwareScope {
    return mutableListOf<FirFunctionSymbol<*>>().apply {
        processAllFunctions(this::add)
    }
}

fun <S> S.collectAllProperties(): Collection<FirVariableSymbol<*>> where S : FirScope, S : FirContainingNamesAwareScope {
    return mutableListOf<FirVariableSymbol<*>>().apply {
        processAllProperties(this::add)
    }
}
