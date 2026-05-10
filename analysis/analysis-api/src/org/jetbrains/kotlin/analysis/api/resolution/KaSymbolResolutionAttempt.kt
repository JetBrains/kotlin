/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * This interface represents an attempt on resolving some [KtResolvable] through [KaResolver.tryResolveSymbols] API.
 *
 * [KaSymbolResolutionAttempt] represents either a [single symbol attempt][KaSingleSymbolResolutionAttempt]
 * or a [compound error][KaCompoundSymbolResolutionError].
 *
 * @see KaResolver.tryResolveSymbols
 */
@KaExperimentalApi
public sealed interface KaSymbolResolutionAttempt : KaLifetimeOwner

/**
 * Represents an attempt to resolve a single symbol, which is either a [success][KaSymbolResolutionSuccess]
 * or an [error][KaSymbolResolutionError].
 *
 * @see KaSymbolResolutionAttempt
 */
@KaExperimentalApi
public sealed interface KaSingleSymbolResolutionAttempt : KaSymbolResolutionAttempt

/**
 * Represents a successful resolution result.
 *
 * Unlike [KaCall], the symbol API doesn't split the API on single-symbol and compound-symbol resolutions.
 * Instead, the result consists of a single symbol for simple cases, and a list of symbols for compound cases.
 *
 * @see KaResolver.tryResolveSymbols
 * @see KaResolver.resolveSymbols
 * @see KaResolver.resolveSymbol
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSymbolResolutionSuccess : KaSingleSymbolResolutionAttempt {
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
public interface KaSymbolResolutionError : KaSingleSymbolResolutionAttempt {
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
 * Represents a failed resolution of a compound (multi) call at the symbol level.
 *
 * This type is produced only when a compound call has a mix of successful and failed sub-calls,
 * or when all sub-calls fail. The [attempts] list contains:
 * - At most one [KaSymbolResolutionSuccess] (merging symbols from all successful sub-calls)
 * - At least one [KaSymbolResolutionError]
 * - At least two entries in total
 *
 * When all sub-calls succeed, [KaSymbolResolutionSuccess] is returned instead.
 * When a single call fails, [KaSymbolResolutionError] is returned instead.
 *
 * Unlike [KaMultiCallResolutionAttempt], this type does not distinguish between specific compound call kinds
 * (for-loop, delegated property, etc.) — it simply holds a flat list of sub-call resolution attempts.
 *
 * @see KaMultiCallResolutionAttempt
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompoundSymbolResolutionError : KaSymbolResolutionAttempt {
    /**
     * The list of individual resolution attempts for each sub-call.
     *
     * Contains at most one [KaSymbolResolutionSuccess] and at least one [KaSymbolResolutionError].
     * At least two entries in total.
     */
    @KaExperimentalApi
    public val attempts: List<KaSingleSymbolResolutionAttempt>
}

/**
 * A list of [KaSymbol].
 *
 * - If [this] is an instance of [KaSymbolResolutionSuccess], the list will contain [KaSymbolResolutionSuccess.symbols].
 * - If [this] is an instance of [KaSymbolResolutionError], the list will contain [KaSymbolResolutionError.candidateSymbols].
 * - If [this] is an instance of [KaCompoundSymbolResolutionError], the list will contain the combined symbols from all attempts.
 *
 * @see KaResolver.tryResolveSymbols
 */
@KaExperimentalApi
public val KaSymbolResolutionAttempt.symbols: List<KaSymbol>
    get() = fold(
        onSuccess = { it },
        onFailure = { attempts ->
            attempts.flatMap {
                if (it is KaSymbolResolutionError) it.candidateSymbols else it.symbols
            }
        },
    )

/**
 * The resolved symbols if the resolution succeeded, or an empty list if it failed.
 *
 * @see KaCallResolutionAttempt.successfulCall
 */
@KaExperimentalApi
public val KaSymbolResolutionAttempt.successfulSymbols: List<KaSymbol>
    get() = fold(onSuccess = { it }, onFailure = { emptyList() })

/**
 * Folds over a [KaSymbolResolutionAttempt] depending on whether the resolution succeeded.
 *
 * - [KaSymbolResolutionSuccess]: invokes [onSuccess] with the resolved [symbols][KaSymbolResolutionSuccess.symbols].
 * - [KaSymbolResolutionError]: invokes [onFailure] with the error wrapped in a single-element list.
 * - [KaCompoundSymbolResolutionError]: invokes [onFailure] with the individual [attempts][KaCompoundSymbolResolutionError.attempts].
 */
@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public inline fun <T> KaSymbolResolutionAttempt.fold(
    onSuccess: (List<KaSymbol>) -> T,
    onFailure: (List<KaSingleSymbolResolutionAttempt>) -> T,
): T {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }

    val attempts = when (this) {
        is KaSymbolResolutionSuccess -> return onSuccess(symbols)
        is KaSymbolResolutionError -> listOf(this)
        is KaCompoundSymbolResolutionError -> attempts
    }

    return onFailure(attempts)
}
