/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.validate
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.groupExtensionsByName
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy

@OptIn(FirExtensionApiInternals::class)
class FirExtensionDeclarationsSymbolProvider private constructor(
    session: FirSession,
    cachesFactory: FirCachesFactory,
    private val extensions: List<FirDeclarationGenerationExtension>
) : FirSymbolProvider(session), FirSessionComponent {
    companion object {
        fun create(session: FirSession): FirExtensionDeclarationsSymbolProvider? {
            val extensions = session.extensionService.declarationGenerators
            if (extensions.isEmpty()) return null
            return FirExtensionDeclarationsSymbolProvider(session, session.firCachesFactory, extensions)
        }
    }

    // ------------------------------------------ caches ------------------------------------------

    private val classCache: FirCache<ClassId, FirClassLikeSymbol<*>?, Nothing?> = cachesFactory.createCache { classId, _ ->
        generateClassLikeDeclaration(classId)
    }

    private val functionCache: FirCache<CallableId, List<FirNamedFunctionSymbol>, Nothing?> = cachesFactory.createCache { callableId, _ ->
        generateTopLevelFunctions(callableId)
    }

    private val propertyCache: FirCache<CallableId, List<FirPropertySymbol>, Nothing?> = cachesFactory.createCache { callableId, _ ->
        generateTopLevelProperties(callableId)
    }

    private val packageCache: FirCache<FqName, Boolean, Nothing?> = cachesFactory.createCache { packageFqName, _ ->
        hasPackage(packageFqName)
    }

    private val extensionsByTopLevelClassId: FirLazyValue<Map<ClassId, List<FirDeclarationGenerationExtension>>, Nothing?> =
        session.firCachesFactory.createLazyValue {
            extensions.flatGroupBy { it.topLevelClassIdsCache.getValue() }
        }

    private val extensionsByNestedClassifierClassId: FirCache<ClassId, Map<ClassId, List<FirDeclarationGenerationExtension>>, Nothing?> =
        session.firCachesFactory.createCache cache@{ outerClassId, _ ->
            val outerClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId) as? FirClassSymbol<*>
                ?: return@cache emptyMap()
            session.groupExtensionsByName(
                outerClassSymbol.fir,
                nameExtractor = { nestedClassifierNamesCache.getValue(outerClassSymbol) },
                nameTransformer = { outerClassId.createNestedClassId(it) }
            )
        }

    private val extensionsByTopLevelCallableId: FirLazyValue<Map<CallableId, List<FirDeclarationGenerationExtension>>, Nothing?> =
        session.firCachesFactory.createLazyValue {
            extensions.flatGroupBy { it.topLevelCallableIdsCache.getValue() }
        }

    // ------------------------------------------ generators ------------------------------------------

    private fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        val matchedExtensions = when {
            classId.isNestedClass -> extensionsByNestedClassifierClassId.getValue(classId.outerClassId!!)[classId]
            else -> extensionsByTopLevelClassId.getValue()[classId]
        } ?: return null
        val generatedClasses = matchedExtensions
            .mapNotNull { it.generateClassLikeDeclaration(classId) }
            .onEach { it.fir.validate() }
        return when (generatedClasses.size) {
            0 -> null
            1 -> generatedClasses.first()
            else -> error("Multiple plugins generated classes with same classId $classId\n${generatedClasses.joinToString("\n") { it.fir.render() }}")
        }
    }

    private fun generateTopLevelFunctions(callableId: CallableId): List<FirNamedFunctionSymbol> {
        return extensionsByTopLevelCallableId.getValue()[callableId].orEmpty()
            .flatMap { it.generateFunctions(callableId, owner = null) }
            .onEach { it.fir.validate() }
    }

    private fun generateTopLevelProperties(callableId: CallableId): List<FirPropertySymbol> {
        return extensionsByTopLevelCallableId.getValue()[callableId].orEmpty()
            .flatMap { it.generateProperties(callableId, owner = null) }
            .onEach { it.fir.validate() }
    }

    private fun hasPackage(packageFqName: FqName): Boolean {
        return extensions.any { it.hasPackage(packageFqName) }
    }

    // ------------------------------------------ provider methods ------------------------------------------

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return classCache.getValue(classId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val callableId = CallableId(packageFqName, name)
        destination += functionCache.getValue(callableId)
        destination += propertyCache.getValue(callableId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += functionCache.getValue(CallableId(packageFqName, name))
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += propertyCache.getValue(CallableId(packageFqName, name))
    }

    override fun getPackage(fqName: FqName): FqName? {
        return fqName.takeIf { packageCache.getValue(fqName, null) }
    }
}
