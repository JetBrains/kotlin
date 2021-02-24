/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractStarImportingScope(
    session: FirSession,
    scopeSession: ScopeSession,
    filter: FirImportingScopeFilter,
    lookupInFir: Boolean
) : FirAbstractImportingScope(session, scopeSession, filter, lookupInFir) {

    // TODO try to hide this
    abstract val starImports: List<FirResolvedImport>

    private val absentClassifierNames = mutableSetOf<Name>()

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        if ((!name.isSpecial && name.identifier.isEmpty()) || starImports.isEmpty() || name in absentClassifierNames) {
            return
        }
        val symbol = findSingleClassifierSymbolByName(name, starImports)
        if (symbol != null) {
            processor(symbol, ConeSubstitutor.Empty)
        } else {
            absentClassifierNames += name
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) =
        processFunctionsByName(name, starImports, processor)

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) =
        processPropertiesByName(name, starImports, processor)
}
