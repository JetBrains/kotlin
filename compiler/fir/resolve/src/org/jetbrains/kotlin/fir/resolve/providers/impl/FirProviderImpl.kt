/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.*

@ThreadSafeMutableState
class FirProviderImpl(val session: FirSession, val kotlinScopeProvider: FirKotlinScopeProvider) : FirProvider() {
    override val symbolProvider: FirSymbolProvider = SymbolProvider()

    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        symbol.originalIfFakeOverride()?.let { originalSymbol ->
            return originalSymbol.moduleData.session.firProvider.getFirCallableContainerFile(originalSymbol)
        }
        if (symbol is FirBackingFieldSymbol) {
            return getFirCallableContainerFile(symbol.fir.propertySymbol)
        }
        if (symbol is FirSyntheticPropertySymbol) {
            val fir = symbol.fir
            if (fir is FirSyntheticProperty) {
                return getFirCallableContainerFile(fir.getter.delegate.symbol)
            }
        }
        return state.callableContainerMap[symbol]
    }

    override fun getFirScriptContainerFile(symbol: FirScriptSymbol): FirFile? {
        return state.scriptContainerMap[symbol]
    }

    override fun getFirScriptByFilePath(path: String): FirScriptSymbol? {
        return state.scriptByFilePathMap[path]
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
        override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
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
            if (fqName in state.allSubPackages) return fqName
            return null
        }

        override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProvider() {
            override fun getPackageNamesWithTopLevelCallables(): Set<String> =
                state.allSubPackages.mapTo(mutableSetOf()) { it.asString() }

            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String> =
                state.classifierInPackage[packageFqName].orEmpty().mapTo(mutableSetOf()) { it.asString() }

            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = buildSet {
                for (key in state.functionMap.keys) {
                    if (key.packageName == packageFqName) {
                        add(key.callableName)
                    }
                }

                for (key in state.propertyMap.keys) {
                    if (key.packageName == packageFqName) {
                        add(key.callableName)
                    }
                }
            }
        }
    }

    private fun recordFile(file: FirFile, state: State) {
        val packageName = file.packageFqName
        state.fileMap.merge(packageName, listOf(file)) { a, b -> a + b }
        generateSequence(packageName) { it.parentOrNull() }.forEach(state.allSubPackages::add)
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
                data.state.classifierInPackage.getOrPut(classId.packageFqName, ::mutableSetOf).add(classId.shortClassName)
            }

            regularClass.acceptChildren(this, data)
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: FirRecorderData) {
            val classId = typeAlias.symbol.classId
            val prevFile = data.state.classifierContainerFileMap.put(classId, data.file)
            data.state.classifierMap.put(classId, typeAlias)?.let {
                data.nameConflictsTracker?.registerClassifierRedeclaration(classId, typeAlias.symbol, data.file, it.symbol, prevFile)
            }

            data.state.classifierInPackage.getOrPut(classId.packageFqName, ::mutableSetOf).add(classId.shortClassName)
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

        override fun visitScript(script: FirScript, data: FirRecorderData) {
            val symbol = script.symbol
            data.state.scriptContainerMap[symbol] = data.file
            data.file.sourceFile?.path?.let { data.state.scriptByFilePathMap[it] = symbol }
            script.acceptChildren(this, data)
        }
    }

    private val state = State()

    private class State {
        val fileMap: MutableMap<FqName, List<FirFile>> = mutableMapOf<FqName, List<FirFile>>()
        val allSubPackages = mutableSetOf<FqName>()
        val classifierMap = mutableMapOf<ClassId, FirClassLikeDeclaration>()
        val classifierContainerFileMap = mutableMapOf<ClassId, FirFile>()
        val classifierInPackage = mutableMapOf<FqName, MutableSet<Name>>()
        val classesInPackage = mutableMapOf<FqName, MutableSet<Name>>()
        val functionMap = mutableMapOf<CallableId, List<FirNamedFunctionSymbol>>()
        val propertyMap = mutableMapOf<CallableId, List<FirPropertySymbol>>()
        val constructorMap = mutableMapOf<CallableId, List<FirConstructorSymbol>>()
        val callableContainerMap = mutableMapOf<FirCallableSymbol<*>, FirFile>()
        val scriptContainerMap = mutableMapOf<FirScriptSymbol, FirFile>()
        val scriptByFilePathMap = mutableMapOf<String, FirScriptSymbol>()

        fun setFrom(other: State) {
            fileMap.clear()
            allSubPackages.clear()
            classifierMap.clear()
            classifierContainerFileMap.clear()
            functionMap.clear()
            propertyMap.clear()
            constructorMap.clear()
            callableContainerMap.clear()
            scriptContainerMap.clear()
            scriptByFilePathMap.clear()

            fileMap.putAll(other.fileMap)
            allSubPackages.addAll(other.allSubPackages)
            classifierMap.putAll(other.classifierMap)
            classifierContainerFileMap.putAll(other.classifierContainerFileMap)
            functionMap.putAll(other.functionMap)
            propertyMap.putAll(other.propertyMap)
            constructorMap.putAll(other.constructorMap)
            callableContainerMap.putAll(other.callableContainerMap)
            scriptContainerMap.putAll(other.scriptContainerMap)
            scriptByFilePathMap.putAll(other.scriptByFilePathMap)
            classesInPackage.putAll(other.classesInPackage)
            classifierInPackage.putAll(other.classifierInPackage)
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
