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
 * [KaCallResolutionAttempt] represents either a [simple call attempt][KaSimpleCallResolutionAttempt]
 * or a [multi-call attempt][KaMultiCallResolutionAttempt].
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 **/
@KaExperimentalApi
public sealed interface KaCallResolutionAttempt : KaLifetimeOwner

/**
 * Represents an attempt to resolve a simple call (as opposed to a [multi-call][KaMultiCallResolutionAttempt]),
 * which is either a [success][KaSimpleCallResolutionSuccess] or an [error][KaSimpleCallResolutionError].
 *
 * Both [KaSimpleCallResolutionSuccess.call] and [KaSimpleCallResolutionError.candidateCalls] always contain [KaSimpleCall]s.
 */
@KaExperimentalApi
public sealed interface KaSimpleCallResolutionAttempt : KaCallResolutionAttempt

/**
 * The former name of [KaSimpleCallResolutionAttempt].
 *
 * @see KaSimpleCallResolutionAttempt
 */
@Deprecated(
    message = "Use 'KaSimpleCallResolutionAttempt' instead",
    replaceWith = ReplaceWith(
        expression = "KaSimpleCallResolutionAttempt",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaSimpleCallResolutionAttempt"],
    ),
)
@KaExperimentalApi
public typealias KaSingleCallResolutionAttempt = KaSimpleCallResolutionAttempt

/**
 * Represents an error that occurred during the resolution of a [KtResolvableCall]
 *
 * A failed [KaMultiCallResolutionAttempt] is *not* an instance of this type, so a type check against this type is
 * not a complete failure check — use [errors] or [isSuccessful] instead.
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
 * `bar()` will be resolved to [KaSimpleCallResolutionError] with `INVISIBLE_REFERENCE` diagnostic and the `bar` call
 *
 * @see errors
 * @see KaResolver.tryResolveCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSimpleCallResolutionError : KaSimpleCallResolutionAttempt {
    /**
     * The diagnostic associated with the error
     */
    public val diagnostic: KaDiagnostic

    /**
     * The list of candidate calls that were considered during the resolution. Can be empty
     */
    public val candidateCalls: List<KaSimpleCall<*, *>>
}

/**
 * The former name of [KaSimpleCallResolutionError].
 *
 * @see KaSimpleCallResolutionError
 */
@Deprecated(
    message = "Use 'KaSimpleCallResolutionError' instead",
    replaceWith = ReplaceWith(
        expression = "KaSimpleCallResolutionError",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaSimpleCallResolutionError"],
    ),
)
@KaExperimentalApi
public typealias KaCallResolutionError = KaSimpleCallResolutionError

/**
 * Represents a successful resolution of a simple [KtResolvableCall].
 *
 * For compound calls (e.g., `i += 1`, `for (x in list)`), see [KaMultiCallResolutionAttempt] instead.
 *
 * Success means that the resolution produced a call: the callee was resolved and carries no diagnostic of its own.
 * It does not mean that the element is free of diagnostics, as they may be attached elsewhere in the call, e.g. to an
 * argument. Use [diagnostics][org.jetbrains.kotlin.analysis.api.components.diagnostics] to check the element itself.
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSimpleCallResolutionSuccess : KaSimpleCallResolutionAttempt {
    /**
     * The resolved [KaSimpleCall].
     */
    public val call: KaSimpleCall<*, *>
}

/**
 * The former name of [KaSimpleCallResolutionSuccess].
 *
 * @see KaSimpleCallResolutionSuccess
 */
@Deprecated(
    message = "Use 'KaSimpleCallResolutionSuccess' instead",
    replaceWith = ReplaceWith(
        expression = "KaSimpleCallResolutionSuccess",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaSimpleCallResolutionSuccess"],
    ),
)
@KaExperimentalApi
public typealias KaCallResolutionSuccess = KaSimpleCallResolutionSuccess

/**
 * Represents an attempt to resolve a compound (multi) call, such as a for-loop, delegated property access,
 * or compound assignment. The assembled [call] is always a [KaMultiCall].
 *
 * Contains individual [KaSimpleCallResolutionAttempt]s for each sub-call, preserving resolution results
 * independently — even if one sub-call fails, the results of other sub-calls are still available.
 */
@KaExperimentalApi
public sealed interface KaMultiCallResolutionAttempt : KaCallResolutionAttempt {
    /**
     * The assembled multi-call, or `null` if any sub-call failed.
     *
     * `null` if and only if at least one of the [simpleAttempts] is a [KaSimpleCallResolutionError]. The
     * successfully resolved sub-calls remain available through [simpleAttempts] even then.
     *
     * Overridden in concrete subtypes with a more precise return type.
     */
    public val call: KaMultiCall?

    /**
     * The list of individual resolution attempts for each sub-call.
     */
    public val simpleAttempts: List<KaSimpleCallResolutionAttempt>

    /**
     * The former name of [simpleAttempts].
     *
     * @see simpleAttempts
     */
    @Deprecated(
        message = "Use 'simpleAttempts' instead",
        replaceWith = ReplaceWith(expression = "simpleAttempts"),
    )
    public val attempts: List<KaSimpleCallResolutionAttempt>
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
    public val iteratorCallAttempt: KaSimpleCallResolutionAttempt

    /**
     * The resolution attempt for the `hasNext()` call.
     *
     * @see KaForLoopCall.hasNextCall
     */
    public val hasNextCallAttempt: KaSimpleCallResolutionAttempt

    /**
     * The resolution attempt for the `next()` call.
     *
     * @see KaForLoopCall.nextCall
     */
    public val nextCallAttempt: KaSimpleCallResolutionAttempt
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
    public val valueGetterCallAttempt: KaSimpleCallResolutionAttempt

    /**
     * The resolution attempt for the `setValue()` call. `null` for `val` properties.
     *
     * @see KaDelegatedPropertyCall.valueSetterCall
     */
    public val valueSetterCallAttempt: KaSimpleCallResolutionAttempt?

    /**
     * The resolution attempt for the `provideDelegate()` call. `null` if not applicable.
     *
     * @see KaDelegatedPropertyCall.provideDelegateCall
     */
    public val provideDelegateCallAttempt: KaSimpleCallResolutionAttempt?
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
    public val variableCallAttempt: KaSimpleCallResolutionAttempt

    /**
     * The resolution attempt for the operation call (e.g. `plus`, `inc`).
     *
     * @see KaCompoundAccessCall.operationCall
     */
    public val operationCallAttempt: KaSimpleCallResolutionAttempt
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
    public val getterCallAttempt: KaSimpleCallResolutionAttempt

    /**
     * The resolution attempt for the operation call (e.g. `plus`, `inc`).
     *
     * @see KaCompoundAccessCall.operationCall
     */
    public val operationCallAttempt: KaSimpleCallResolutionAttempt

    /**
     * The resolution attempt for the `set()` call.
     *
     * @see KaCompoundArrayAccessCall.setterCall
     */
    public val setterCallAttempt: KaSimpleCallResolutionAttempt
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
 * - [KaSimpleCallResolutionSuccess]: the resolved [call][KaSimpleCallResolutionSuccess.call] as a single-element list.
 * - [KaSimpleCallResolutionError]: the [candidate calls][KaSimpleCallResolutionError.candidateCalls].
 * - [KaMultiCallResolutionAttempt]: the assembled [call][KaMultiCallResolutionAttempt.call] if all sub-calls
 *   succeeded, or the combined calls from individual [simpleAttempts][KaMultiCallResolutionAttempt.simpleAttempts] otherwise.
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.calls: List<KaSimpleOrMultiCall>
    get() = when (this) {
        is KaSimpleCallResolutionError -> candidateCalls
        is KaSimpleCallResolutionSuccess -> listOf(call)
        is KaMultiCallResolutionAttempt -> call?.let(::listOf) ?: simpleAttempts.flatMap(KaSimpleCallResolutionAttempt::calls)
    }

/**
 * The only call of [calls], or `null` if the attempt has no calls or more than one.
 *
 * Unlike [successful], a call is also returned for a failed resolution, as long as [calls] holds exactly one. For a
 * failed [KaMultiCallResolutionAttempt] that entry may come from any sub-attempt — a resolved sub-call, or a candidate
 * of a failed one — so it is not necessarily a candidate for the element itself.
 *
 * For a *successful* [KaMultiCallResolutionAttempt], [calls] holds the assembled [KaMultiCall], so [single] is that
 * multi-call and its [simple]/[function]/[variable] narrowings are all `null`.
 *
 * #### Example
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
 * `bar()` is resolved to a [KaSimpleCallResolutionError], so [successful] is `null`, while [single] is the `bar` candidate call.
 *
 * @see calls
 * @see successful
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.single: KaSimpleOrMultiCall?
    get() = calls.singleOrNull()

/**
 * The flattened list of simple resolution attempts.
 *
 * - [KaSimpleCallResolutionAttempt]: [this] attempt as a single-element list.
 * - [KaMultiCallResolutionAttempt]: the individual [sub-attempts][KaMultiCallResolutionAttempt.simpleAttempts].
 *
 * The list is never empty.
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.simpleAttempts: List<KaSimpleCallResolutionAttempt>
    get() = when (this) {
        is KaSimpleCallResolutionAttempt -> listOf(this)
        is KaMultiCallResolutionAttempt -> simpleAttempts
    }

/**
 * The list of errors that occurred during the resolution.
 *
 * - [KaSimpleCallResolutionSuccess]: an empty list.
 * - [KaSimpleCallResolutionError]: [this] error as a single-element list.
 * - [KaMultiCallResolutionAttempt]: the errors among the individual [sub-attempts][KaMultiCallResolutionAttempt.simpleAttempts].
 *   A multi-call attempt fails as soon as any of its sub-calls fails, so the list is empty if and only if
 *   the assembled [call][KaMultiCallResolutionAttempt.call] is not `null`.
 *
 * The list is empty if and only if the resolution succeeded. So, unlike a `this is KaSimpleCallResolutionError`
 * check, which only covers simple attempts, this property detects failures of every attempt kind.
 *
 * @see simpleAttempts
 * @see successful
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.errors: List<KaSimpleCallResolutionError>
    get() = when (this) {
        is KaSimpleCallResolutionSuccess -> emptyList()
        is KaSimpleCallResolutionError -> listOf(this)
        is KaMultiCallResolutionAttempt -> simpleAttempts.filterIsInstance<KaSimpleCallResolutionError>()
    }

/**
 * The resolved call if the resolution succeeded, or `null` if it failed.
 *
 * - [KaSimpleCallResolutionSuccess]: the resolved [call][KaSimpleCallResolutionSuccess.call].
 * - [KaSimpleCallResolutionError]: `null`.
 * - [KaMultiCallResolutionAttempt]: the assembled [call][KaMultiCallResolutionAttempt.call]
 *   if all sub-calls succeeded, or `null` otherwise.
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.successful: KaSimpleOrMultiCall?
    get() = when (this) {
        is KaSimpleCallResolutionSuccess -> call
        is KaSimpleCallResolutionError -> null
        is KaMultiCallResolutionAttempt -> call
    }

/**
 * Whether the resolution succeeded.
 *
 * `true` if and only if [successful] is not `null`, and equivalently if and only if [errors] is empty.
 *
 * Unlike a `this is KaSimpleCallResolutionSuccess` check, which only covers simple attempts, this property also
 * accounts for [KaMultiCallResolutionAttempt], which fails as soon as any of its sub-calls fails.
 *
 * @see successful
 * @see errors
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.isSuccessful: Boolean
    get() = successful != null

/**
 * The former name of [successful].
 *
 * @see successful
 */
@Deprecated(
    message = "Use 'successful' instead",
    replaceWith = ReplaceWith(
        expression = "successful",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.successful"],
    ),
)
@KaExperimentalApi
public val KaCallResolutionAttempt.successfulCall: KaSimpleOrMultiCall?
    get() = successful

/**
 * Folds over a [KaCallResolutionAttempt] depending on whether the resolution succeeded.
 *
 * - [KaSimpleCallResolutionSuccess]: invokes [onSuccess] with the resolved [call][KaSimpleCallResolutionSuccess.call].
 * - [KaSimpleCallResolutionError]: invokes [onFailure] with the error wrapped in a single-element list.
 * - [KaMultiCallResolutionAttempt]: if all sub-calls succeeded, invokes [onSuccess] with the assembled
 *   [call][KaMultiCallResolutionAttempt.call]; otherwise invokes [onFailure] with the [errors] of the failed
 *   sub-calls. The successful sub-calls are not passed to [onFailure]; use [simpleAttempts] to reach them.
 */
@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public inline fun <T> KaCallResolutionAttempt.fold(
    onSuccess: (KaSimpleOrMultiCall) -> T,
    onFailure: (List<KaSimpleCallResolutionError>) -> T,
): T {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }

    val call = successful
    return if (call != null) {
        onSuccess(call)
    } else {
        onFailure(errors)
    }
}
