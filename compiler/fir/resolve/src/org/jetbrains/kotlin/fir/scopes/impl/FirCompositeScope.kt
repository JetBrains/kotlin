/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name

class FirCompositeScope(
    val scopes: MutableList<FirScope>,
    private val reversedPriority: Boolean = false
) : FirScope() {
    constructor(vararg scopes: FirScope) : this(scopes.toMutableList())

    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> Unit
    ) {
        val scopes = if (reversedPriority) scopes.asReversed() else scopes
        for (scope in scopes) {
            scope.processClassifiersByName(name, processor)
        }
    }

    private inline fun <T> processComposite(
        process: FirScope.(Name, (T) -> Unit) -> Unit,
        name: Name,
        noinline processor: (T) -> Unit
    ) {
        val unique = mutableSetOf<T>()
        val scopes = if (reversedPriority) scopes.asReversed() else scopes
        for (scope in scopes) {
            scope.process(name) {
                if (unique.add(it)) {
                    processor(it)
                }
            }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        return processComposite(FirScope::processFunctionsByName, name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> Unit) {
        return processComposite(FirScope::processPropertiesByName, name, processor)
    }

}
