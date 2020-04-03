/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerScopeLevel
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractImportingScope(
    session: FirSession,
    protected val scopeSession: ScopeSession,
    lookupInFir: Boolean
) : FirAbstractProviderBasedScope(session, lookupInFir) {

    // TODO: Rewrite somehow?
    private fun getStaticsScope(classId: ClassId): FirScope? {
        val symbol = provider.getClassLikeSymbolByFqName(classId) ?: return null
        if (symbol is FirTypeAliasSymbol) {
            val expansionSymbol = symbol.fir.expandedConeType?.lookupTag?.toSymbol(session)
            if (expansionSymbol != null) {
                return getStaticsScope(expansionSymbol.classId)
            }
        } else {
            return (symbol as FirClassSymbol<*>).fir.unsubstitutedScope(session, scopeSession)
        }

        return null
    }

    protected fun <T : FirCallableSymbol<*>> processCallables(
        import: FirResolvedImport,
        name: Name,
        token: TowerScopeLevel.Token<T>,
        processor: (FirCallableSymbol<*>) -> Unit
    ) {
        val callableId = CallableId(import.packageFqName, import.relativeClassName, name)

        val classId = import.resolvedClassId
        if (classId != null) {
            val scope = getStaticsScope(classId) ?: return

            when (token) {
                TowerScopeLevel.Token.Functions -> scope.processFunctionsByName(
                    callableId.callableName,
                    processor
                )
                TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(
                    callableId.callableName,
                    processor
                )
            }
        } else if (name.isSpecial || name.identifier.isNotEmpty()) {
            val symbols = provider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
            if (symbols.isEmpty()) {
                return
            }

            for (symbol in symbols) {
                processor(symbol)
            }
        }

    }

    abstract fun <T : FirCallableSymbol<*>> processCallables(
        name: Name,
        token: TowerScopeLevel.Token<T>,
        processor: (FirCallableSymbol<*>) -> Unit
    )

    final override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        return processCallables(
            name,
            TowerScopeLevel.Token.Functions
        ) { if (it is FirFunctionSymbol<*>) processor(it) }
    }

    final override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return processCallables(
            name,
            TowerScopeLevel.Token.Properties
        ) { if (it is FirVariableSymbol<*>) processor(it) }
    }

}
