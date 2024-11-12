/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public val KaCallInfo.calls: List<KaCall> get() = calls

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public inline fun <reified T : KaCall> KaCallInfo.singleCallOrNull(): T? = singleCallOrNull()

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public fun KaCallInfo.singleFunctionCallOrNull(): KaFunctionCall<*>? = singleFunctionCallOrNull()

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public fun KaCallInfo.singleVariableAccessCall(): KaVariableAccessCall? = singleVariableAccessCall()

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public fun KaCallInfo.singleConstructorCallOrNull(): KaFunctionCall<KaConstructorSymbol>? = singleConstructorCallOrNull()

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public inline fun <reified T : KaCall> KaCallInfo.successfulCallOrNull(): T? = successfulCallOrNull()

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public fun KaCallInfo.successfulFunctionCallOrNull(): KaFunctionCall<*>? = successfulFunctionCallOrNull()

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public fun KaCallInfo.successfulVariableAccessCall(): KaVariableAccessCall? = successfulVariableAccessCall()

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public fun KaCallInfo.successfulConstructorCallOrNull(): KaFunctionCall<KaConstructorSymbol>? = successfulConstructorCallOrNull()

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaPartiallyAppliedSymbol<S, C>.symbol: S get() = symbol

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaCallableMemberCall<S, C>.symbol: S get() = symbol
