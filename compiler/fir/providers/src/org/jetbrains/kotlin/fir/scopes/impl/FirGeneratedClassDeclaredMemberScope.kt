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
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.validate
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirGeneratedClassDeclaredMemberScope(
    val useSiteSession: FirSession,
    val firClass: FirClass,
    needNestedClassifierScope: Boolean
) : FirClassDeclaredMemberScope(firClass.classId) {
    private val extensions: List<FirDeclarationGenerationExtension> = firClass.findGeneratedExtensions(useSiteSession) {
        needToGenerateAdditionalMembersInClass(it)
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

    private val callableNamesCache: FirLazyValue<Set<Name>, Nothing?> = firCachesFactory.createLazyValue {
        extensions.flatMapTo(mutableSetOf()) { it.getCallableNamesForClass(firClass.symbol) }
    }

    // ------------------------------------------ generators ------------------------------------------

    private fun generateMemberFunctions(name: Name): List<FirNamedFunctionSymbol> {
        return extensions
            .flatMap { it.generateFunctions(CallableId(firClass.classId, name), firClass.symbol) }
            .onEach { it.fir.validate() }
    }

    private fun generateMemberProperties(name: Name): List<FirPropertySymbol> {
        return extensions
            .flatMap { it.generateProperties(CallableId(firClass.classId, name), firClass.symbol) }
            .onEach { it.fir.validate() }
    }

    private fun generateConstructors(): List<FirConstructorSymbol> {
        val classId = firClass.symbol.classId
        val callableId = if (classId.isNestedClass) {
            CallableId(classId.parentClassId!!, classId.shortClassName)
        } else {
            CallableId(classId.asSingleFqName().parent(), classId.shortClassName)
        }
        return extensions.flatMap { it.generateConstructors(callableId) }.onEach { it.fir.validate() }
    }

    // ------------------------------------------ scope methods ------------------------------------------

    override fun getCallableNames(): Set<Name> {
        return callableNamesCache.getValue()
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

class FirGeneratedClassNestedClassifierScope(
    klass: FirClass,
    useSiteSession: FirSession
) : FirNestedClassifierScope(klass, useSiteSession) {
    private val extensions = klass.findGeneratedExtensions(useSiteSession) { needToGenerateNestedClassifiersInClass(it) }

    private val nestedClassifierCache: FirCache<Name, FirRegularClassSymbol?, Nothing?> =
        useSiteSession.firCachesFactory.createCache { name, _ ->
            generateNestedClassifier(name)
        }

    private val nestedClassifiersNames: FirLazyValue<Set<Name>, Nothing?> =
        useSiteSession.firCachesFactory.createLazyValue {
            extensions.flatMapTo(mutableSetOf()) { it.getNestedClassifiersNames(klass.symbol) }
        }

    private fun generateNestedClassifier(name: Name): FirRegularClassSymbol? {
        if (name !in getClassifierNames()) return null
        val generatedClass = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(klass.classId.createNestedClassId(name))
        require(generatedClass is FirRegularClassSymbol?) { "Only regular class are allowed as nested classes" }
        return generatedClass
    }

    override fun getNestedClassSymbol(name: Name): FirRegularClassSymbol? {
        return nestedClassifierCache.getValue(name)
    }

    override fun isEmpty(): Boolean {
        return getClassifierNames().isEmpty()
    }

    override fun getClassifierNames(): Set<Name> {
        return nestedClassifiersNames.getValue()
    }
}


private inline fun FirClass.findGeneratedExtensions(
    useSiteSession: FirSession,
    predicate: FirDeclarationGenerationExtension.(FirClass) -> Boolean
): List<FirDeclarationGenerationExtension> {
    val origin = origin
    val declarationGenerators = useSiteSession.extensionService.declarationGenerators
    return if (origin is FirDeclarationOrigin.Plugin) {
        declarationGenerators.filter { it.key == origin.key }.also {
            require(it.isNotEmpty()) { "Extension for ${origin.key} not found" }
        }
    } else {
        declarationGenerators.filter { it.predicate(this) }
    }
}
