/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.caches

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKnownClassDeclarationSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.hasPsi
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * A *fallback cache* for computing and storing ambiguous class-like symbols. The cache is based on a [symbolProvider] and uses it as a
 * first line to retrieve most of the class-like symbols. If there is truly an ambiguity for a given [ClassId], the cache is able to
 * compute, store, and provide an alternative symbol for the exact PSI declaration.
 *
 * The cache is used by symbol providers to implement [LLPsiAwareSymbolProvider][org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLPsiAwareSymbolProvider].
 */
internal open class LLAmbiguousClassLikeSymbolCache<P, K : PsiElement, V : FirClassLikeSymbol<*>?, CONTEXT>(
    private val symbolProvider: P,
    private val searchScope: GlobalSearchScope,
    private val cache: FirCache<K, V, CONTEXT>,
) where P : FirSymbolProvider, P : LLKnownClassDeclarationSymbolProvider<K> {
    constructor(
        symbolProvider: P,
        searchScope: GlobalSearchScope,
        computeSymbol: (K, CONTEXT) -> V,
    ) : this(
        symbolProvider,
        searchScope,
        symbolProvider.session.firCachesFactory.createCache<K, V, CONTEXT> { declaration, context ->
            computeSymbol(declaration, context)
        },
    )

    /**
     * Returns and possibly computes the symbol for the given [declaration].
     *
     * **Do not call this function unless the [symbolProvider]'s *main cache* has already been queried. Otherwise, duplicate symbols might
     * be created by the fallback cache.**
     */
    fun getSymbol(declaration: K, context: CONTEXT): V = cache.getValue(declaration, context)

    /**
     * Returns the symbol for the given [declaration] if it's already cached in the
     *
     * **Take care to query the [symbolProvider]'s *main cache* first.**
     */
    fun getSymbolIfCached(declaration: K): V? = cache.getValueIfComputed(declaration)

    /**
     * A common implementation for [LLPsiAwareSymbolProvider.getClassLikeSymbolByPsi][org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLPsiAwareSymbolProvider.getClassLikeSymbolByPsi].
     */
    inline fun <reified T : K> getClassLikeSymbolByPsi(
        classId: ClassId,
        declaration: PsiElement,
        createContext: (T) -> CONTEXT,
    ): FirClassLikeSymbol<*>? {
        if (declaration !is T) return null

        // To calculate a symbol from a known `declaration`, we need to make sure that `declaration` is in the scope of the symbol provider.
        if (declaration.containingFile.virtualFile !in searchScope) return null

        // Fast path: Query the symbol provider normally. The *vast majority* of class IDs will not have ambiguities, hence most symbols
        // will be contained in the symbol provider's main class-like cache.
        val symbol = symbolProvider.getClassLikeSymbolByClassId(classId, declaration)
            ?: return null // If we cannot find any symbol, there is no point in checking the ambiguity cache.

        if (symbol.hasPsi(declaration)) {
            return symbol
        }

        // Avoid creation of the context if the symbol is already cached. Note that `createContext` likely recreates the context anyway, as
        // the call to `getClassLikeSymbolByClassId` above probably already created the same context. However, we ignore this inefficiency
        // to avoid complicating the code further. Access to and computation in the fallback cache is a very rare operation, so it's fine to
        // be slightly inefficient.
        cache.getValueIfComputed(declaration)?.let { return it }

        // Edge case: Access the fallback cache to provide an alternative symbol for `declaration`.
        return cache.getValue(declaration, createContext(declaration))
    }

    companion object {
        fun <P, K : PsiElement, V : FirClassLikeSymbol<*>?> withoutContext(
            symbolProvider: P,
            searchScope: GlobalSearchScope,
            computeSymbol: (K) -> V,
        ): LLAmbiguousClassLikeSymbolCacheWithoutContext<P, K, V> where P : FirSymbolProvider, P : LLKnownClassDeclarationSymbolProvider<K> =
            LLAmbiguousClassLikeSymbolCacheWithoutContext(
                symbolProvider,
                searchScope,
                computeSymbol,
            )
    }
}

internal class LLAmbiguousClassLikeSymbolCacheWithoutContext<P, K : PsiElement, V : FirClassLikeSymbol<*>?>(
    symbolProvider: P,
    searchScope: GlobalSearchScope,
    computeSymbol: (K) -> V,
) : LLAmbiguousClassLikeSymbolCache<P, K, V, Nothing?>(
    symbolProvider,
    searchScope,
    symbolProvider.session.firCachesFactory.createCache { declaration -> computeSymbol(declaration) },
) where P : FirSymbolProvider, P : LLKnownClassDeclarationSymbolProvider<K> {
    inline fun <reified T : K> getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): FirClassLikeSymbol<*>? =
        getClassLikeSymbolByPsi<T>(classId, declaration, createContext = { null })
}
