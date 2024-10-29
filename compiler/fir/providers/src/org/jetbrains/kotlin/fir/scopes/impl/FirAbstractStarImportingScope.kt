/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractStarImportingScope(
    session: FirSession,
    scopeSession: ScopeSession,
    lookupInFir: Boolean,
    val excludedImportNames: Set<FqName>
) : FirAbstractImportingScope(session, scopeSession, lookupInFir) {

    // TODO try to hide this
    abstract val starImports: List<FirResolvedImport>

    override fun isExcluded(import: FirResolvedImport, name: Name): Boolean {
        if (excludedImportNames.isNotEmpty()) {
            return import.importedFqName!!.child(name) in excludedImportNames
        }
        return false
    }

    override fun mayHaveClassName(import: FirResolvedImport, name: Name): Boolean {
        // TODO (marco): Describe classifier names check on client-side.
        // TODO (marco): Accesses for nested classes are still speculative! But we currently don't have a mechanism to deflect them.
        // It only makes sense to check the classifier name set if `name` represents a top-level class, as we can be sure that the
        // `resolvedParentClassId` is precise (unless we have red code). For example, in `import org.jetbrains.kotlin.fir.FirSession.*`,
        // we know for sure that `org.jetbrains.kotlin.fir.FirSession` exists (unless, again, we have red code, which we don't need to
        // optimize for).
        if (import.resolvedParentClassId == null) {
            // TODO (marco): Instead of doing this, we should use the normal speculative symbol provider access. Copying this kind of code
            //               to every use site is error-prone.
            val classifierNames = provider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(import.packageFqName) ?: return true
            return name in classifierNames
        }
        return true
    }

    private val absentClassifierNames = mutableSetOf<Name>()

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        if ((!name.isSpecial && name.identifier.isEmpty()) || starImports.isEmpty() || name in absentClassifierNames) {
            return
        }
        var foundAny = false
        processClassifiersFromImportsByName(name, starImports) { symbol ->
            foundAny = true
            processor(symbol, ConeSubstitutor.Empty)
        }
        if (!foundAny) {
            absentClassifierNames += name
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        processFunctionsByName(name, starImports, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        processPropertiesByName(name, starImports, processor)
    }

    @DelicateScopeAPI
    abstract override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirAbstractStarImportingScope
}
