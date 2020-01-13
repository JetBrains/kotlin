/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
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
        processor: (FirClassifierSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        val scopes = if (reversedPriority) scopes.asReversed() else scopes
        for (scope in scopes) {
            if (!scope.processClassifiersByName(name, processor)) {
                return STOP
            }
        }
        return NEXT
    }

    private inline fun <T> processComposite(
        process: FirScope.(Name, (T) -> ProcessorAction) -> ProcessorAction,
        name: Name,
        noinline processor: (T) -> ProcessorAction
    ): ProcessorAction {
        val unique = mutableSetOf<T>()
        val scopes = if (reversedPriority) scopes.asReversed() else scopes
        for (scope in scopes) {
            if (!scope.process(name) {
                    if (unique.add(it)) {
                        processor(it)
                    } else {
                        NEXT
                    }
                }
            ) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        return processComposite(FirScope::processFunctionsByName, name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        return processComposite(FirScope::processPropertiesByName, name, processor)
    }

}
