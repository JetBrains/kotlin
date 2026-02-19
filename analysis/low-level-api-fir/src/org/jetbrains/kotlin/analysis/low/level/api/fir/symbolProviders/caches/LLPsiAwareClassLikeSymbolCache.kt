/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.caches

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.getNotNullValueForNotNullContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsOnlyApi
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKnownClassDeclarationSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.hasPsi
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCacheInternals
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder

/**
 * A cache for [FirClassLikeSymbol]s which is able to provide an exact symbol for a given PSI declaration, even if the symbol's [ClassId] is
 * ambiguous.
 *
 * The cache achieves this preciseness by using a *fallback cache*. Consisting of two separate [FirCache]s, the cache first tries to
 * retrieve the class-like symbol from the [mainCache]. If there is truly an ambiguity for a given [ClassId], the cache is able to compute,
 * store, and provide an alternative symbol for the exact PSI declaration from the [ambiguityCache].
 *
 * If no specific PSI declaration is requested, the cache is also able to fulfill [ClassId]-based requests, which do not have any
 * requirements about the originating PSI declaration. The only requirement is that one symbol is consistently returned for the same class
 * ID once it has been cached.
 *
 * Using two separate caches optimizes requests not only for the frequent [ClassId]-based accesses, but also PSI-based accesses, as there
 * should be no ambiguities in >99.9% of all cases. So even when requesting a symbol for a declaration, we can benefit from faster
 * [ClassId] hash code and equality functions compared to the PSI. In ideal cases, [ambiguityCache] will be empty.
 *
 * The cache is used by symbol providers to implement [LLPsiAwareSymbolProvider][org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLPsiAwareSymbolProvider].
 */
internal open class LLPsiAwareClassLikeSymbolCache<E : PsiElement, V : FirClassLikeSymbol<*>?, CONTEXT>(
    private val mainCache: FirCache<ClassId, V, CONTEXT>,
    private val ambiguityCache: FirCache<E, V, CONTEXT>,
) {
    constructor(
        session: LLFirSession,
        computeSymbolByClassId: (ClassId, CONTEXT) -> V,
        computeSymbolByPsi: (E, CONTEXT) -> V,
    ) : this(
        session.firCachesFactory.createCache<ClassId, V, CONTEXT> { classId, context ->
            computeSymbolByClassId(classId, context)
        },
        session.firCachesFactory.createCache<E, V, CONTEXT> { declaration, context ->
            computeSymbolByPsi(declaration, context)
        },
    )

    /**
     * Returns the already cached or newly computed symbol for the given [classId].
     *
     * The function does not query the ambiguity cache because any symbol corresponding to [classId] is acceptable. The given [context] is
     * only used to compute a symbol if it's not already cached. Furthermore, [getSymbolByClassId] must follow the contracts of
     * [LLKnownClassDeclarationSymbolProvider.getClassLikeSymbolByClassId].
     */
    @LLModuleSpecificSymbolProviderAccess
    fun getSymbolByClassId(
        classId: ClassId,
        context: CONTEXT,
        buildAdditionalAttachments: (ExceptionAttachmentBuilder.(ClassId, CONTEXT) -> Unit)? = null,
    ): V =
        mainCache.getNotNullValueForNotNullContext(classId, context, buildAdditionalAttachments)

    /**
     * Returns the symbol for the given [classId] if it's already cached.
     *
     * The function does not query the ambiguity cache because any symbol corresponding to [classId] is acceptable.
     */
    fun getCachedSymbolByClassId(classId: ClassId): V? = mainCache.getValueIfComputed(classId)

    /**
     * Returns the already cached or newly computed symbol for exactly the given [declaration], after ensuring that it is of type [T].
     *
     * [getSymbolByPsi] is a common implementation for [LLPsiAwareSymbolProvider.getClassLikeSymbolByPsi][org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLPsiAwareSymbolProvider.getClassLikeSymbolByPsi].
     */
    @LLModuleSpecificSymbolProviderAccess
    inline fun <reified T : E> getSymbolByPsi(
        classId: ClassId,
        declaration: PsiElement,
        noinline buildAdditionalAttachments: (ExceptionAttachmentBuilder.(ClassId, CONTEXT) -> Unit)? = null,
        createContext: (T) -> CONTEXT,
    ): V? {
        if (declaration !is T) return null

        // Avoid the creation of the context if the symbol is already cached.
        getCachedSymbolByPsi(classId, declaration)?.let { return it }

        return getSymbolByPsi(classId, declaration, createContext(declaration), buildAdditionalAttachments)
    }

    /**
     * Returns the already cached or newly computed symbol for exactly the given [declaration].
     *
     * In contrast to [getSymbolByPsi], this function skips [getCachedSymbolByPsi] and the [declaration] type check, allowing more
     * flexibility on the client side.
     */
    @LLModuleSpecificSymbolProviderAccess
    fun getSymbolByPsi(
        classId: ClassId,
        declaration: E,
        context: CONTEXT,
        buildAdditionalAttachments: (ExceptionAttachmentBuilder.(ClassId, CONTEXT) -> Unit)? = null,
    ): V? {
        // Fast path: Query the cache normally. The *vast majority* of class IDs will not have ambiguities, hence most symbols will be
        // contained in the main cache.
        val symbol = getSymbolByClassId(classId, context, buildAdditionalAttachments)
            ?: return null // If we cannot find any symbol, there is no point in checking the ambiguity cache.

        if (symbol.hasPsi(declaration)) {
            return symbol
        }

        // Edge case: Access the fallback cache to provide an alternative symbol for `declaration`.
        return ambiguityCache.getNotNullValueForNotNullContext(declaration, context) { _, _ ->
            if (buildAdditionalAttachments != null) {
                buildAdditionalAttachments(classId, context)
            }
        }
    }

    /**
     * Returns the symbol for exactly the given [declaration] if it's already cached.
     */
    fun getCachedSymbolByPsi(classId: ClassId, declaration: E): V? =
        getCachedSymbolByClassId(classId)
            ?.takeIf { it.hasPsi(declaration) }
            ?: ambiguityCache.getValueIfComputed(declaration)

    @OptIn(FirCacheInternals::class)
    @LLStatisticsOnlyApi
    internal val cachedValues: List<V>
        get() = mainCache.cachedValues + ambiguityCache.cachedValues
}
