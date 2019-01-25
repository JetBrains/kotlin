/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.Name

class FirTopLevelDeclaredMemberScope(
    file: FirFile,
    session: FirSession,
    lookupInFir: Boolean = true
) : FirAbstractProviderBasedScope(session, lookupInFir) {
    private val packageFqName = file.packageFqName

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeSymbol) -> Boolean
    ): Boolean {
        val symbols = provider.getCallableSymbols(CallableId(packageFqName, name))
        for (symbol in symbols) {
            if (!processor(symbol)) {
                return false
            }
        }
        return true
    }
}