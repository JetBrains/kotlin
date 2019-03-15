/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.name.Name

class FirTopLevelDeclaredMemberScope(
    file: FirFile,
    session: FirSession,
    lookupInFir: Boolean = true
) : FirAbstractProviderBasedScope(session, lookupInFir) {
    private val packageFqName = file.packageFqName

    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        val symbols = provider.getCallableSymbols(CallableId(packageFqName, name))
        for (symbol in symbols) {
            if (symbol is ConeFunctionSymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }

    override fun processPropertiesByName(name: Name, processor: (ConePropertySymbol) -> ProcessorAction): ProcessorAction {
        val symbols = provider.getCallableSymbols(CallableId(packageFqName, name))
        for (symbol in symbols) {
            if (symbol is ConePropertySymbol && !processor(symbol)) {
                return STOP
            }
        }
        return NEXT
    }
}