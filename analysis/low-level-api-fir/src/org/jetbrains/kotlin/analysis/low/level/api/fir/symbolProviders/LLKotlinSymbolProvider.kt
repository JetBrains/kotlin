/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

/**
 * A [FirSymbolProvider] which provides symbols from Kotlin sources via [KotlinDeclarationProvider].
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined.LLCombinedKotlinSymbolProvider
 */
internal abstract class LLKotlinSymbolProvider(session: FirSession) :
    FirSymbolProvider(session),
    LLKnownClassDeclarationSymbolProvider<KtClassLikeDeclaration>,
    LLPsiAwareSymbolProvider {
    abstract val declarationProvider: KotlinDeclarationProvider

    abstract val packageProvider: KotlinPackageProvider

    /**
     * Whether the [LLKotlinSymbolProvider] should be able to find symbols defined in `kotlin` packages. This is usually not the case for
     * source sessions, unless the `allowKotlinPackage` flag is enabled in the session's `languageVersionSettings`.
     */
    abstract val allowKotlinPackage: Boolean

    /**
     * Maps the [FirCallableSymbol]s with the given [callableId] for known [callables] to [destination].
     *
     * As the [callables] are already known, this function is optimized to avoid declaration provider accesses. However, the given callable
     * declarations have to be coherent with the union of [KotlinDeclarationProvider.getTopLevelFunctions] and
     * [KotlinDeclarationProvider.getTopLevelProperties]. In other words, the callables must be chosen such that the resulting
     * [FirCallableSymbol]s are the same as the result of [getTopLevelCallableSymbolsTo] without known declarations.
     */
    @FirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<KtCallableDeclaration>,
    )

    /**
     * Maps the [FirNamedFunctionSymbol]s with the given [callableId] for known [functions] to [destination].
     *
     * As the [functions] are already known, this function is optimized to avoid declaration provider accesses. However, the given function
     * declarations have to be coherent with [KotlinDeclarationProvider.getTopLevelFunctions]. In other words, the functions must be chosen
     * such that the resulting [FirNamedFunctionSymbol]s are the same as the result of [getTopLevelFunctionSymbolsTo] without known
     * declarations.
     */
    @FirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<KtNamedFunction>,
    )

    /**
     * Maps the [FirPropertySymbol]s with the given [callableId] for known [properties] to [destination].
     *
     * As the [properties] are already known, this function is optimized to avoid declaration provider accesses. However, the given property
     * declarations have to be coherent with [KotlinDeclarationProvider.getTopLevelProperties]. In other words, the properties must be
     * chosen such that the resulting [FirPropertySymbol]s are the same as the result of [getTopLevelPropertySymbolsTo] without known
     * declarations.
     */
    @FirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<KtProperty>,
    )

    override fun toString(): String {
        return "${this::class.simpleName} for $session"
    }
}
