/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.Name

class FirClassDeclaredMemberScope(
    klass: FirRegularClass,
    session: FirSession,
    lookupInFir: Boolean = true
) : FirAbstractProviderBasedScope(session, lookupInFir) {
    private val classId = klass.symbol.classId

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeSymbol) -> Boolean
    ): Boolean {
        val symbols = provider.getCallableSymbols(CallableId(classId.packageFqName, classId.relativeClassName, name))
        for (symbol in symbols) {
            if (!processor(symbol)) {
                return false
            }
        }
        return true
    }
}