/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.buildUseSiteScope
import org.jetbrains.kotlin.fir.resolve.calls.TowerScopeLevel
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConeVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast

abstract class FirAbstractImportingScope(session: FirSession, lookupInFir: Boolean) : FirAbstractProviderBasedScope(session, lookupInFir) {


    protected val scopeCache = ScopeSession()

    fun <T : ConeCallableSymbol> processCallables(
        import: FirResolvedImport,
        name: Name,
        token: TowerScopeLevel.Token<T>,
        processor: (ConeCallableSymbol) -> ProcessorAction
    ): ProcessorAction {
        val callableId = CallableId(import.packageFqName, import.relativeClassName, name)

        val classId = import.resolvedClassId
        if (classId != null) {

            val scope =
                provider.getClassUseSiteMemberScope(classId, session, scopeCache) ?: error("No scope for $classId")

            val action = when (token) {
                TowerScopeLevel.Token.Functions -> scope.processFunctionsByName(
                    callableId.callableName,
                    processor.cast()
                )
                TowerScopeLevel.Token.Properties -> scope.processPropertiesByName(
                    callableId.callableName,
                    processor.cast()
                )
                else -> ProcessorAction.NEXT
            }
            if (action.stop()) {
                return ProcessorAction.STOP
            }
        } else {
            val matchedClass = provider.getClassLikeSymbolByFqName(ClassId(import.packageFqName, name))

            if (matchedClass != null && matchedClass is FirClassSymbol) {
                //TODO: why don't we use declared member scope at this point?
                if (matchedClass.fir.buildUseSiteScope(session, scopeCache).processFunctionsByName(
                        name,
                        processor
                    ) == ProcessorAction.STOP
                ) {
                    return ProcessorAction.STOP
                }
            }

            val symbols = provider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)

            for (symbol in symbols) {
                if (processor(symbol).stop()) {
                    return ProcessorAction.STOP
                }
            }
        }

        return ProcessorAction.NEXT
    }

    protected abstract fun <T : ConeCallableSymbol> processCallables(
        name: Name,
        token: TowerScopeLevel.Token<T>,
        processor: (ConeCallableSymbol) -> ProcessorAction
    ): ProcessorAction

    final override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        return processCallables(
            name,
            TowerScopeLevel.Token.Functions
        ) { if (it is ConeFunctionSymbol) processor(it) else ProcessorAction.NEXT }
    }

    final override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        return processCallables(
            name,
            TowerScopeLevel.Token.Properties
        ) { if (it is ConeVariableSymbol) processor(it) else ProcessorAction.NEXT }
    }

}