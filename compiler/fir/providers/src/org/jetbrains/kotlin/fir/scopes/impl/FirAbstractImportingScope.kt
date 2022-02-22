/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.moduleVisibilityChecker
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedForCalls
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

enum class FirImportingScopeFilter {
    ALL, INVISIBLE_CLASSES, MEMBERS_AND_VISIBLE_CLASSES;

    fun check(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
        if (this == ALL) return true
        // TODO: also check DeprecationLevel.HIDDEN and required Kotlin version
        val fir = symbol.fir
        val isVisible = when (fir.status.visibility) {
            // When importing from the same module, status may be unknown because the status resolver depends on super types
            // to determine visibility for functions, so it may not have finished yet. Since we only care about classes,
            // though, "unknown" will always become public anyway.
            Visibilities.Unknown -> true
            Visibilities.Internal ->
                symbol.fir.moduleData == session.moduleData || session.moduleVisibilityChecker?.isInFriendModule(fir) == true
            // All non-`internal` visibilities are either even more restrictive (e.g. `private`) or must not
            // be checked in imports (e.g. `protected` may be valid in some use sites).
            else -> !fir.status.visibility.mustCheckInImports()
        }
        return isVisible == (this == MEMBERS_AND_VISIBLE_CLASSES)
    }
}

abstract class FirAbstractImportingScope(
    session: FirSession,
    protected val scopeSession: ScopeSession,
    protected val filter: FirImportingScopeFilter,
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

    protected fun findSingleClassifierSymbolByName(name: Name?, imports: List<FirResolvedImport>): FirClassLikeSymbol<*>? {
        var result: FirClassLikeSymbol<*>? = null
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            val classId = import.resolvedParentClassId?.createNestedClassId(importedName)
                ?: ClassId.topLevel(import.packageFqName.child(importedName))
            val symbol = provider.getClassLikeSymbolByClassId(classId) ?: continue
            if (!filter.check(symbol, session)) continue
            result = when (result) {
                null, symbol -> symbol
                // TODO: if there is an ambiguity at this scope, further scopes should not be checked.
                //  Doing otherwise causes KT-39073. Also, returning null here instead of an error symbol
                //  or something produces poor quality diagnostics ("unresolved name" rather than "ambiguity").
                else -> return null
            }
        }
        return result
    }

    protected fun processFunctionsByName(name: Name?, imports: List<FirResolvedImport>, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (filter == FirImportingScopeFilter.INVISIBLE_CLASSES) return
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
        if (filter == FirImportingScopeFilter.INVISIBLE_CLASSES) return
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
