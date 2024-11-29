/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol

/**
 * Information about a call at the call site retrieved from [resolveToCall][org.jetbrains.kotlin.analysis.api.components.KaResolver.resolveToCall].
 * The call may either be resolved successfully ([KaSuccessCallInfo]), or with errors ([KaErrorCallInfo]), coming with a list of candidate
 * calls and a diagnostic.
 */
public sealed interface KaCallInfo : KaLifetimeOwner

/**
 * A successfully resolved call.
 */
public interface KaSuccessCallInfo : KaCallInfo {
    /**
     * The successfully resolved [KaCall].
     */
    public val call: KaCall
}

/**
 * An erroneous call. The [candidateCalls] and [diagnostic] can be used to further analyze the error.
 */
public interface KaErrorCallInfo : KaCallInfo {
    /**
     * A list of [KaCall]s to candidates that were considered during the call resolution process, but ultimately not selected. This may be
     * due to various errors. For example, an ambiguity results in an error call with multiple candidates.
     *
     * An error call is not guaranteed to have any candidates.
     */
    public val candidateCalls: List<KaCall>

    /**
     * The [KaDiagnostic] describing the error.
     */
    public val diagnostic: KaDiagnostic
}

/**
 * The list of [KaCall]s associated with the [KaCallInfo]. The list contains a single [KaCall] in case of a
 * [successful call][KaSuccessCallInfo], but may contain multiple candidates in case of an [error call][KaErrorCallInfo].
 */
public val KaCallInfo.calls: List<KaCall>
    get() = when (this) {
        is KaErrorCallInfo -> candidateCalls
        is KaSuccessCallInfo -> listOf(call)
    }

/**
 * Returns the single [KaCall] of type [T] associated with the [KaCallInfo], or `null` if there is no such exact single call.
 *
 * In the case of an [error call][KaErrorCallInfo], returns a single [candidate call][KaErrorCallInfo.candidateCalls] of type [T].
 */
public inline fun <reified T : KaCall> KaCallInfo.singleCallOrNull(): T? {
    return calls.singleOrNull { it is T } as T?
}

/**
 * Returns the single [KaFunctionCall] associated with the [KaCallInfo], or `null` if there is no such exact single call.
 *
 * @see singleCallOrNull
 */
public fun KaCallInfo.singleFunctionCallOrNull(): KaFunctionCall<*>? = singleCallOrNull()

/**
 * Returns the single [KaVariableAccessCall] associated with the [KaCallInfo], or `null` if there is no such exact single call.
 *
 * @see singleCallOrNull
 */
public fun KaCallInfo.singleVariableAccessCall(): KaVariableAccessCall? = singleCallOrNull()

/**
 * Returns the single [KaFunctionCall] with a [KaConstructorSymbol] associated with the [KaCallInfo], or `null` if there is no such exact
 * single call.
 *
 * @see singleCallOrNull
 */
@Suppress("UNCHECKED_CAST")
public fun KaCallInfo.singleConstructorCallOrNull(): KaFunctionCall<KaConstructorSymbol>? =
    singleCallOrNull<KaFunctionCall<*>>()?.takeIf { it.symbol is KaConstructorSymbol } as KaFunctionCall<KaConstructorSymbol>?

/**
 * Returns the successful [KaCall] of type [T] associated with the [KaCallInfo], or `null` if there is no such exact call (either the call
 * is not successful, or the successful call is of another type).
 */
public inline fun <reified T : KaCall> KaCallInfo.successfulCallOrNull(): T? {
    return (this as? KaSuccessCallInfo)?.call as? T
}

/**
 * Returns the successful [KaFunctionCall] associated with the [KaCallInfo], or `null` if there is no such exact call.
 *
 * @see successfulCallOrNull
 */
public fun KaCallInfo.successfulFunctionCallOrNull(): KaFunctionCall<*>? = successfulCallOrNull()

/**
 * Returns the successful [KaVariableAccessCall] associated with the [KaCallInfo], or `null` if there is no such exact call.
 *
 * @see successfulCallOrNull
 */
public fun KaCallInfo.successfulVariableAccessCall(): KaVariableAccessCall? = successfulCallOrNull()

/**
 * Returns the successful [KaFunctionCall] with a [KaConstructorSymbol] associated with the [KaCallInfo], or `null` if there is no such
 * exact call.
 *
 * @see successfulCallOrNull
 */
@Suppress("UNCHECKED_CAST")
public fun KaCallInfo.successfulConstructorCallOrNull(): KaFunctionCall<KaConstructorSymbol>? =
    successfulCallOrNull<KaFunctionCall<*>>()?.takeIf { it.symbol is KaConstructorSymbol } as KaFunctionCall<KaConstructorSymbol>?
