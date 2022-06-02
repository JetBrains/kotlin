/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol

class FirSpecialTowerDataContexts {
    private val towerDataContextForAnonymousFunctions: MutableMap<FirAnonymousFunctionSymbol, FirTowerDataContext> = mutableMapOf()
    private val towerDataContextForCallableReferences: MutableMap<FirCallableReferenceAccess, FirTowerDataContext> = mutableMapOf()

    fun getAnonymousFunctionContext(symbol: FirAnonymousFunctionSymbol): FirTowerDataContext? {
        return towerDataContextForAnonymousFunctions[symbol]
    }

    fun getCallableReferenceContext(access: FirCallableReferenceAccess): FirTowerDataContext? {
        return towerDataContextForCallableReferences[access]
    }

    fun storeAnonymousFunctionContext(symbol: FirAnonymousFunctionSymbol, context: FirTowerDataContext) {
        towerDataContextForAnonymousFunctions[symbol] = context
    }

    fun dropAnonymousFunctionContext(symbol: FirAnonymousFunctionSymbol) {
        towerDataContextForAnonymousFunctions.remove(symbol)
    }

    fun storeCallableReferenceContext(access: FirCallableReferenceAccess, context: FirTowerDataContext) {
        towerDataContextForCallableReferences[access] = context
    }

    fun dropCallableReferenceContext(access: FirCallableReferenceAccess) {
        towerDataContextForCallableReferences.remove(access)
    }

    fun putAll(contexts: FirSpecialTowerDataContexts) {
        towerDataContextForCallableReferences.putAll(contexts.towerDataContextForCallableReferences)
        towerDataContextForAnonymousFunctions.putAll(contexts.towerDataContextForAnonymousFunctions)
    }

    fun clear() {
        towerDataContextForAnonymousFunctions.clear()
        towerDataContextForCallableReferences.clear()
    }
}
