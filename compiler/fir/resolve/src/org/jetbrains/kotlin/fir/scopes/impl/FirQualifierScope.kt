/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.Name

class FirQualifierScope(
    private val delegateCallablesScope: FirScope?,
    private val delegateClassifiersScope: FirScope?
) : FirScope() {
    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        return delegateClassifiersScope?.processClassifiersByName(name) {
            if (it is FirRegularClassSymbol) {
                processor(it)
            } else {
                ProcessorAction.NEXT
            }
        } ?: ProcessorAction.NEXT
    }

    override fun processFunctionsByName(
        name: Name,
        processor: (FirFunctionSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        return delegateCallablesScope?.processFunctionsByName(name, processor) ?: ProcessorAction.NEXT
    }

    override fun processPropertiesByName(
        name: Name,
        processor: (FirCallableSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        return delegateCallablesScope?.processPropertiesByName(name, processor) ?: ProcessorAction.NEXT
    }
}