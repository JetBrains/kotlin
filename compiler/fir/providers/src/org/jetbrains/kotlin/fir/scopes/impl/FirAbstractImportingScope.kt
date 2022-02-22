/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedForCalls
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractImportingScope(
    session: FirSession,
    protected val scopeSession: ScopeSession,
    lookupInFir: Boolean
) : FirAbstractProviderBasedScope(session, lookupInFir) {
    private val FirClassLikeSymbol<*>.fullyExpandedSymbol: FirClassSymbol<*>?
        get() = when (this) {
            is FirTypeAliasSymbol -> fir.expandedConeType?.lookupTag?.toSymbol(session)?.fullyExpandedSymbol
            is FirClassSymbol<*> -> this
        }

    private fun FirClassSymbol<*>.getStaticsScope(): FirContainingNamesAwareScope? =
        if (fir.classKind == ClassKind.OBJECT) {
            FirObjectImportedCallableScope(
                classId, fir.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
            )
        } else {
            fir.scopeProvider.getStaticScope(fir, session, scopeSession)
        }

    fun getStaticsScope(classId: ClassId): FirContainingNamesAwareScope? =
        provider.getClassLikeSymbolByClassId(classId)?.fullyExpandedSymbol?.getStaticsScope()

    protected fun processImportsByName(
        name: Name?,
        imports: List<FirResolvedImport>,
        processor: (FirClassLikeSymbol<*>) -> Unit
    ) {
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            val classId = import.resolvedParentClassId?.createNestedClassId(importedName)
                ?: ClassId.topLevel(import.packageFqName.child(importedName))
            val symbol = provider.getClassLikeSymbolByClassId(classId) ?: continue
            processor(symbol)
        }
    }

    protected fun processFunctionsByName(name: Name?, imports: List<FirResolvedImport>, processor: (FirNamedFunctionSymbol) -> Unit) {
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            val staticsScope = import.resolvedParentClassId?.let(::getStaticsScope)
            if (staticsScope != null) {
                staticsScope.processFunctionsByName(importedName, processor)
            } else if (importedName.isSpecial || importedName.identifier.isNotEmpty()) {
                for (symbol in provider.getTopLevelFunctionSymbols(import.packageFqName, importedName)) {
                    symbol.ensureResolvedForCalls()
                    processor(symbol)
                }
            }
        }
    }

    protected fun processPropertiesByName(name: Name?, imports: List<FirResolvedImport>, processor: (FirVariableSymbol<*>) -> Unit) {
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            val staticsScope = import.resolvedParentClassId?.let(::getStaticsScope)
            if (staticsScope != null) {
                staticsScope.processPropertiesByName(importedName, processor)
            } else if (importedName.isSpecial || importedName.identifier.isNotEmpty()) {
                for (symbol in provider.getTopLevelPropertySymbols(import.packageFqName, importedName)) {
                    symbol.ensureResolvedForCalls()
                    processor(symbol)
                }
            }
        }
    }
}
