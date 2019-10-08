/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name

class FirSuperTypeScope(
    session: FirSession,
    val scopes: List<FirScope>
) : AbstractFirOverrideScope(session) {

    private val absentFunctions = mutableSetOf<Name>()

    private val absentProperties = mutableSetOf<Name>()

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        if (name in absentFunctions) {
            return ProcessorAction.NEXT
        }
        val accepted = HashSet<FirFunctionSymbol<*>>()
        val pending = mutableListOf<FirFunctionSymbol<*>>()
        var empty = true
        for (scope in scopes) {
            if (scope.processFunctionsByName(name) { functionSymbol ->
                    empty = false
                    if (functionSymbol !in accepted && functionSymbol.isOverridden(accepted) == null) {
                        pending += functionSymbol
                        processor(functionSymbol)
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
        if (empty) {
            absentFunctions += name
        }
        return super.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        if (name in absentProperties) {
            return ProcessorAction.NEXT
        }
        val accepted = HashSet<FirCallableSymbol<*>>()
        val pending = mutableListOf<FirCallableSymbol<*>>()
        var empty = true
        for (scope in scopes) {
            if (scope.processPropertiesByName(name) {
                    empty = false
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
        if (empty) {
            absentProperties += name
        }
        return super.processPropertiesByName(name, processor)
    }
}