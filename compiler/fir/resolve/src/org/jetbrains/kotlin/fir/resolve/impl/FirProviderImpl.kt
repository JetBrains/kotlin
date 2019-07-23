/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.buildDefaultUseSiteScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirProviderImpl(val session: FirSession) : FirProvider() {
    override fun getFirCallableContainerFile(symbol: ConeCallableSymbol): FirFile? {
        return state.callableContainerMap[symbol]
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return getFirClassifierByFqName(classId)?.symbol
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return (state.callableMap[CallableId(packageFqName, null, name)] ?: emptyList())
    }

    override fun getClassDeclaredMemberScope(classId: ClassId) =
        (getFirClassifierByFqName(classId) as? FirRegularClass)?.let(::FirClassDeclaredMemberScope)

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return state.classifierContainerFileMap[fqName] ?: error("Couldn't find container for $fqName")
    }

    fun recordFile(file: FirFile) {
        recordFile(file, state)
    }

    private fun recordFile(file: FirFile, state: State) {
        val packageName = file.packageFqName
        state.fileMap.merge(packageName, listOf(file)) { a, b -> a + b }

        file.acceptChildren(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {}


            override fun visitRegularClass(regularClass: FirRegularClass) {
                val classId = regularClass.symbol.classId

                state.classifierMap[classId] = regularClass
                state.classifierContainerFileMap[classId] = file

                regularClass.acceptChildren(this)
            }

            override fun visitTypeAlias(typeAlias: FirTypeAlias) {
                val classId = typeAlias.symbol.classId
                state.classifierMap[classId] = typeAlias
                state.classifierContainerFileMap[classId] = file
            }

            override fun <F : FirCallableMemberDeclaration<F>> visitCallableMemberDeclaration(
                callableMemberDeclaration: FirCallableMemberDeclaration<F>
            ) {
                val symbol = callableMemberDeclaration.symbol
                val callableId = symbol.callableId
                state.callableMap.merge(callableId, listOf(symbol)) { a, b -> a + b }
                state.callableContainerMap[symbol] = file
            }

            override fun visitConstructor(constructor: FirConstructor) {
                visitCallableMemberDeclaration(constructor)
            }

            override fun visitNamedFunction(namedFunction: FirNamedFunction) {
                visitCallableMemberDeclaration(namedFunction)
            }

            override fun visitProperty(property: FirProperty) {
                visitCallableMemberDeclaration(property)
            }
        })
    }

    private val state = State()

    private class State {
        val fileMap = mutableMapOf<FqName, List<FirFile>>()
        val classifierMap = mutableMapOf<ClassId, FirClassLikeDeclaration<*>>()
        val classifierContainerFileMap = mutableMapOf<ClassId, FirFile>()
        val callableMap = mutableMapOf<CallableId, List<FirCallableSymbol<*>>>()
        val callableContainerMap = mutableMapOf<ConeCallableSymbol, FirFile>()

        fun setFrom(other: State) {
            fileMap.clear()
            classifierMap.clear()
            classifierContainerFileMap.clear()
            callableMap.clear()
            callableContainerMap.clear()

            fileMap.putAll(other.fileMap)
            classifierMap.putAll(other.classifierMap)
            classifierContainerFileMap.putAll(other.classifierContainerFileMap)
            callableMap.putAll(other.callableMap)
            callableContainerMap.putAll(other.callableContainerMap)
        }
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        return state.fileMap[fqName].orEmpty()
    }

    override fun getFirClassifierByFqName(fqName: ClassId): FirClassLikeDeclaration<*>? {
        return state.classifierMap[fqName]
    }

    @TestOnly
    fun ensureConsistent(files: List<FirFile>) {
        val newState = State()
        files.forEach { recordFile(it, newState) }

        val failures = mutableListOf<String>()

        fun <K, V> checkMapDiff(
            title: String,
            a: Map<K, V>,
            b: Map<K, V>,
            equal: (old: V?, new: V?) -> Boolean = { old, new -> old === new }
        ) {
            var hasTitle = false
            val unionKeys = a.keys + b.keys

            for ((key, aValue, bValue) in unionKeys.map { Triple(it, a[it], b[it]) }) {
                if (!equal(aValue, bValue)) {
                    if (!hasTitle) {
                        failures += title
                        hasTitle = true
                    }
                    failures += "diff at key = '$key': was: '$aValue', become: '$bValue'"
                }
            }
        }

        fun <K, V> checkMMapDiff(title: String, a: Map<K, List<V>>, b: Map<K, List<V>>) {
            var hasTitle = false
            val unionKeys = a.keys + b.keys
            for ((key, aValue, bValue) in unionKeys.map { Triple(it, a[it], b[it]) }) {
                if (aValue == null || bValue == null) {
                    if (!hasTitle) {
                        failures += title
                        hasTitle = true
                    }
                    failures += "diff at key = '$key': was: $aValue, become: $bValue"
                } else {
                    val aSet = aValue.toSet()
                    val bSet = bValue.toSet()

                    val aLost = aSet - bSet
                    val bNew = bSet - aSet
                    if (aLost.isNotEmpty() || bNew.isNotEmpty()) {
                        failures += "diff at key = '$key':"
                        failures += "    Lost:"
                        aLost.forEach { failures += "     $it" }
                        failures += "    New:"
                        bNew.forEach { failures += "     $it" }
                    }
                }
            }

        }

        checkMMapDiff("fileMap", state.fileMap, newState.fileMap)
        checkMapDiff("classifierMap", state.classifierMap, newState.classifierMap)
        checkMapDiff("classifierContainerFileMap", state.classifierContainerFileMap, newState.classifierContainerFileMap)
        checkMMapDiff("callableMap", state.callableMap, newState.callableMap)
        checkMapDiff("callableContainerMap", state.callableContainerMap, newState.callableContainerMap)

        if (!rebuildIndex) {
            assert(failures.isEmpty()) {
                failures.joinToString(separator = "\n")
            }
        } else {
            state.setFrom(newState)
        }
    }

    override fun getClassUseSiteMemberScope(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        return when (val symbol = this.getClassLikeSymbolByFqName(classId) ?: return null) {
            is FirClassSymbol -> symbol.fir.buildDefaultUseSiteScope(useSiteSession, scopeSession)
            is FirTypeAliasSymbol -> {
                val expandedTypeRef = symbol.fir.expandedTypeRef as FirResolvedTypeRef
                val expandedType = expandedTypeRef.type as? ConeLookupTagBasedType ?: return null
                val lookupTag = expandedType.lookupTag as? ConeClassLikeLookupTag ?: return null
                getClassUseSiteMemberScope(lookupTag.classId, useSiteSession, scopeSession)
            }
            else -> throw IllegalArgumentException("Unexpected FIR symbol in getClassUseSiteMemberScope: $symbol")
        }
    }

}

private const val rebuildIndex = true
