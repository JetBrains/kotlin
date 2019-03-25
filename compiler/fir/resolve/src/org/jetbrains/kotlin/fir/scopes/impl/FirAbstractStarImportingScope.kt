/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractStarImportingScope(
    session: FirSession, lookupInFir: Boolean = true
) : FirAbstractProviderBasedScope(session, lookupInFir) {

    protected abstract val starImports: List<FirResolvedImport>

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeClassifierSymbol) -> Boolean
    ): Boolean {
        for (import in starImports) {
            val relativeClassName = import.relativeClassName
            val classId = if (relativeClassName == null) {
                ClassId(import.packageFqName, name)
            } else {
                ClassId(import.packageFqName, relativeClassName.child(name), false)
            }
            val symbol = provider.getClassLikeSymbolByFqName(classId) ?: continue
            if (!processor(symbol)) {
                return false
            }
        }
        return true
    }

    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        return processCallables(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        return processCallables(name, processor)
    }

    private inline fun <reified T : ConeCallableSymbol> processCallables(
        name: Name,
        processor: (T) -> ProcessorAction
    ): ProcessorAction {
        for (import in starImports) {
            val callableId = CallableId(import.packageFqName, import.relativeClassName, name)
            val symbols = provider.getCallableSymbols(callableId).filterIsInstance<T>()

            for (symbol in symbols) {
                if (processor(symbol) == ProcessorAction.STOP) {
                    return ProcessorAction.STOP
                }
            }

        }
        return ProcessorAction.NEXT
    }
}
