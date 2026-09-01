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
 * [KaSymbolResolutionAttempt] represents either a [simple symbol attempt][KaSimpleSymbolResolutionAttempt]
 * or a [compound error][KaCompoundSymbolResolutionError].
 *
 * @see KaResolver.tryResolveSymbols
 */
@KaExperimentalApi
public sealed interface KaSymbolResolutionAttempt : KaLifetimeOwner

/**
 * Represents an attempt to resolve a simple (non-compound) [KtResolvable], which is either
 * a [success][KaSimpleSymbolResolutionSuccess] or an [error][KaSimpleSymbolResolutionError].
 *
 * @see KaSymbolResolutionAttempt
 */
@KaExperimentalApi
public sealed interface KaSimpleSymbolResolutionAttempt : KaSymbolResolutionAttempt

/**
 * The former name of [KaSimpleSymbolResolutionAttempt].
 *
 * @see KaSimpleSymbolResolutionAttempt
 */
@Deprecated(
    message = "Use 'KaSimpleSymbolResolutionAttempt' instead",
    replaceWith = ReplaceWith(
        expression = "KaSimpleSymbolResolutionAttempt",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaSimpleSymbolResolutionAttempt"],
    ),
)
@KaExperimentalApi
public typealias KaSingleSymbolResolutionAttempt = KaSimpleSymbolResolutionAttempt

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
public interface KaSimpleSymbolResolutionSuccess : KaSimpleSymbolResolutionAttempt {
    /**
     * The non-empty list of resolved symbols
     */
    public val symbols: List<KaSymbol>
}

/**
 * The former name of [KaSimpleSymbolResolutionSuccess].
 *
 * @see KaSimpleSymbolResolutionSuccess
 */
@Deprecated(
    message = "Use 'KaSimpleSymbolResolutionSuccess' instead",
    replaceWith = ReplaceWith(
        expression = "KaSimpleSymbolResolutionSuccess",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaSimpleSymbolResolutionSuccess"],
    ),
)
@KaExperimentalApi
public typealias KaSymbolResolutionSuccess = KaSimpleSymbolResolutionSuccess

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
 * `bar()` will be resolved to [KaSimpleSymbolResolutionError] with `INVISIBLE_REFERENCE` diagnostic and the `bar` symbol candidate
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSimpleSymbolResolutionError : KaSimpleSymbolResolutionAttempt {
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
     * here `m.property` is resolved into [KaSimpleSymbolResolutionError] because it is invisible from the call site,
     * but the compiler produces `INVISIBLE_REFERENCE` diagnostic with `property` candidate
     *
     * **Note: the collection can be empty**
     */
    public val candidateSymbols: List<KaSymbol>
}

/**
 * The former name of [KaSimpleSymbolResolutionError].
 *
 * @see KaSimpleSymbolResolutionError
 */
@Deprecated(
    message = "Use 'KaSimpleSymbolResolutionError' instead",
    replaceWith = ReplaceWith(
        expression = "KaSimpleSymbolResolutionError",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaSimpleSymbolResolutionError"],
    ),
)
@KaExperimentalApi
public typealias KaSymbolResolutionError = KaSimpleSymbolResolutionError

/**
 * Represents a failed resolution of a compound (multi) call at the symbol level.
 *
 * This type is produced only when a compound call has a mix of successful and failed sub-calls,
 * or when all sub-calls fail. The [simpleAttempts] list contains:
 * - At most one [KaSimpleSymbolResolutionSuccess] (merging symbols from all successful sub-calls)
 * - At least one [KaSimpleSymbolResolutionError]
 * - At least two entries in total
 *
 * When all sub-calls succeed, [KaSimpleSymbolResolutionSuccess] is returned instead.
 * When a simple call fails, [KaSimpleSymbolResolutionError] is returned instead.
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
     * Contains at most one [KaSimpleSymbolResolutionSuccess] and at least one [KaSimpleSymbolResolutionError].
     * At least two entries in total.
     */
    @KaExperimentalApi
    public val simpleAttempts: List<KaSimpleSymbolResolutionAttempt>

    /**
     * The former name of [simpleAttempts].
     *
     * @see simpleAttempts
     */
    @Deprecated(
        message = "Use 'simpleAttempts' instead",
        replaceWith = ReplaceWith(expression = "simpleAttempts"),
    )
    @KaExperimentalApi
    public val attempts: List<KaSimpleSymbolResolutionAttempt>
}

/**
 * A list of [KaSymbol].
 *
 * - If [this] is an instance of [KaSimpleSymbolResolutionSuccess], the list will contain [KaSimpleSymbolResolutionSuccess.symbols].
 * - If [this] is an instance of [KaSimpleSymbolResolutionError], the list will contain [KaSimpleSymbolResolutionError.candidateSymbols].
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
                if (it is KaSimpleSymbolResolutionError) it.candidateSymbols else it.symbols
            }
        },
    )

/**
 * The resolved symbols if the resolution succeeded, or an empty list if it failed.
 *
 * @see KaCallResolutionAttempt.successful
 */
@KaExperimentalApi
public val KaSymbolResolutionAttempt.successfulSymbols: List<KaSymbol>
    get() = fold(onSuccess = { it }, onFailure = { emptyList() })

/**
 * Folds over a [KaSymbolResolutionAttempt] depending on whether the resolution succeeded.
 *
 * - [KaSimpleSymbolResolutionSuccess]: invokes [onSuccess] with the resolved [symbols][KaSimpleSymbolResolutionSuccess.symbols].
 * - [KaSimpleSymbolResolutionError]: invokes [onFailure] with the error wrapped in a single-element list.
 * - [KaCompoundSymbolResolutionError]: invokes [onFailure] with the individual [simpleAttempts][KaCompoundSymbolResolutionError.simpleAttempts].
 */
@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public inline fun <T> KaSymbolResolutionAttempt.fold(
    onSuccess: (List<KaSymbol>) -> T,
    onFailure: (List<KaSimpleSymbolResolutionAttempt>) -> T,
): T {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }

    val attempts = when (this) {
        is KaSimpleSymbolResolutionSuccess -> return onSuccess(symbols)
        is KaSimpleSymbolResolutionError -> listOf(this)
        is KaCompoundSymbolResolutionError -> simpleAttempts
    }

    return onFailure(attempts)
}
