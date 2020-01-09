/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name

abstract class FirScope {
    open fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> Unit
    ) {}

    open fun processFunctionsByName(
        name: Name,
        processor: (FirFunctionSymbol<*>) -> Unit
    ) {}

    open fun processPropertiesByName(
        name: Name,
        // NB: it'd be great to write FirVariableSymbol<*> here, but there is FirAccessorSymbol :(
        processor: (FirCallableSymbol<*>) -> Unit
    ) {}
}

enum class ProcessorAction {
    STOP,
    NEXT,
    NONE;

    operator fun not(): Boolean {
        return when (this) {
            STOP -> true
            NEXT -> false
            NONE -> false
        }
    }

    fun stop() = this == STOP
    fun next() = this != STOP
}
