/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirLazyValue
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.validate
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.ownerGenerator
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirGeneratedClassDeclaredMemberScope private constructor(
    val useSiteSession: FirSession,
    val firClass: FirClass,
    needNestedClassifierScope: Boolean,
    val extensionsByCallableName: Map<Name, List<FirDeclarationGenerationExtension>>,
    val allCallableNames: Set<Name>
) : FirClassDeclaredMemberScope(firClass.classId) {
    companion object {
        fun create(session: FirSession, firClass: FirClass, needNestedClassifierScope: Boolean): FirGeneratedClassDeclaredMemberScope? {
            val extensionsByCallableName = session.groupExtensionsByName(
                firClass,
                nameExtractor = { getCallableNamesForClass(it) },
                nameTransformer = { it }
            )
            val allCallableNames = extensionsByCallableName.keys
            if (allCallableNames.isEmpty()) return null
            return FirGeneratedClassDeclaredMemberScope(
                session,
                firClass,
                needNestedClassifierScope,
                extensionsByCallableName,
                allCallableNames
            )
        }
    }

    private val nestedClassifierScope: FirNestedClassifierScope? = runIf(needNestedClassifierScope) {
        useSiteSession.nestedClassifierScope(firClass)
    }

    private val firCachesFactory = useSiteSession.firCachesFactory

    // ------------------------------------------ caches ------------------------------------------

    private val functionCache: FirCache<Name, List<FirNamedFunctionSymbol>, Nothing?> = firCachesFactory.createCache { callableId, _ ->
        generateMemberFunctions(callableId)
    }

    private val propertyCache: FirCache<Name, List<FirPropertySymbol>, Nothing?> = firCachesFactory.createCache { callableId, _ ->
        generateMemberProperties(callableId)
    }

    private val constructorCache: FirLazyValue<List<FirConstructorSymbol>, Nothing?> = firCachesFactory.createLazyValue {
        generateConstructors()
    }

    // ------------------------------------------ generators ------------------------------------------

    private fun generateMemberFunctions(name: Name): List<FirNamedFunctionSymbol> {
        if (name == SpecialNames.INIT) return emptyList()
        return extensionsByCallableName[name].orEmpty()
            .flatMap { it.generateFunctions(CallableId(firClass.classId, name), firClass.symbol) }
            .onEach { it.fir.validate() }
    }

    private fun generateMemberProperties(name: Name): List<FirPropertySymbol> {
        if (name == SpecialNames.INIT) return emptyList()
        return extensionsByCallableName[name].orEmpty()
            .flatMap { it.generateProperties(CallableId(firClass.classId, name), firClass.symbol) }
            .onEach { it.fir.validate() }
    }

    private fun generateConstructors(): List<FirConstructorSymbol> {
        return extensionsByCallableName[SpecialNames.INIT].orEmpty()
            .flatMap { it.generateConstructors(firClass.symbol) }
            .onEach { it.fir.validate() }
    }

    // ------------------------------------------ scope methods ------------------------------------------

    override fun getCallableNames(): Set<Name> {
        return allCallableNames
    }

    override fun getClassifierNames(): Set<Name> {
        return nestedClassifierScope?.getClassifierNames() ?: emptySet()
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        nestedClassifierScope?.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name !in getCallableNames()) return
        for (functionSymbol in functionCache.getValue(name)) {
            processor(functionSymbol)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        if (name !in getCallableNames()) return
        for (propertySymbol in propertyCache.getValue(name)) {
            processor(propertySymbol)
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        for (constructorSymbol in constructorCache.getValue()) {
            processor(constructorSymbol)
        }
    }
}

internal inline fun <T, V> FirSession.groupExtensionsByName(
    klass: FirClass,
    nameExtractor: FirDeclarationGenerationExtension.(FirClassSymbol<*>) -> Set<T>,
    nameTransformer: (T) -> V
): Map<V, List<FirDeclarationGenerationExtension>> {
    val extensions = getExtensionsForClass(klass)
    val symbol = klass.symbol
    return extensions.flatGroupBy(
        keySelector = { extension -> extension.nameExtractor(symbol) },
        keyTransformer = nameTransformer,
        valueTransformer = { it }
    )
}

internal fun FirSession.getExtensionsForClass(klass: FirClass): List<FirDeclarationGenerationExtension> {
    val extensions = extensionService.declarationGenerators
    return if (klass.origin.generated) {
        listOf(klass.ownerGenerator!!)
    } else {
        extensions
    }
}

class FirGeneratedClassNestedClassifierScope private constructor(
    useSiteSession: FirSession,
    klass: FirClass,
    private val nestedClassifierNames: Set<Name>
) : FirNestedClassifierScope(klass, useSiteSession) {
    companion object {
        @OptIn(FirExtensionApiInternals::class)
        fun create(useSiteSession: FirSession, klass: FirClass): FirGeneratedClassNestedClassifierScope? {
            val extensions = useSiteSession.getExtensionsForClass(klass)
            val symbol = klass.symbol
            val classifierNames = extensions.flatMapTo(mutableSetOf()) { it.nestedClassifierNamesCache.getValue(symbol) }
            if (classifierNames.isEmpty()) return null
            return FirGeneratedClassNestedClassifierScope(useSiteSession, klass, classifierNames)
        }
    }

    private val nestedClassifierCache: FirCache<Name, FirRegularClassSymbol?, Nothing?> =
        useSiteSession.firCachesFactory.createCache { name, _ ->
            generateNestedClassifier(name)
        }

    private fun generateNestedClassifier(name: Name): FirRegularClassSymbol? {
        if (klass is FirRegularClass) {
            val companion = klass.companionObjectSymbol
            if (companion != null && companion.origin.generated && companion.classId.shortClassName == name) {
                return companion
            }
        }

        if (name !in nestedClassifierNames) return null
        val generatedClass = useSiteSession.generatedDeclarationsSymbolProvider
            ?.getClassLikeSymbolByClassId(klass.classId.createNestedClassId(name))
        require(generatedClass is FirRegularClassSymbol?) { "Only regular class are allowed as nested classes" }
        return generatedClass
    }

    override fun getNestedClassSymbol(name: Name): FirRegularClassSymbol? {
        return nestedClassifierCache.getValue(name)
    }

    override fun isEmpty(): Boolean {
        return nestedClassifierNames.isEmpty()
    }

    override fun getClassifierNames(): Set<Name> {
        return nestedClassifierNames
    }
}
