/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:[JvmName("KaCalls") JvmMultifileClass]

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.resolution.KtResolvableCall
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Represents an attempt to resolve [KtResolvableCall].
 *
 * [KaCallResolutionAttempt] represents either a [single call attempt][KaSingleCallResolutionAttempt]
 * or a [multi-call attempt][KaMultiCallResolutionAttempt].
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 **/
@KaExperimentalApi
public sealed interface KaCallResolutionAttempt : KaLifetimeOwner

/**
 * Represents an attempt to resolve a single call (as opposed to a [multi-call][KaMultiCallResolutionAttempt]),
 * which is either a [success][KaCallResolutionSuccess] or an [error][KaCallResolutionError].
 *
 * Both [KaCallResolutionSuccess.call] and [KaCallResolutionError.candidateCalls] always contain [KaSingleCall]s.
 */
@KaExperimentalApi
public sealed interface KaSingleCallResolutionAttempt : KaCallResolutionAttempt

/**
 * Represents an error that occurred during the resolution of a [KtResolvableCall]
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
 * `bar()` will be resolved to [KaCallResolutionError] with `INVISIBLE_REFERENCE` diagnostic and the `bar` call
 *
 * @see KaResolver.tryResolveCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCallResolutionError : KaSingleCallResolutionAttempt {
    /**
     * The diagnostic associated with the error
     */
    public val diagnostic: KaDiagnostic

    /**
     * The list of candidate calls that were considered during the resolution. Can be empty
     */
    public val candidateCalls: List<KaSingleCall<*, *>>
}

/**
 * Represents a successful resolution of a single [KtResolvableCall].
 *
 * For compound calls (e.g., `i += 1`, `for (x in list)`), see [KaMultiCallResolutionAttempt] instead.
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCallResolutionSuccess : KaSingleCallResolutionAttempt {
    /**
     * The resolved [KaSingleCall].
     */
    public val call: KaSingleCall<*, *>
}

/**
 * Represents an attempt to resolve a compound (multi) call, such as a for-loop, delegated property access,
 * or compound assignment. The assembled [call] is always a [KaMultiCall].
 *
 * Contains individual [KaSingleCallResolutionAttempt]s for each sub-call, preserving resolution results
 * independently — even if one sub-call fails, the results of other sub-calls are still available.
 */
@KaExperimentalApi
public sealed interface KaMultiCallResolutionAttempt : KaCallResolutionAttempt {
    /**
     * The assembled multi-call, or `null` if any sub-call failed.
     *
     * Overridden in concrete subtypes with a more precise return type.
     */
    public val call: KaMultiCall?

    /**
     * The list of individual resolution attempts for each sub-call.
     */
    public val attempts: List<KaSingleCallResolutionAttempt>
}

/**
 * Represents an attempt to resolve a `for` loop, which desugars into three operator calls:
 * [iterator()][iteratorCallAttempt], [hasNext()][hasNextCallAttempt], and [next()][nextCallAttempt].
 *
 * When all sub-calls succeed, [call] contains the assembled [KaForLoopCall].
 * When any sub-call fails, [call] is `null` but individual attempts still contain their resolution results.
 *
 * @see KaForLoopCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaForLoopCallResolutionAttempt : KaMultiCallResolutionAttempt {
    /** The assembled [KaForLoopCall], or `null` if any sub-call failed. */
    override val call: KaForLoopCall?

    /**
     * The resolution attempt for the `iterator()` call.
     *
     * @see KaForLoopCall.iteratorCall
     */
    public val iteratorCallAttempt: KaSingleCallResolutionAttempt

    /**
     * The resolution attempt for the `hasNext()` call.
     *
     * @see KaForLoopCall.hasNextCall
     */
    public val hasNextCallAttempt: KaSingleCallResolutionAttempt

    /**
     * The resolution attempt for the `next()` call.
     *
     * @see KaForLoopCall.nextCall
     */
    public val nextCallAttempt: KaSingleCallResolutionAttempt
}

/**
 * Represents an attempt to resolve a delegated property, which desugars into up to three operator calls:
 * [getValue()][valueGetterCallAttempt], [setValue()][valueSetterCallAttempt], and [provideDelegate()][provideDelegateCallAttempt].
 *
 * When all sub-calls succeed, [call] contains the assembled [KaDelegatedPropertyCall].
 * When any sub-call fails, [call] is `null` but individual attempts still contain their resolution results.
 *
 * @see KaDelegatedPropertyCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDelegatedPropertyCallResolutionAttempt : KaMultiCallResolutionAttempt {
    /** The assembled [KaDelegatedPropertyCall], or `null` if any sub-call failed. */
    override val call: KaDelegatedPropertyCall?

    /**
     * The resolution attempt for the `getValue()` call.
     *
     * @see KaDelegatedPropertyCall.valueGetterCall
     */
    public val valueGetterCallAttempt: KaSingleCallResolutionAttempt

    /**
     * The resolution attempt for the `setValue()` call. `null` for `val` properties.
     *
     * @see KaDelegatedPropertyCall.valueSetterCall
     */
    public val valueSetterCallAttempt: KaSingleCallResolutionAttempt?

    /**
     * The resolution attempt for the `provideDelegate()` call. `null` if not applicable.
     *
     * @see KaDelegatedPropertyCall.provideDelegateCall
     */
    public val provideDelegateCallAttempt: KaSingleCallResolutionAttempt?
}

/**
 * Represents an attempt to resolve a compound variable access (e.g. `i += 1` or `i++`).
 *
 * When all sub-calls succeed, [call] contains the assembled [KaCompoundVariableAccessCall].
 * When any sub-call fails, [call] is `null` but individual attempts still contain their resolution results.
 *
 * @see KaCompoundVariableAccessCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompoundVariableAccessCallResolutionAttempt : KaMultiCallResolutionAttempt {
    /**
     * The assembled [KaCompoundVariableAccessCall], or `null` if any sub-call failed.
     *
     * @see KaCompoundAccessCall.operationCall
     */
    override val call: KaCompoundVariableAccessCall?

    /**
     * The resolution attempt for the variable access.
     *
     * @see KaCompoundVariableAccessCall.variableCall
     */
    public val variableCallAttempt: KaSingleCallResolutionAttempt

    /**
     * The resolution attempt for the operation call (e.g. `plus`, `inc`).
     *
     * @see KaCompoundAccessCall.operationCall
     */
    public val operationCallAttempt: KaSingleCallResolutionAttempt
}

/**
 * Represents an attempt to resolve a compound array access (e.g. `a[1] += "foo"` or `a[0]++`).
 *
 * When all sub-calls succeed, [call] contains the assembled [KaCompoundArrayAccessCall].
 * When any sub-call fails, [call] is `null` but individual attempts still contain their resolution results.
 *
 * @see KaCompoundArrayAccessCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompoundArrayAccessCallResolutionAttempt : KaMultiCallResolutionAttempt {
    /**
     * The assembled [KaCompoundArrayAccessCall], or `null` if any sub-call failed.
     *
     * @see KaCompoundAccessCall.operationCall
     */
    override val call: KaCompoundArrayAccessCall?

    /**
     * The resolution attempt for the `get()` call.
     *
     * @see KaCompoundArrayAccessCall.getterCall
     */
    public val getterCallAttempt: KaSingleCallResolutionAttempt

    /**
     * The resolution attempt for the operation call (e.g. `plus`, `inc`).
     *
     * @see KaCompoundAccessCall.operationCall
     */
    public val operationCallAttempt: KaSingleCallResolutionAttempt

    /**
     * The resolution attempt for the `set()` call.
     *
     * @see KaCompoundArrayAccessCall.setterCall
     */
    public val setterCallAttempt: KaSingleCallResolutionAttempt
}

/**
 * [KaMultiCallResolutionAttempt] represents a bunch of unrelated compound calls,
 * so the client typically is not expected to handle all possible cases.
 *
 * The usual way to work with compound calls is to get them using a special [KaResolver.resolveCall] overload
 */
@Suppress("unused")
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
private interface KaMultiUnknownCallResolutionAttempt : KaMultiCallResolutionAttempt

/**
 * The flattened list of resolved calls.
 *
 * - [KaCallResolutionSuccess]: the resolved [call][KaCallResolutionSuccess.call] as a single-element list.
 * - [KaCallResolutionError]: the [candidate calls][KaCallResolutionError.candidateCalls].
 * - [KaMultiCallResolutionAttempt]: the assembled [call][KaMultiCallResolutionAttempt.call] if all sub-calls
 *   succeeded, or the combined calls from individual [attempts][KaMultiCallResolutionAttempt.attempts] otherwise.
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.calls: List<KaSingleOrMultiCall>
    get() = if (this is KaCallResolutionError) {
        candidateCalls
    } else {
        fold(
            onSuccess = ::listOf,
            onFailure = { it.flatMap(KaSingleCallResolutionAttempt::calls) },
        )
    }

/**
 * The resolved call if the resolution succeeded, or `null` if it failed.
 *
 * - [KaCallResolutionSuccess]: the resolved [call][KaCallResolutionSuccess.call].
 * - [KaCallResolutionError]: `null`.
 * - [KaMultiCallResolutionAttempt]: the assembled [call][KaMultiCallResolutionAttempt.call]
 *   if all sub-calls succeeded, or `null` otherwise.
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.successfulCall: KaSingleOrMultiCall?
    get() = fold(onSuccess = { it }, onFailure = { null })

/**
 * Folds over a [KaCallResolutionAttempt] depending on whether the resolution succeeded.
 *
 * - [KaCallResolutionSuccess]: invokes [onSuccess] with the resolved [call][KaCallResolutionSuccess.call].
 * - [KaCallResolutionError]: invokes [onFailure] with the error wrapped in a single-element list.
 * - [KaMultiCallResolutionAttempt]: if all sub-calls succeeded, invokes [onSuccess] with the assembled
 *   [call][KaMultiCallResolutionAttempt.call]; otherwise invokes [onFailure] with the individual
 *   [attempts][KaMultiCallResolutionAttempt.attempts].
 */
@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public inline fun <T> KaCallResolutionAttempt.fold(
    onSuccess: (KaSingleOrMultiCall) -> T,
    onFailure: (List<KaSingleCallResolutionAttempt>) -> T,
): T {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }

    val call = when (this) {
        is KaCallResolutionSuccess -> call
        is KaMultiCallResolutionAttempt -> call
        else -> null
    }

    if (call != null) return onSuccess(call)

    val attempts = when (this) {
        is KaCallResolutionError -> listOf(this)
        is KaMultiCallResolutionAttempt -> attempts
        else -> error("Unexpected ${KaCallResolutionAttempt::class.simpleName}: $this")
    }

    return onFailure(attempts)
}
