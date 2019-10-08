/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirSelfImportingScope(val fqName: FqName, val session: FirSession) : FirScope() {

    private val symbolProvider = FirSymbolProvider.getInstance(session)

    private val cache = mutableMapOf<Name, FirClassifierSymbol<*>?>()

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (FirClassifierSymbol<*>) -> Boolean
    ): Boolean {
        if (name.asString().isEmpty()) return true


        val symbol = cache.getOrPut(name) {
            val unambiguousFqName = ClassId(fqName, name)
            symbolProvider.getClassLikeSymbolByFqName(unambiguousFqName)
        }

        return if (symbol != null) {
            processor(symbol)
        } else {
            true
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val symbols = symbolProvider.getTopLevelCallableSymbols(fqName, name)
        for (symbol in symbols) {
            if (symbol is FirFunctionSymbol<*> && !processor(symbol)) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val symbols = symbolProvider.getTopLevelCallableSymbols(fqName, name)
        for (symbol in symbols) {
            if (symbol is FirPropertySymbol && !processor(symbol)) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }
}
