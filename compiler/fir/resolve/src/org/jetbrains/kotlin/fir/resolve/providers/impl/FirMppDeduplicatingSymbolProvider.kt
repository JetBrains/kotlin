/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeEquivalentCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * This is the special symbol provider for support of the hierarchical MPP compilation scheme.
 * In this scheme, each module in the HMPP hierarchy depends on its own dependencies: metadata klibs for common modules,
 * platform jars/klibs for the leaf platform module.
 *
 * Platform libraries contain all declarations from all hmpp modules/sourcests, and metadata klibs contain only declarations
 * which were declared exactly in the corresponding sourceset. Because of that, there might be a situation where FIR for the same
 * declaration is deserialized several times (once for common, once for platform).
 * Here is the example of such a situation (without deduplicating provider)
 *
 * ```
 *   // MODULE: lib-common // sources
 *   fun foo() {}
 *   class A
 *
 *   // MODULE: lib-platform()()(lib-common) // sources
 *   fun bar()
 *
 *   // LIB: lib-common.metadata.klib
 *   fun foo() {} // (foo.1)
 *   class A  // (A.1)
 *
 *   // LIB: lib-platform.jar
 *   fun foo() {} // (foo.2)
 *   fun bar() {} // (bar.2)
 *   class A  // (A.2)
 *
 *   // MODULE: app-common(lib-common)
 *   fun test_common() {
 *       foo() // resolved to (foo.1)
 *       A() // resolved to (A.1)
 *   }
 *
 *   // MODULE: app-platform(lib-platform)()(app-common)
 *   fun test_platform() {
 *       foo() // resolved to (foo.2)
 *       bar() // resolved to (bar.2)
 *       A() // resolved to (A.2)
 *   }
 * ```
 *
 * As it showed in the example, there are two symbols for `class A` and for `fun foo`, but they actually represent the same declaration
 * (at the backend and runtime there will be only declarations from the platform jar/klib). A lot of different places in the frontend
 * expect that there will be only one fir declaration for each real declaration, so [FirMppDeduplicatingSymbolProvider] is supposed to
 * keep this invariant. It contains [commonSymbolProvider] (dependency provider of the common module) and
 * [platformSymbolProvider] (dependency provider of the platform module), so on each lookup request it checks both providers and
 * both of them returned some symbols, it tries to filter out symbols from the platform dependencies, if they are actually equal
 * to something returned from the common dependencies.
 *
 * All mapped declarations are stored in [commonCallableToPlatformCallableMap] and [classMapping] so later they could be accessed
 * from the IR actualizer to remap references to common declarations with references to platform declarations.
 */
@OptIn(SymbolInternals::class)
class FirMppDeduplicatingSymbolProvider(
    session: FirSession,
    val commonSymbolProvider: FirSymbolProvider,
    val platformSymbolProvider: FirSymbolProvider,
) : FirSymbolProvider(session) {
    private val providers: List<FirSymbolProvider> = listOf(commonSymbolProvider, platformSymbolProvider)

    data class ClassPair(val commonClass: FirClassLikeSymbol<*>, val platformClass: FirClassLikeSymbol<*>)

    val classMapping: Map<ClassId, ClassPair> get() = _classMapping
    private val _classMapping: MutableMap<ClassId, ClassPair> = mutableMapOf()

    private val processedCallables: MutableMap<CallableId, List<FirCallableSymbol<*>>> = mutableMapOf()

    val commonCallableToPlatformCallableMap: Map<FirCallableSymbol<*>, FirCallableSymbol<*>> get() = _commonCallableToPlatformCallableMap
    private val _commonCallableToPlatformCallableMap: MutableMap<FirCallableSymbol<*>, FirCallableSymbol<*>> = mutableMapOf()
    private val platformCallableToCommonCallableMap: MutableMap<FirCallableSymbol<*>, FirCallableSymbol<*>> = mutableMapOf()

    override val symbolNamesProvider: FirSymbolNamesProvider = FirCompositeSymbolNamesProvider.Companion.fromSymbolProviders(providers)

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val commonSymbol = commonSymbolProvider.getClassLikeSymbolByClassId(classId)
        val platformSymbol = platformSymbolProvider.getClassLikeSymbolByClassId(classId)

        return when {
            commonSymbol == null -> platformSymbol
            platformSymbol == null -> commonSymbol
            commonSymbol == platformSymbol -> commonSymbol
            else -> {
                _classMapping[classId] = ClassPair(commonSymbol, platformSymbol)
                when {
                    commonSymbol.isExpect -> platformSymbol
                    // TODO(KT-77031): investigate if it's ok to return the platformSymbol from here
                    else -> commonSymbol
                }
            }
        }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        val callableId = CallableId(packageFqName, name)
        processedCallables[callableId]?.let { return it }

        val commonDeclarations = commonSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        val platformDeclarations = platformSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        val resultingDeclarations = preferCommonDeclarations(commonDeclarations, platformDeclarations)
        processedCallables[callableId] = resultingDeclarations
        return resultingDeclarations
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += getTopLevelCallableSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelCallableSymbols(packageFqName, name).filterIsInstanceTo<FirNamedFunctionSymbol, _>(destination)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        getTopLevelCallableSymbols(packageFqName, name).filterIsInstanceTo<FirPropertySymbol, _>(destination)
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return providers.any { it.hasPackage(fqName) }
    }

    private fun <D : FirCallableDeclaration, S : FirCallableSymbol<D>> preferCommonDeclarations(
        commonDeclarations: List<S>,
        platformDeclarations: List<S>,
    ): List<S> {
        val result = commonDeclarations.toMutableList()

        for (platformSymbol in platformDeclarations) {
            val matchingCommonSymbol = commonDeclarations.firstOrNull { areEquivalentTopLevelCallables(it.fir, platformSymbol.fir) }

            if (matchingCommonSymbol != null) {
                _commonCallableToPlatformCallableMap[matchingCommonSymbol] = platformSymbol
                platformCallableToCommonCallableMap[platformSymbol] = matchingCommonSymbol
            } else {
                result += platformSymbol
            }
        }
        return result
    }

    private fun areEquivalentTopLevelCallables(
        first: FirCallableDeclaration,
        second: FirCallableDeclaration,
    ): Boolean {
        return ConeEquivalentCallConflictResolver.areEquivalentTopLevelCallables(
            first,
            second,
            session,
            argumentMappingIsEqual = null
        )
    }
}
