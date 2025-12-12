/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.resolution.KtResolvable

/**
 * This interface represents an attempt on resolving some [KtResolvable] through [KaResolver.tryResolveSymbol] API.
 *
 * - A successful result is represented by [KaSymbolResolutionSuccess]
 * - An unsuccessful result is represented by [KaSymbolResolutionError]
 *
 * @see KaResolver.tryResolveSymbol
 * @see KaSymbolResolutionSuccess
 * @see KaSymbolResolutionError
 */
@KaExperimentalApi
public sealed interface KaSymbolResolutionAttempt : KaLifetimeOwner

/**
 * Represents a successful resolution result. It can be either a single symbol success or a multi-symbol success
 *
 * @see KaSingleSymbolResolutionSuccess
 * @see KaMultiSymbolResolutionSuccess
 */
@KaExperimentalApi
public sealed interface KaSymbolResolutionSuccess : KaSymbolResolutionAttempt

/**
 * Represents a successful resolution resulting in a single symbol
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSingleSymbolResolutionSuccess : KaSymbolResolutionSuccess {
    /**
     * The resolved symbol
     */
    public val symbol: KaSymbol
}

/**
 * Represents a successful resolution resulting in multiple symbols
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaMultiSymbolResolutionSuccess : KaSymbolResolutionSuccess {
    /**
     * The non-empty list of resolved symbols
     */
    public val symbols: List<KaSymbol>
}

/**
 * Represents an error that occurred during the resolution of a [KtResolvable]
 *
 * ### Example
 *
 * ```kotlin
 * class Foo {
 *    private fun bar() {}
 * }
 *
 * fun usage(foo: Foo) {
 *    foo.bar()
 * //     ^^^^^
 * }
 * ```
 *
 * `bar()` will be resolved to [KaSymbolResolutionError] with `INVISIBLE_REFERENCE` diagnostic and the `bar` symbol candidate
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSymbolResolutionError : KaSymbolResolutionAttempt {
    /**
     * Defines a reason why this attempt is unsuccessful
     */
    public val diagnostic: KaDiagnostic

    /**
     * Represents a collection of candidate symbols for a resolution attempt.
     *
     * Code example:
     * ```kotlin
     * class MyClass(private val property: Int)
     *
     * fun check(m: MyClass) {
     *     m.property
     * }
     * ```
     * here `m.property` is resolved into [KaSymbolResolutionError] because it is invisible from the call site,
     * but the compiler produces `INVISIBLE_REFERENCE` diagnostic with `property` candidate
     *
     * **Note: the collection can be empty**
     */
    public val candidateSymbols: List<KaSymbol>
}

/**
 * Returns a list of [KaSymbol].
 *
 * - If [this] is an instance of [KaSingleSymbolResolutionSuccess], the list will contain only [KaSingleSymbolResolutionSuccess.symbol].
 * - If [this] is an instance of [KaMultiSymbolResolutionSuccess], the list will contain [KaMultiSymbolResolutionSuccess.symbols].
 * - If [this] is an instance of [KaSymbolResolutionError], the list will contain [KaSymbolResolutionError.candidateSymbols].
 */
@KaExperimentalApi
public val KaSymbolResolutionAttempt.symbols: List<KaSymbol>
    get() = when (this) {
        is KaSingleSymbolResolutionSuccess -> listOf(symbol)
        is KaMultiSymbolResolutionSuccess -> symbols
        is KaSymbolResolutionError -> candidateSymbols
    }
