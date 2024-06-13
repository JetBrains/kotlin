/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol

/**
 * Call information at call site.
 */
public sealed class KaCallInfo : KaLifetimeOwner

/**
 * Successfully resolved call.
 */
public class KaSuccessCallInfo(private val backingCall: KaCall) : KaCallInfo() {
    override val token: KaLifetimeToken get() = backingCall.token
    public val call: KaCall get() = withValidityAssertion { backingCall }
}

/**
 * Call that contains errors.
 */
public class KaErrorCallInfo(
    candidateCalls: List<KaCall>,
    diagnostic: KaDiagnostic,
    override val token: KaLifetimeToken,
) : KaCallInfo() {
    public val candidateCalls: List<KaCall> by validityAsserted(candidateCalls)
    public val diagnostic: KaDiagnostic by validityAsserted(diagnostic)
}

public val KaCallInfo.calls: List<KaCall>
    get() = when (this) {
        is KaErrorCallInfo -> candidateCalls
        is KaSuccessCallInfo -> listOf(call)
    }

public inline fun <reified T : KaCall> KaCallInfo.singleCallOrNull(): T? {
    return calls.singleOrNull { it is T } as T?
}

public fun KaCallInfo.singleFunctionCallOrNull(): KaFunctionCall<*>? = singleCallOrNull()
public fun KaCallInfo.singleVariableAccessCall(): KaVariableAccessCall? = singleCallOrNull()

@Suppress("UNCHECKED_CAST")
public fun KaCallInfo.singleConstructorCallOrNull(): KaFunctionCall<KaConstructorSymbol>? =
    singleCallOrNull<KaFunctionCall<*>>()?.takeIf { it.symbol is KaConstructorSymbol } as KaFunctionCall<KaConstructorSymbol>?

public inline fun <reified T : KaCall> KaCallInfo.successfulCallOrNull(): T? {
    return (this as? KaSuccessCallInfo)?.call as? T
}

public fun KaCallInfo.successfulFunctionCallOrNull(): KaFunctionCall<*>? = successfulCallOrNull()
public fun KaCallInfo.successfulVariableAccessCall(): KaVariableAccessCall? = successfulCallOrNull()

@Suppress("UNCHECKED_CAST")
public fun KaCallInfo.successfulConstructorCallOrNull(): KaFunctionCall<KaConstructorSymbol>? =
    successfulCallOrNull<KaFunctionCall<*>>()?.takeIf { it.symbol is KaConstructorSymbol } as KaFunctionCall<KaConstructorSymbol>?