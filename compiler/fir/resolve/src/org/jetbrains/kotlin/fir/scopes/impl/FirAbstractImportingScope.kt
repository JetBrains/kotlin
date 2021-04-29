/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.moduleVisibilityChecker
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedForCalls
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.JVM_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.KOTLIN_NATIVE_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.KOTLIN_THROWS_ANNOTATION_FQ_NAME

enum class FirImportingScopeFilter {
    ALL, INVISIBLE_CLASSES, MEMBERS_AND_VISIBLE_CLASSES;

    fun check(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
        if (this == ALL) return true
        // TODO: also check DeprecationLevel.HIDDEN and required Kotlin version
        val fir = symbol.fir as? FirMemberDeclaration ?: return false
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

    private fun FirClassSymbol<*>.getStaticsScope(): FirScope? =
        if (fir.classKind == ClassKind.OBJECT) {
            FirObjectImportedCallableScope(
                classId, fir.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
            )
        } else {
            fir.scopeProvider.getStaticScope(fir, session, scopeSession)
        }

    fun getStaticsScope(classId: ClassId): FirScope? =
        provider.getClassLikeSymbolByFqName(classId)?.fullyExpandedSymbol?.getStaticsScope()

    protected fun findSingleClassifierSymbolByName(name: Name?, imports: List<FirResolvedImport>): FirClassLikeSymbol<*>? {
        var result: FirClassLikeSymbol<*>? = null
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            val classId = import.resolvedClassId?.createNestedClassId(importedName)
                ?: ClassId.topLevel(import.packageFqName.child(importedName))
            val symbol = provider.getClassLikeSymbolByFqName(classId) ?: continue
            if (!filter.check(symbol, session)) continue
            result = when {
                result == null || result == symbol -> symbol
                // Importing multiple versions of the same type is normally an ambiguity, but in the case of `kotlin.Throws`,
                // it should take precedence over platform-specific variants. This is lifted directly from `LazyImportScope`
                // from the old backend; most likely `Throws` predates expect-actual, and this is a backwards compatibility hack.
                // TODO: remove redundant versions of `Throws` from the standard library
                result.classId.isJvmOrNativeThrows && symbol.classId.isCommonThrows -> symbol
                result.classId.isCommonThrows && symbol.classId.isJvmOrNativeThrows -> result
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
            val staticsScope = import.resolvedClassId?.let(::getStaticsScope)
            if (staticsScope != null) {
                staticsScope.processFunctionsByName(importedName, processor)
            } else if (importedName.isSpecial || importedName.identifier.isNotEmpty()) {
                for (symbol in provider.getTopLevelFunctionSymbols(import.packageFqName, importedName)) {
                    symbol.ensureResolvedForCalls(session)
                    processor(symbol)
                }
            }
        }
    }

    protected fun processPropertiesByName(name: Name?, imports: List<FirResolvedImport>, processor: (FirVariableSymbol<*>) -> Unit) {
        if (filter == FirImportingScopeFilter.INVISIBLE_CLASSES) return
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            val staticsScope = import.resolvedClassId?.let(::getStaticsScope)
            if (staticsScope != null) {
                staticsScope.processPropertiesByName(importedName, processor)
            } else if (importedName.isSpecial || importedName.identifier.isNotEmpty()) {
                for (symbol in provider.getTopLevelPropertySymbols(import.packageFqName, importedName)) {
                    symbol.ensureResolvedForCalls(session)
                    processor(symbol)
                }
            }
        }
    }
}

private val ClassId.isJvmOrNativeThrows: Boolean
    get() = asSingleFqName().let { it == JVM_THROWS_ANNOTATION_FQ_NAME || it == KOTLIN_NATIVE_THROWS_ANNOTATION_FQ_NAME }

private val ClassId.isCommonThrows: Boolean
    get() = asSingleFqName() == KOTLIN_THROWS_ANNOTATION_FQ_NAME
