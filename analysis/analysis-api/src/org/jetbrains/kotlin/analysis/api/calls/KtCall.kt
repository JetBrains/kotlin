/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaAnnotationCall
import org.jetbrains.kotlin.analysis.api.resolution.KaApplicableCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundArrayAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaDelegatedConstructorCall
import org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaInapplicableCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedVariableSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
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
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

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

public sealed class KaSimpleVariableAccess {
    public object Read : KaSimpleVariableAccess()

    public class Write(
        /**
         * [KtExpression] that represents the new value that should be assigned to this variable. Or null if the assignment is incomplete
         * and misses the new value.
         */
        public val value: KtExpression?,
    ) : KaSimpleVariableAccess()
}

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

/**
 * The type of access to a variable or using the array access convention.
 */
public sealed class KaCompoundAccess(
    operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>
) : KaLifetimeOwner {
    private val backingOperationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol> = operationPartiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingOperationPartiallyAppliedSymbol.token

    /**
     * The function that compute the value for this compound access. For example, if the access is `+=`, this is the resolved `plus`
     * function. If the access is `++`, this is the resolved `inc` function.
     */
    public val operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol> get() = withValidityAssertion { backingOperationPartiallyAppliedSymbol }

    /**
     * A compound access that read, compute, and write the computed value back. Note that calls to `<op>Assign` is not represented by this.
     */
    public class CompoundAssign(
        operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
        kind: Kind,
        operand: KtExpression,
    ) : KaCompoundAccess(operationPartiallyAppliedSymbol) {
        public val kind: Kind by validityAsserted(kind)
        public val operand: KtExpression by validityAsserted(operand)

        public enum class Kind {
            PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN
        }

    }

    /**
     * A compound access that read, increment or decrement, and write the computed value back.
     */
    public class IncOrDecOperation(
        operationPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
        kind: Kind,
        precedence: Precedence,
    ) : KaCompoundAccess(operationPartiallyAppliedSymbol) {
        public val kind: Kind by validityAsserted(kind)
        public val precedence: Precedence by validityAsserted(precedence)

        public enum class Kind {
            INC, DEC
        }

        public enum class Precedence {
            PREFIX, POSTFIX
        }
    }
}

public typealias KtCompoundAccess = KaCompoundAccess

/**
 * A receiver value of a call.
 */
public sealed class KaReceiverValue : KaLifetimeOwner {
    /**
     * Returns inferred [KaType] of the receiver.
     *
     * In case of smart cast on the receiver returns smart cast type.
     *
     * For builder inference in FIR implementation it currently works incorrectly, see KT-50916.
     */
    public abstract val type: KaType
}

public typealias KtReceiverValue = KaReceiverValue

/**
 * An explicit expression receiver. For example
 * ```
 *   "".length // explicit receiver `""`
 * ```
 */
public class KaExplicitReceiverValue(
    expression: KtExpression,
    type: KaType,
    isSafeNavigation: Boolean,
    override val token: KaLifetimeToken,
) : KaReceiverValue() {
    public val expression: KtExpression by validityAsserted(expression)

    /**
     * Whether safe navigation is used on this receiver. For example
     * ```
     * fun test(s1: String?, s2: String) {
     *   s1?.length // explicit receiver `s1` has `isSafeNavigation = true`
     *   s2.length // explicit receiver `s2` has `isSafeNavigation = false`
     * }
     * ```
     */
    public val isSafeNavigation: Boolean by validityAsserted(isSafeNavigation)

    override val type: KaType by validityAsserted(type)
}

public typealias KtExplicitReceiverValue = KaExplicitReceiverValue

/**
 * An implicit receiver. For example
 * ```
 * class A {
 *   val i: Int = 1
 *   fun test() {
 *     i // implicit receiver bound to class `A`
 *   }
 * }
 *
 * fun String.test() {
 *   length // implicit receiver bound to the `KaReceiverParameterSymbol` of type `String` declared by `test`.
 * }
 * ```
 */
public class KaImplicitReceiverValue(
    symbol: KaSymbol,
    type: KaType,
) : KaReceiverValue() {
    private val backingSymbol: KaSymbol = symbol

    override val token: KaLifetimeToken get() = backingSymbol.token
    public val symbol: KaSymbol get() = withValidityAssertion { backingSymbol }
    override val type: KaType by validityAsserted(type)
}

public typealias KtImplicitReceiverValue = KaImplicitReceiverValue

/**
 * A smart-casted receiver. For example
 * ```
 * fun Any.test() {
 *   if (this is String) {
 *     length // smart-casted implicit receiver bound to the `KaReceiverParameterSymbol` of type `String` declared by `test`.
 *   }
 * }
 * ```
 */
public class KaSmartCastedReceiverValue(
    original: KaReceiverValue,
    smartCastType: KaType,
) : KaReceiverValue() {
    private val backingOriginal: KaReceiverValue = original

    override val token: KaLifetimeToken get() = backingOriginal.token
    public val original: KaReceiverValue get() = withValidityAssertion { backingOriginal }
    public override val type: KaType by validityAsserted(smartCastType)
}

public typealias KtSmartCastedReceiverValue = KaSmartCastedReceiverValue