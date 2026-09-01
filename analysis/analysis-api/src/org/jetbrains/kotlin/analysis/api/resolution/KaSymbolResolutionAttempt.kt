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
 * A [KaCompoundSymbolResolutionError] is *not* an instance of this type, so a type check against this type is not
 * a complete failure check — use [errors] or [isSuccessful] instead.
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
    get() = when (this) {
        is KaSimpleSymbolResolutionSuccess -> symbols
        is KaSimpleSymbolResolutionError -> candidateSymbols
        is KaCompoundSymbolResolutionError -> simpleAttempts.flatMap(KaSimpleSymbolResolutionAttempt::symbols)
    }

/**
 * The resolved symbols if the resolution succeeded, or an empty list if it failed.
 *
 * A successful resolution always has at least one symbol, so an empty list always means a failure.
 *
 * @see isSuccessful
 * @see KaCallResolutionAttempt.successful
 */
@KaExperimentalApi
public val KaSymbolResolutionAttempt.successfulSymbols: List<KaSymbol>
    get() = fold(onSuccess = { it }, onFailure = { emptyList() })

/**
 * The flattened list of simple resolution attempts.
 *
 * - [KaSimpleSymbolResolutionAttempt]: [this] attempt as a single-element list.
 * - [KaCompoundSymbolResolutionError]: the individual [sub-attempts][KaCompoundSymbolResolutionError.simpleAttempts].
 *
 * The list is never empty.
 */
@KaExperimentalApi
public val KaSymbolResolutionAttempt.simpleAttempts: List<KaSimpleSymbolResolutionAttempt>
    get() = when (this) {
        is KaSimpleSymbolResolutionAttempt -> listOf(this)
        is KaCompoundSymbolResolutionError -> simpleAttempts
    }

/**
 * The list of errors that occurred during the resolution.
 *
 * - [KaSimpleSymbolResolutionSuccess]: an empty list.
 * - [KaSimpleSymbolResolutionError]: [this] error as a single-element list.
 * - [KaCompoundSymbolResolutionError]: the errors among the individual
 *   [sub-attempts][KaCompoundSymbolResolutionError.simpleAttempts], which always contain at least one.
 *
 * The list is empty if and only if the resolution succeeded. So, unlike a `this is KaSimpleSymbolResolutionError`
 * check, which only covers simple attempts, this property detects failures of every attempt kind.
 *
 * @see isSuccessful
 */
@KaExperimentalApi
public val KaSymbolResolutionAttempt.errors: List<KaSimpleSymbolResolutionError>
    get() = when (this) {
        is KaSimpleSymbolResolutionSuccess -> emptyList()
        is KaSimpleSymbolResolutionError -> listOf(this)
        is KaCompoundSymbolResolutionError -> simpleAttempts.filterIsInstance<KaSimpleSymbolResolutionError>()
    }

/**
 * Whether the resolution succeeded.
 *
 * `true` if and only if [errors] is empty. A [KaCompoundSymbolResolutionError] is always a failure, even when some
 * of its [sub-attempts][KaCompoundSymbolResolutionError.simpleAttempts] succeeded.
 *
 * @see errors
 * @see successfulSymbols
 */
@KaExperimentalApi
public val KaSymbolResolutionAttempt.isSuccessful: Boolean
    get() = this is KaSimpleSymbolResolutionSuccess

/**
 * Folds over a [KaSymbolResolutionAttempt] depending on whether the resolution succeeded.
 *
 * - [KaSimpleSymbolResolutionSuccess]: invokes [onSuccess] with the resolved [symbols][KaSimpleSymbolResolutionSuccess.symbols].
 * - [KaSimpleSymbolResolutionError]: invokes [onFailure] with the error wrapped in a single-element list.
 * - [KaCompoundSymbolResolutionError]: invokes [onFailure] with the [errors] of the failed sub-calls. The successful
 *   sub-call, if any, is not passed to [onFailure]; use [simpleAttempts] to reach it.
 */
@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public inline fun <T> KaSymbolResolutionAttempt.fold(
    onSuccess: (List<KaSymbol>) -> T,
    onFailure: (List<KaSimpleSymbolResolutionError>) -> T,
): T {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }

    return if (this is KaSimpleSymbolResolutionSuccess) {
        onSuccess(symbols)
    } else {
        onFailure(errors)
    }
}
