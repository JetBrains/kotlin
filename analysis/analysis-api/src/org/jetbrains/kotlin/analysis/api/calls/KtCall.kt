/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Call information at call site.
 */
public sealed class KaCallInfo : KaLifetimeOwner

public typealias KtCallInfo = KaCallInfo

/**
 * Successfully resolved call.
 */
public class KaSuccessCallInfo(private val backingCall: KaCall) : KaCallInfo() {
    override val token: KaLifetimeToken get() = backingCall.token
    public val call: KaCall get() = withValidityAssertion { backingCall }
}

public typealias KtSuccessCallInfo = KaSuccessCallInfo

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

public typealias KtErrorCallInfo = KaErrorCallInfo

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

/**
 * A candidate considered for a call. I.e., one of the overload candidates in scope at the call site.
 */
public sealed class KaCallCandidateInfo(
    candidate: KaCall,
    isInBestCandidates: Boolean,
) : KaLifetimeOwner {
    private val backingCandidate: KaCall = candidate

    override val token: KaLifetimeToken get() = backingCandidate.token
    public val candidate: KaCall get() = withValidityAssertion { backingCandidate }

    /**
     * Returns true if the [candidate] is in the final set of candidates that the call is actually resolved to. There can be multiple
     * candidates if the call is ambiguous.
     */
    public val isInBestCandidates: Boolean by validityAsserted(isInBestCandidates)
}

public typealias KtCallCandidateInfo = KaCallCandidateInfo

/**
 * A candidate that is applicable for a call. A candidate is applicable if the call's arguments are complete and are assignable to the
 * candidate's parameters, AND the call's type arguments are complete and fit all the constraints of the candidate's type parameters.
 */
public class KaApplicableCallCandidateInfo(
    candidate: KaCall,
    isInBestCandidates: Boolean,
) : KaCallCandidateInfo(candidate, isInBestCandidates)

public typealias KtApplicableCallCandidateInfo = KaApplicableCallCandidateInfo

/**
 * A candidate that is NOT applicable for a call. A candidate is inapplicable if a call argument is missing or is not assignable to the
 * candidate's parameters, OR a call type argument is missing or does not fit the constraints of the candidate's type parameters.
 */
public class KaInapplicableCallCandidateInfo(
    candidate: KaCall,
    isInBestCandidates: Boolean,
    diagnostic: KaDiagnostic,
) : KaCallCandidateInfo(candidate, isInBestCandidates) {
    /**
     * The reason the [candidate] was not applicable for the call (e.g., argument type mismatch, or no value for parameter).
     */
    public val diagnostic: KaDiagnostic by validityAsserted(diagnostic)
}

public typealias KtInapplicableCallCandidateInfo = KaInapplicableCallCandidateInfo

/**
 * A call to a function, a simple/compound access to a property, or a simple/compound access through `get` and `set` convention.
 */
public sealed class KaCall : KaLifetimeOwner

public typealias KtCall = KaCall

/**
 * A callable symbol partially applied with receivers and type arguments. Essentially, this is a call that misses some information. For
 * properties, the missing information is the type of access (read, write, or compound access) to this property. For functions, the missing
 * information is the value arguments for the call.
 */
public class KaPartiallyAppliedSymbol<out S : KaCallableSymbol, out C : KaCallableSignature<S>>(
    signature: C,
    dispatchReceiver: KaReceiverValue?,
    extensionReceiver: KaReceiverValue?,
) : KaLifetimeOwner {
    private val backingSignature: C = signature

    override val token: KaLifetimeToken get() = backingSignature.token

    /**
     * The function or variable (property) declaration.
     */
    public val signature: C get() = withValidityAssertion { backingSignature }

    /**
     * The dispatch receiver for this symbol access. Dispatch receiver is available if the symbol is declared inside a class or object.
     */
    public val dispatchReceiver: KaReceiverValue? by validityAsserted(dispatchReceiver)

    /**
     * The extension receiver for this symbol access. Extension receiver is available if the symbol is declared with an extension receiver.
     */
    public val extensionReceiver: KaReceiverValue? by validityAsserted(extensionReceiver)
}

public typealias KtPartiallyAppliedSymbol<S, C> = KaPartiallyAppliedSymbol<S, C>

public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaPartiallyAppliedSymbol<S, C>.symbol: S get() = signature.symbol

/**
 * A call to a function, or a simple/compound access to a property.
 */
public sealed class KaCallableMemberCall<S : KaCallableSymbol, C : KaCallableSignature<S>> : KaCall() {
    public abstract val partiallyAppliedSymbol: KaPartiallyAppliedSymbol<S, C>

    /**
     * This map returns inferred type arguments. If the type placeholders was used, actual inferred type will be used as a value.
     * Keys for this map is from the set [partiallyAppliedSymbol].signature.typeParameters.
     * In case of resolution or inference error could return empty map.
     */
    public abstract val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>
}

public typealias KtCallableMemberCall<S, C> = KaCallableMemberCall<S, C>

public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaCallableMemberCall<S, C>.symbol: S get() = partiallyAppliedSymbol.symbol

public sealed class KaFunctionCall<S : KaFunctionLikeSymbol>(
    argumentMapping: LinkedHashMap<KtExpression, KaVariableLikeSignature<KaValueParameterSymbol>>,
) : KaCallableMemberCall<S, KaFunctionLikeSignature<S>>() {

    /**
     * The mapping from argument to parameter declaration. In case of vararg parameters, multiple arguments may be mapped to the same
     * [KaValueParameterSymbol].
     */
    public val argumentMapping: LinkedHashMap<KtExpression, KaVariableLikeSignature<KaValueParameterSymbol>>
            by validityAsserted(argumentMapping)
}

public typealias KtFunctionCall<S> = KaFunctionCall<S>

public typealias KaPartiallyAppliedFunctionSymbol<S> = KaPartiallyAppliedSymbol<S, KaFunctionLikeSignature<S>>

public typealias KtPartiallyAppliedFunctionSymbol<S> = KaPartiallyAppliedFunctionSymbol<S>

/**
 * A call to a function.
 */
public class KaSimpleFunctionCall(
    partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionLikeSymbol>,
    argumentMapping: LinkedHashMap<KtExpression, KaVariableLikeSignature<KaValueParameterSymbol>>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    isImplicitInvoke: Boolean,
) : KaFunctionCall<KaFunctionLikeSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionLikeSymbol> = partiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionLikeSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)

    /**
     * Whether this function call is an implicit invoke call on a value that has an `invoke` member function. See
     * https://kotlinlang.org/docs/operator-overloading.html#invoke-operator for more details.
     */
    public val isImplicitInvoke: Boolean by validityAsserted(isImplicitInvoke)
}

public typealias KtSimpleFunctionCall = KaSimpleFunctionCall

/**
 * A call to an annotation. For example
 * ```
 * @Deprecated("foo") // call to annotation constructor with single argument `"foo"`.
 * fun foo() {}
 * ```
 */
public class KaAnnotationCall(
    partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
    argumentMapping: LinkedHashMap<KtExpression, KaVariableLikeSignature<KaValueParameterSymbol>>,
) : KaFunctionCall<KaConstructorSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> = partiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> get() = withValidityAssertion { emptyMap() }
}

public typealias KtAnnotationCall = KaAnnotationCall

/**
 * A delegated call to constructors. For example
 * ```
 * open class SuperClass(i: Int)
 * class SubClass1: SuperClass(1) // a call to constructor of `SuperClass` with single argument `1`
 * class SubClass2 : SuperClass {
 *   constructor(i: Int): super(i) {} // a call to constructor of `SuperClass` with single argument `i`
 *   constructor(): this(2) {} // a call to constructor of `SubClass2` with single argument `2`.
 * }
 * ```
 */
public class KaDelegatedConstructorCall(
    partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
    kind: Kind,
    argumentMapping: LinkedHashMap<KtExpression, KaVariableLikeSignature<KaValueParameterSymbol>>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
) : KaFunctionCall<KaConstructorSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> = partiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)

    public val kind: Kind by validityAsserted(kind)

    public enum class Kind { SUPER_CALL, THIS_CALL }
}

public typealias KtDelegatedConstructorCall = KaDelegatedConstructorCall

/**
 * An access to variables (including properties).
 */
public sealed class KaVariableAccessCall : KaCallableMemberCall<KaVariableLikeSymbol, KaVariableLikeSignature<KaVariableLikeSymbol>>()

public typealias KtVariableAccessCall = KaVariableAccessCall

public typealias KaPartiallyAppliedVariableSymbol<S> = KaPartiallyAppliedSymbol<S, KaVariableLikeSignature<S>>

public typealias KtPartiallyAppliedVariableSymbol<S> = KaPartiallyAppliedVariableSymbol<S>

/**
 * A simple read or write to a variable or property.
 */
public class KaSimpleVariableAccessCall(
    partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableLikeSymbol>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    simpleAccess: KaSimpleVariableAccess,
) : KaVariableAccessCall() {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableLikeSymbol> = partiallyAppliedSymbol

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    override val partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableLikeSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)

    /**
     * The type of access to this property.
     */
    public val simpleAccess: KaSimpleVariableAccess by validityAsserted(simpleAccess)
}

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

public interface KaCompoundAccessCall {
    /**
     * The type of this compound access.
     */
    public val compoundAccess: KaCompoundAccess
}

public typealias KtCompoundAccessCall = KaCompoundAccessCall

/**
 * A compound access of a mutable variable.  For example
 * ```
 * fun test() {
 *   var i = 0
 *   i += 1
 *   // partiallyAppliedSymbol: {
 *   //   symbol: `i`
 *   //   dispatchReceiver: null
 *   //   extensionReceiver: null
 *   // }
 *   // accessType: OpAssign {
 *   //   kind: PLUS
 *   //   operand: 1
 *   //   operationSymbol: Int.plus()
 *   // }
 *
 *   i++
 *   // partiallyAppliedSymbol: {
 *   //   symbol: `i`
 *   //   dispatchReceiver: null
 *   //   extensionReceiver: null
 *   // }
 *   // accessType: IncDec {
 *   //   kind: INC
 *   //   precedence: POSTFIX
 *   //   operationSymbol: Int.inc()
 *   // }
 * }
 * ```
 * Note that if the variable has a `<op>Assign` member, then it's represented as a simple `KaFunctionCall`. For example,
 * ```
 * fun test(m: MutableList<String>) {
 *   m += "a" // A simple `KaFunctionCall` to `MutableList.plusAssign`, not a `KaVariableAccessCall`. However, the dispatch receiver of this
 *            // call, `m`, is a simple read access represented as a `KaVariableAccessCall`
 * }
 * ```
 */
public class KaCompoundVariableAccessCall(
    partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableLikeSymbol>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    compoundAccess: KaCompoundAccess,
) : KaVariableAccessCall(), KaCompoundAccessCall {
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableLikeSymbol> = partiallyAppliedSymbol
    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    override val partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableLikeSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }
    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)
    override val compoundAccess: KaCompoundAccess by validityAsserted(compoundAccess)
}

public typealias KtCompoundVariableAccessCall = KaCompoundVariableAccessCall

/**
 * A compound access using the array access convention. For example,
 * ```
 * fun test(m: MutableMap<String, String>) {
 *   m["a"] += "b"
 *   // indexArguments: ["a"]
 *   // getPartiallyAppliedSymbol: {
 *   //   symbol: MutableMap.get()
 *   //   dispatchReceiver: `m`
 *   //   extensionReceiver: null
 *   // }
 *   // setPartiallyAppliedSymbol: {
 *   //   symbol: MutableMap.set()
 *   //   dispatchReceiver: `m`
 *   //   extensionReceiver: null
 *   // }
 *   // accessType: OpAssign {
 *   //   kind: PLUS
 *   //   operand: "b"
 *   //   operationSymbol: String?.plus()
 *   // }
 * }
 * ```
 * Such a call always involve both calls to `get` and `set` functions. With the example above, a call to `String?.plus` is sandwiched
 * between `get` and `set` call to compute the new value passed to `set`.
 *
 * Note that simple access using the array access convention is not captured by this class. For example, assuming `ThrowingMap` throws
 * in case of absent key instead of returning `null`,
 * ```
 * fun test(m: ThrowingMap<String, MutableList<String>>) {
 *   m["a"] += "b"
 * }
 * ```
 * The above call is represented as a simple `KaFunctionCall` to `MutableList.plusAssign`, with the dispatch receiver referencing the
 * `m["a"]`, which is again a simple `KaFunctionCall` to `ThrowingMap.get`.
 */
public class KaCompoundArrayAccessCall(
    compoundAccess: KaCompoundAccess,
    indexArguments: List<KtExpression>,
    getPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
    setPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
) : KaCall(), KaCompoundAccessCall {
    private val backingCompoundAccess: KaCompoundAccess = compoundAccess

    override val token: KaLifetimeToken get() = backingCompoundAccess.token

    override val compoundAccess: KaCompoundAccess get() = withValidityAssertion { backingCompoundAccess }

    public val indexArguments: List<KtExpression> by validityAsserted(indexArguments)

    /**
     * The `get` function that's invoked when reading values corresponding to the given [indexArguments].
     */
    public val getPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol> by validityAsserted(getPartiallyAppliedSymbol)

    /**
     * The `set` function that's invoked when writing values corresponding to the given [indexArguments] and computed value from the
     * operation.
     */
    public val setPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol> by validityAsserted(setPartiallyAppliedSymbol)
}

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