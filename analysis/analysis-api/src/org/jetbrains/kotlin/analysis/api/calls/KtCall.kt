/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.resolution.KaAnnotationCall
import org.jetbrains.kotlin.analysis.api.resolution.KaApplicableCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundAccess
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundArrayAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaDelegatedConstructorCall
import org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaInapplicableCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedVariableSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccess
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSmartCastedReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.calls
import org.jetbrains.kotlin.analysis.api.resolution.singleCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.singleConstructorCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.singleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulConstructorCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaCallInfo = KaCallInfo
public typealias KtCallInfo = KaCallInfo

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaSuccessCallInfo = KaSuccessCallInfo
public typealias KtSuccessCallInfo = KaSuccessCallInfo

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaErrorCallInfo = KaErrorCallInfo
public typealias KtErrorCallInfo = KaErrorCallInfo

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
public typealias KaCallCandidateInfo = KaCallCandidateInfo
public typealias KtCallCandidateInfo = KaCallCandidateInfo

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaApplicableCallCandidateInfo = KaApplicableCallCandidateInfo
public typealias KtApplicableCallCandidateInfo = KaApplicableCallCandidateInfo

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaInapplicableCallCandidateInfo = KaInapplicableCallCandidateInfo
public typealias KtInapplicableCallCandidateInfo = KaInapplicableCallCandidateInfo

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaCall = KaCall
public typealias KtCall = KaCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaPartiallyAppliedSymbol<S, C> = KaPartiallyAppliedSymbol<S, C>
public typealias KtPartiallyAppliedSymbol<S, C> = KaPartiallyAppliedSymbol<S, C>

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaPartiallyAppliedSymbol<S, C>.symbol: S get() = symbol

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaCallableMemberCall<S, C> = KaCallableMemberCall<S, C>
public typealias KtCallableMemberCall<S, C> = KaCallableMemberCall<S, C>

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaCallableMemberCall<S, C>.symbol: S get() = symbol

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaFunctionCall<S> = KaFunctionCall<S>
public typealias KtFunctionCall<S> = KaFunctionCall<S>

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaPartiallyAppliedFunctionSymbol<S> = KaPartiallyAppliedFunctionSymbol<S>
public typealias KtPartiallyAppliedFunctionSymbol<S> = KaPartiallyAppliedFunctionSymbol<S>

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaSimpleFunctionCall = KaSimpleFunctionCall
public typealias KtSimpleFunctionCall = KaSimpleFunctionCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaAnnotationCall = KaAnnotationCall
public typealias KtAnnotationCall = KaAnnotationCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaDelegatedConstructorCall = KaDelegatedConstructorCall
public typealias KtDelegatedConstructorCall = KaDelegatedConstructorCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaVariableAccessCall = KaVariableAccessCall
public typealias KtVariableAccessCall = KaVariableAccessCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaPartiallyAppliedVariableSymbol<S> = KaPartiallyAppliedVariableSymbol<S>
public typealias KtPartiallyAppliedVariableSymbol<S> = KaPartiallyAppliedVariableSymbol<S>

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaSimpleVariableAccessCall = KaSimpleVariableAccessCall
public typealias KtSimpleVariableAccessCall = KaSimpleVariableAccessCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaSimpleVariableAccess = KaSimpleVariableAccess
public typealias KtSimpleVariableAccess = KaSimpleVariableAccess

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaCompoundAccessCall = KaCompoundAccessCall
public typealias KtCompoundAccessCall = KaCompoundAccessCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaCompoundVariableAccessCall = KaCompoundVariableAccessCall
public typealias KtCompoundVariableAccessCall = KaCompoundVariableAccessCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaCompoundArrayAccessCall = KaCompoundArrayAccessCall
public typealias KtCompoundArrayAccessCall = KaCompoundArrayAccessCall

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaCompoundAccess = KaCompoundAccess
public typealias KtCompoundAccess = KaCompoundAccess

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaReceiverValue = KaReceiverValue
public typealias KtReceiverValue = KaReceiverValue

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaExplicitReceiverValue = KaExplicitReceiverValue
public typealias KtExplicitReceiverValue = KaExplicitReceiverValue

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaImplicitReceiverValue = KaImplicitReceiverValue
public typealias KtImplicitReceiverValue = KaImplicitReceiverValue

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaSmartCastedReceiverValue = KaSmartCastedReceiverValue
public typealias KtSmartCastedReceiverValue = KaSmartCastedReceiverValue