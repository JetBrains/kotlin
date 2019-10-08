/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.TowerScopeLevel
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractSimpleImportingScope(
    session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractImportingScope(session, scopeSession, lookupInFir = true) {

    protected abstract val simpleImports: Map<Name, List<FirResolvedImportImpl>>

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (FirClassifierSymbol<*>) -> Boolean
    ): Boolean {
        val imports = simpleImports[name] ?: return true
        if (imports.isEmpty()) return true
        val provider = FirSymbolProvider.getInstance(session)
        for (import in imports) {
            val importedName = import.importedName ?: continue
            val classId =
                import.resolvedClassId?.createNestedClassId(importedName)
                    ?: ClassId.topLevel(import.packageFqName.child(importedName))
            val symbol = provider.getClassLikeSymbolByFqName(classId) ?: continue
            if (!processor(symbol)) {
                return false
            }
        }
        return true
    }

    override fun <T : FirCallableSymbol<*>> processCallables(
        name: Name,
        token: TowerScopeLevel.Token<T>,
        processor: (FirCallableSymbol<*>) -> ProcessorAction
    ): ProcessorAction {
        val imports = simpleImports[name] ?: return ProcessorAction.NONE
        if (imports.isEmpty()) return ProcessorAction.NONE

        for (import in imports) {
            if (processCallables(import, import.importedName!!, token, processor).stop()) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }
}
