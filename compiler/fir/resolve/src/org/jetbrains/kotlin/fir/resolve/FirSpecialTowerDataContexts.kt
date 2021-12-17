/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol

class FirSpecialTowerDataContexts {
    val towerDataContextForAnonymousFunctions: MutableMap<FirAnonymousFunctionSymbol, FirTowerDataContext> = mutableMapOf()
    val towerDataContextForCallableReferences: MutableMap<FirCallableReferenceAccess, FirTowerDataContext> = mutableMapOf()
    var currentContext: FirTowerDataContext? = null

    fun setAnonymousFunctionContextIfAny(symbol: FirAnonymousFunctionSymbol): Boolean {
        val context = towerDataContextForAnonymousFunctions[symbol]
        if (context != null) {
            currentContext = context
            return true
        }
        return false
    }

    fun setCallableReferenceContextIfAny(access: FirCallableReferenceAccess): Boolean {
        val context = towerDataContextForCallableReferences[access]
        if (context != null) {
            currentContext = context
            return true
        }
        return false
    }
}