/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getCallableSymbols
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.ClassId
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
        val imports = simpleImports[name] ?: return ProcessorAction.NEXT
        if (imports.isEmpty()) return ProcessorAction.NEXT
        val provider = FirSymbolProvider.getInstance(session)
        for (import in imports) {
            val importedName = import.importedName ?: continue
            val callableId = CallableId(
                import.packageFqName,
                import.relativeClassName,
                importedName
            )

            for (symbol in provider.getCallableSymbols(callableId).filterIsInstance<T>()) {
                if (!processor(symbol)) {
                    return ProcessorAction.NEXT
                }
            }
        }
        return ProcessorAction.NEXT
    }
}
