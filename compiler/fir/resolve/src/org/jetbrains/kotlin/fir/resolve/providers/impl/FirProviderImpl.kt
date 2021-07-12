/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@ThreadSafeMutableState
class FirProviderImpl(val session: FirSession, val kotlinScopeProvider: FirKotlinScopeProvider) : FirProvider() {
    override val symbolProvider: FirSymbolProvider = SymbolProvider()

    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        symbol.originalIfFakeOverride()?.let {
            return getFirCallableContainerFile(it)
        }
        if (symbol is FirAccessorSymbol) {
            val fir = symbol.fir
            if (fir is FirSyntheticProperty) {
                return getFirCallableContainerFile(fir.getter.delegate.symbol)
            }
        }
        return state.callableContainerMap[symbol]
    }

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return state.classifierContainerFileMap[fqName] ?: error("Couldn't find container for $fqName")
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        return state.classifierContainerFileMap[fqName]
    }

    fun recordFile(file: FirFile) {
        recordFile(file, state)
    }

    private inner class SymbolProvider : FirSymbolProvider(session) {
        override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
            return getFirClassifierByFqName(classId)?.symbol
        }

        @FirSymbolProviderInternals
        override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
            destination += (state.functionMap[CallableId(packageFqName, null, name)] ?: emptyList())
            destination += (state.propertyMap[CallableId(packageFqName, null, name)] ?: emptyList())
        }

        @FirSymbolProviderInternals
        override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
            destination += (state.functionMap[CallableId(packageFqName, null, name)] ?: emptyList())
        }

        @FirSymbolProviderInternals
        override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
            destination += (state.propertyMap[CallableId(packageFqName, null, name)] ?: emptyList())
        }

        override fun getPackage(fqName: FqName): FqName? {
            if (getFirFilesByPackage(fqName).isNotEmpty()) return fqName
            return null
        }
    }

    @FirProviderInternals
    override fun recordGeneratedClass(owner: FirAnnotatedDeclaration, klass: FirRegularClass) {
        klass.accept(FirRecorder, FirRecorderData(state, owner.file, session.nameConflictsTracker))
    }

    @FirProviderInternals
    override fun recordGeneratedMember(owner: FirAnnotatedDeclaration, klass: FirDeclaration) {
        klass.accept(FirRecorder, FirRecorderData(state, owner.file, session.nameConflictsTracker))
    }

    private val FirAnnotatedDeclaration.file: FirFile
        get() = when (this) {
            is FirFile -> this
            is FirRegularClass -> getFirClassifierContainerFile(this.symbol.classId)
            else -> error("Should not be here")
        }

    private fun recordFile(file: FirFile, state: State) {
        val packageName = file.packageFqName
        state.fileMap.merge(packageName, listOf(file)) { a, b -> a + b }
        file.acceptChildren(FirRecorder, FirRecorderData(state, file, session.nameConflictsTracker))
    }
    
    private class FirRecorderData(
        val state: State,
        val file: FirFile,
        val nameConflictsTracker: FirNameConflictsTrackerComponent?
    )

    private object FirRecorder : FirDefaultVisitor<Unit, FirRecorderData>() {
        override fun visitElement(element: FirElement, data: FirRecorderData) {}

        override fun visitRegularClass(regularClass: FirRegularClass, data: FirRecorderData) {
            val classId = regularClass.symbol.classId
            val prevFile = data.state.classifierContainerFileMap.put(classId, data.file)
            data.state.classifierMap.put(classId, regularClass)?.let {
                data.nameConflictsTracker?.registerClassifierRedeclaration(classId, regularClass.symbol, data.file, it.symbol, prevFile)
            }

            if (!classId.isNestedClass && !classId.isLocal) {
                data.state.classesInPackage.getOrPut(classId.packageFqName, ::mutableSetOf).add(classId.shortClassName)
            }

            regularClass.acceptChildren(this, data)
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: FirRecorderData) {
            val classId = typeAlias.symbol.classId
            val prevFile = data.state.classifierContainerFileMap.put(classId, data.file)
            data.state.classifierMap.put(classId, typeAlias)?.let {
                data.nameConflictsTracker?.registerClassifierRedeclaration(classId, typeAlias.symbol, data.file, it.symbol, prevFile)
            }
        }

        override fun visitPropertyAccessor(
            propertyAccessor: FirPropertyAccessor,
            data: FirRecorderData
        ) {
            val symbol = propertyAccessor.symbol
            data.state.callableContainerMap[symbol] = data.file
        }

        private inline fun <reified D : FirCallableDeclaration, S : FirCallableSymbol<D>> registerCallable(
            symbol: S,
            data: FirRecorderData,
            map: MutableMap<CallableId, List<S>>
        ) {
            val callableId = symbol.callableId
            map.merge(callableId, listOf(symbol)) { a, b -> a + b }
            data.state.callableContainerMap[symbol] = data.file
        }

        override fun visitConstructor(constructor: FirConstructor, data: FirRecorderData) {
            val symbol = constructor.symbol
            registerCallable(symbol, data, data.state.constructorMap)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: FirRecorderData) {
            val symbol = simpleFunction.symbol
            registerCallable(symbol, data, data.state.functionMap)
        }

        override fun visitProperty(property: FirProperty, data: FirRecorderData) {
            val symbol = property.symbol
            registerCallable(symbol, data, data.state.propertyMap)
            property.getter?.let { visitPropertyAccessor(it, data) }
            property.setter?.let { visitPropertyAccessor(it, data) }
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry, data: FirRecorderData) {
            val symbol = enumEntry.symbol
            data.state.callableContainerMap[symbol] = data.file
        }
    }

    private val state = State()

    private class State {
        val fileMap = mutableMapOf<FqName, List<FirFile>>()
        val classifierMap = mutableMapOf<ClassId, FirClassLikeDeclaration>()
        val classifierContainerFileMap = mutableMapOf<ClassId, FirFile>()
        val classesInPackage = mutableMapOf<FqName, MutableSet<Name>>()
        val functionMap = mutableMapOf<CallableId, List<FirNamedFunctionSymbol>>()
        val propertyMap = mutableMapOf<CallableId, List<FirPropertySymbol>>()
        val constructorMap = mutableMapOf<CallableId, List<FirConstructorSymbol>>()
        val callableContainerMap = mutableMapOf<FirCallableSymbol<*>, FirFile>()

        fun setFrom(other: State) {
            fileMap.clear()
            classifierMap.clear()
            classifierContainerFileMap.clear()
            functionMap.clear()
            propertyMap.clear()
            constructorMap.clear()
            callableContainerMap.clear()

            fileMap.putAll(other.fileMap)
            classifierMap.putAll(other.classifierMap)
            classifierContainerFileMap.putAll(other.classifierContainerFileMap)
            functionMap.putAll(other.functionMap)
            propertyMap.putAll(other.propertyMap)
            constructorMap.putAll(other.constructorMap)
            callableContainerMap.putAll(other.callableContainerMap)
            classesInPackage.putAll(other.classesInPackage)
        }
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> {
        return state.fileMap[fqName].orEmpty()
    }

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? {
        require(!classId.isLocal) {
            "Local $classId should never be used to find its corresponding classifier"
        }
        return state.classifierMap[classId]
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
        checkMMapDiff("callableMap", state.functionMap, newState.functionMap)
        checkMMapDiff("callableMap", state.propertyMap, newState.propertyMap)
        checkMMapDiff("callableMap", state.constructorMap, newState.constructorMap)
        checkMapDiff("callableContainerMap", state.callableContainerMap, newState.callableContainerMap)

        if (!rebuildIndex) {
            assert(failures.isEmpty()) {
                failures.joinToString(separator = "\n")
            }
        } else {
            state.setFrom(newState)
        }
    }

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> {
        return state.classesInPackage[fqName] ?: emptySet()
    }
}

private const val rebuildIndex = true
