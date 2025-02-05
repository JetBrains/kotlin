/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildFieldCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractImportingScope(
    session: FirSession,
    protected val scopeSession: ScopeSession,
    lookupInFir: Boolean
) : FirAbstractProviderBasedScope(session, lookupInFir) {
    private fun FirClassSymbol<*>.getStaticsScope(): FirContainingNamesAwareScope? =
        if (fir.classKind == ClassKind.OBJECT) {
            unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = FirResolvePhase.STATUS)
        } else {
            fir.scopeProvider.getStaticScope(fir, session, scopeSession)
        }

    fun getStaticsScope(classId: ClassId): FirContainingNamesAwareScope? =
        provider.getClassLikeSymbolByClassId(classId)?.fullyExpandedClass(session)?.getStaticsScope()

    protected abstract fun isExcluded(import: FirResolvedImport, name: Name): Boolean

    protected fun processClassifiersFromImportsByName(
        name: Name?,
        imports: List<FirResolvedImport>,
        processor: (FirClassLikeSymbol<*>) -> Unit
    ) {
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            if (isExcluded(import, importedName)) continue
            val classId = import.resolvedParentClassId?.createNestedClassId(importedName)
                ?: ClassId.topLevel(import.packageFqName.child(importedName))
            val symbol = provider.getClassLikeSymbolByClassId(classId) ?: continue
            processor(symbol)
        }
    }

    private inline fun <D : FirCallableDeclaration, S : FirCallableSymbol<D>> processCallablesFromImportsByName(
        name: Name?,
        imports: List<FirResolvedImport>,
        crossinline processor: (S) -> Unit,
        crossinline buildImportedCopy: S.(ClassId) -> S,
        processCallablesByName: FirContainingNamesAwareScope.(Name, (S) -> Unit) -> Unit,
        getTopLevelCallableSymbols: (FqName, Name) -> List<S>
    ) {
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            if (isExcluded(import, importedName)) continue
            val parentClassId = import.resolvedParentClassId
            if (parentClassId != null) {
                val staticsScopeOwnerSymbol = provider.getClassLikeSymbolByClassId(parentClassId)?.fullyExpandedClass(session)
                val staticsScope = staticsScopeOwnerSymbol?.getStaticsScope()
                if (staticsScope != null) {
                    staticsScope.processCallablesByName(importedName) {
                        if (it.isStatic || staticsScopeOwnerSymbol.classKind == ClassKind.OBJECT) {
                            processor(it.buildImportedCopy(staticsScopeOwnerSymbol.classId))
                        } else {
                            processor(it)
                        }
                    }
                    continue
                }
            }
            if (importedName.isSpecial || importedName.identifier.isNotEmpty()) {
                for (symbol in getTopLevelCallableSymbols(import.packageFqName, importedName)) {
                    processor(symbol)
                }
            }
        }
    }

    private inline fun <D : FirCallableDeclaration, S : FirCallableSymbol<D>> processCallablesFromImportsByNameToList(
        name: Name?,
        imports: List<FirResolvedImport>,
        out: MutableList<S>,
        crossinline buildImportedCopy: S.(ClassId) -> S,
        processCallablesByName: FirContainingNamesAwareScope.(Name, MutableList<S>) -> Unit,
        getTopLevelCallableSymbols: (FqName, Name) -> List<S>
    ) {
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            if (isExcluded(import, importedName)) continue
            val parentClassId = import.resolvedParentClassId
            if (parentClassId != null) {
                val staticsScopeOwnerSymbol = provider.getClassLikeSymbolByClassId(parentClassId)?.fullyExpandedClass(session)
                val staticsScope = staticsScopeOwnerSymbol?.getStaticsScope()
                if (staticsScope != null) {
                    val tempList = mutableListOf<S>()
                    staticsScope.processCallablesByName(importedName, tempList)
                    for (it in tempList) {
                        if (it.isStatic || staticsScopeOwnerSymbol.classKind == ClassKind.OBJECT) {
                            out.add(it.buildImportedCopy(staticsScopeOwnerSymbol.classId))
                        } else {
                            out.add(it)
                        }
                    }
                    continue
                }
            }
            if (importedName.isSpecial || importedName.identifier.isNotEmpty()) {
                out.addAll(getTopLevelCallableSymbols(import.packageFqName, importedName))
            }
        }
    }

    protected fun processFunctionsByName(name: Name?, imports: List<FirResolvedImport>, out: MutableList<FirNamedFunctionSymbol>) {
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            if (isExcluded(import, importedName)) continue
            val parentClassId = import.resolvedParentClassId
            if (parentClassId != null) {
                val staticsScopeOwnerSymbol = provider.getClassLikeSymbolByClassId(parentClassId)?.fullyExpandedClass(session)
                val staticsScope = staticsScopeOwnerSymbol?.getStaticsScope()
                if (staticsScope != null) {
                    val tempList = mutableListOf<FirNamedFunctionSymbol>()
                    staticsScope.processFunctionsByName(importedName, tempList)
                    for (it in tempList) {
                        if (it.isStatic || staticsScopeOwnerSymbol.classKind == ClassKind.OBJECT) {
                            out.add(it.fir.buildImportedVersion(staticsScopeOwnerSymbol.classId).symbol)
                        } else {
                            out.add(it)
                        }
                    }
                    continue
                }
            }
            if (importedName.isSpecial || importedName.identifier.isNotEmpty()) {
                out.addAll(provider.getTopLevelFunctionSymbols(import.packageFqName, importedName))
            }
        }
    }

    protected fun processFunctionsByName(name: Name?, imports: List<FirResolvedImport>, processor: (FirNamedFunctionSymbol) -> Unit) {
        val result = mutableListOf<FirNamedFunctionSymbol>()
        processFunctionsByName(name, imports, result)
        result.forEach(processor)
    }

    protected fun processPropertiesByName(name: Name?, imports: List<FirResolvedImport>, processor: (FirVariableSymbol<*>) -> Unit) {
        for (import in imports) {
            val importedName = name ?: import.importedName ?: continue
            if (isExcluded(import, importedName)) continue
            val parentClassId = import.resolvedParentClassId
            if (parentClassId != null) {
                val staticsScopeOwnerSymbol = provider.getClassLikeSymbolByClassId(parentClassId)?.fullyExpandedClass(session)
                val staticsScope = staticsScopeOwnerSymbol?.getStaticsScope()
                if (staticsScope != null) {
                    staticsScope.processPropertiesByName(importedName) {
                        if (it.isStatic || staticsScopeOwnerSymbol.classKind == ClassKind.OBJECT) {
                            processor(it.fir.buildImportedVersion(staticsScopeOwnerSymbol.classId).symbol)
                        } else {
                            processor(it)
                        }
                    }
                    continue
                }
            }
            if (importedName.isSpecial || importedName.identifier.isNotEmpty()) {
                provider.getTopLevelPropertySymbols(import.packageFqName, importedName).forEach(processor)
            }
        }
    }

    @DelicateScopeAPI
    abstract override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirAbstractImportingScope
}

internal fun FirSimpleFunction.buildImportedVersion(importedClassId: ClassId): FirSimpleFunction {
    return buildSimpleFunctionCopy(this) {
        origin = FirDeclarationOrigin.ImportedFromObjectOrStatic
        this.symbol = FirNamedFunctionSymbol(CallableId(importedClassId, name))
    }.apply {
        importedFromObjectOrStaticData = ImportedFromObjectOrStaticData(importedClassId, this@buildImportedVersion)
    }
}

internal fun FirVariable.buildImportedVersion(importedClassId: ClassId): FirVariable {
    return when (this) {
        is FirProperty -> {
            buildPropertyCopy(this) {
                origin = FirDeclarationOrigin.ImportedFromObjectOrStatic
                this.symbol = FirPropertySymbol(CallableId(importedClassId, name))
                this.delegateFieldSymbol = null
            }.apply {
                importedFromObjectOrStaticData = ImportedFromObjectOrStaticData(importedClassId, this@buildImportedVersion)
            }
        }
        is FirField -> {
            buildFieldCopy(this) {
                origin = FirDeclarationOrigin.ImportedFromObjectOrStatic
                this.symbol = FirFieldSymbol(CallableId(importedClassId, name))
            }.apply {
                importedFromObjectOrStaticData = ImportedFromObjectOrStaticData(importedClassId, this@buildImportedVersion)
            }
        }
        is FirEnumEntry -> {
            // It's not important to create an imported copy of FirEnumEntry
            this
        }
        else -> {
            throw IllegalStateException("Unexpected variable in buildImportedCopy: ${render()} of type ${this::class.java}")
        }
    }
}

private object ImportedFromObjectOrStaticClassIdKey : FirDeclarationDataKey()

class ImportedFromObjectOrStaticData<D : FirCallableDeclaration>(
    val objectClassId: ClassId,
    val original: D,
)

var <D : FirCallableDeclaration> D.importedFromObjectOrStaticData: ImportedFromObjectOrStaticData<D>?
        by FirDeclarationDataRegistry.data(ImportedFromObjectOrStaticClassIdKey)
