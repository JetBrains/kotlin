/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirSyntheticFunctionInterfaceProviderBase.Companion.mayBeSyntheticFunctionClassName
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

abstract class FirAbstractStarImportingScope(
    session: FirSession,
    scopeSession: ScopeSession,
    lookupInFir: Boolean,
    val excludedImportNames: Set<FqName>
) : FirAbstractImportingScope(session, scopeSession, lookupInFir) {

    // TODO try to hide this
    abstract val starImports: List<FirResolvedImport>

    /**
     * Only contains packages which are package-only star imports, without a parent class ID.
     */
    private val importedPackages by lazy(LazyThreadSafetyMode.PUBLICATION) {
        starImports.filter { it.resolvedParentClassId == null }.mapToSetOrEmpty { it.packageFqName }
    }

    private val starImportsWithParentClassId by lazy(LazyThreadSafetyMode.PUBLICATION) {
        starImports.filter { it.resolvedParentClassId != null }
    }

    override fun isExcluded(import: FirResolvedImport, name: Name): Boolean {
        if (excludedImportNames.isNotEmpty()) {
            return import.importedFqName!!.child(name) in excludedImportNames
        }
        return false
    }

    private val absentClassifierNames = mutableSetOf<Name>()

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        if ((!name.isSpecial && name.identifier.isEmpty()) || starImports.isEmpty() || name in absentClassifierNames) {
            return
        }

        var foundAny = false

        val isProcessedByFastPath = processClassifierByNameFastPath(name) { symbol ->
            foundAny = true
            processor(symbol, ConeSubstitutor.Empty)
        }

        if (!isProcessedByFastPath) {
            processClassifiersFromImportsByName(name, starImports) { symbol ->
                foundAny = true
                processor(symbol, ConeSubstitutor.Empty)
            }
        }

        if (!foundAny) {
            absentClassifierNames += name
        }
    }

    // TODO: Make this fast path less expensive in the compiler where `getTopLevelClassIdsByShortName` currently always returns `null`.
    //  Meaning, avoid it altogether without all these expensive checks.
    private fun processClassifierByNameFastPath(
        name: Name,
        processor: (FirClassLikeSymbol<*>) -> Unit,
    ): Boolean {
        if (name.isSpecial) return false

        // `getTopLevelClassIdsByShortName` cannot return synthetic function class names, so we should fall back to the slow path in case
        // the name may be a synthetic function class name.
        @OptIn(FirSymbolProviderInternals::class)
        if (name.mayBeSyntheticFunctionClassName()) return false

        val topLevelClassIds = provider.symbolNamesProvider.getTopLevelClassIdsByShortName(name) ?: return false

        // We don't have to iterate through all imports, but can instead just check each possible class ID against the packages imported by
        // all star imports.
        //
        // The order is not important, as multiple classifier results for a given `name` result in ambiguous candidates. Meaning we don't
        // have to follow the imports in order.
        // TODO: Add a test for this.
        //
        // TODO: Check back with the compiler team if the order of candidates really isn't important in any use case.
        for (classId in topLevelClassIds) {
            // TODO: Not optimal since we're constructing a new `FqName` here.
            if (excludedImportNames.isNotEmpty() && classId.asSingleFqName() in excludedImportNames) continue
            if (classId.packageFqName !in importedPackages) continue

            val symbol = provider.getClassLikeSymbolByClassId(classId) ?: continue
            processor(symbol)
        }

        // We also have to check imports with parent class IDs, as `topLevelClassIds` only includes top-level class IDs, and here we're
        // looking at nested imports.
        // TODO: Alternative: Also provide nested class IDs for `getTopLevelClassIdsByShortName`.
        // TODO: We can probably call `processClassifiersFromImportsByName` here.
        for (import in starImportsWithParentClassId) {
            if (isExcluded(import, name)) continue

            val classId = import.resolvedParentClassId!!.createNestedClassId(name)
            val symbol = provider.getClassLikeSymbolByClassId(classId) ?: continue
            processor(symbol)
        }

        return true
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        processFunctionsByName(name, starImports, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        processPropertiesByName(name, starImports, processor)
    }

    @FirImplementationDetail
    override fun findEnumEntryWithoutResolution(name: Name): FirEnumEntrySymbol? {
        return doFindEnumEntryWithoutResolution(name, starImports)
    }

    @DelicateScopeAPI
    abstract override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirAbstractStarImportingScope
}
