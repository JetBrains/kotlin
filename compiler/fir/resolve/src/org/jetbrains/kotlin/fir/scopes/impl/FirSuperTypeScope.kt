/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name

class FirSuperTypeScope(
    session: FirSession,
    val scopes: List<FirScope>
) : AbstractFirOverrideScope(session) {

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val accepted = HashSet<ConeFunctionSymbol>()
        val pending = mutableListOf<ConeFunctionSymbol>()
        for (scope in scopes) {
            if (scope.processFunctionsByName(name) {

                    if (it !in accepted && it.isOverridden(accepted) == null) {
                        pending += it
                        processor(it)
                    } else {
                        ProcessorAction.NEXT
                    }
                }.stop()
            ) {
                return ProcessorAction.STOP
            }
            accepted += pending
            pending.clear()
        }
        return super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val accepted = HashSet<FirCallableSymbol<*>>()
        val pending = mutableListOf<FirCallableSymbol<*>>()
        for (scope in scopes) {
            if (scope.processPropertiesByName(name) {
                    if (it !in accepted && it.isOverridden(accepted) == null) {
                        pending += it
                        processor(it)
                    } else {
                        ProcessorAction.NEXT
                    }
                }.stop()
            ) {
                return ProcessorAction.STOP
            }
            accepted += pending
            pending.clear()
        }
        return super.processPropertiesByName(name, processor)
    }
}