/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

abstract class FirAbstractSimpleImportingScope(
    session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractImportingScope(session, scopeSession, lookupInFir = true) {

    // TODO try to hide this
    abstract val simpleImports: Map<Name, List<FirResolvedImport>>

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        val imports = simpleImports[name] ?: return
        processImportsByName(null, imports) { symbol ->
            processor(symbol, ConeSubstitutor.Empty)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        val imports = simpleImports[name] ?: return
        processFunctionsByName(null, imports, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val imports = simpleImports[name] ?: return
        processPropertiesByName(null, imports, processor)
    }
}
