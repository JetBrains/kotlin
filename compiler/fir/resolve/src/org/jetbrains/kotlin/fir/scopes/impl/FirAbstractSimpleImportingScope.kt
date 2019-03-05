/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractSimpleImportingScope(val session: FirSession) : FirScope {

    protected abstract val simpleImports: Map<Name, List<FirResolvedImportImpl>>

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeClassifierSymbol) -> Boolean
    ): Boolean {
        val imports = simpleImports[name] ?: return true
        if (imports.isEmpty()) return true
        val provider = FirSymbolProvider.getInstance(session)
        for (import in imports) {
            val symbol = provider.getClassLikeSymbolByFqName(import.resolvedFqName) ?: continue
            if (!processor(symbol)) {
                return false
            }
        }
        return true
    }

}
