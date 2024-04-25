/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Call information at call site.
 */
public sealed class KtCallInfo : KtLifetimeOwner

/**
 * Successfully resolved call.
 */
public class KtSuccessCallInfo(private val backingCall: KtCall) : KtCallInfo() {
    override val token: KtLifetimeToken get() = backingCall.token
    public val call: KtCall get() = withValidityAssertion { backingCall }
}

/**
 * Call that contains errors.
 */
public class KtErrorCallInfo(
    candidateCalls: List<KtCall>,
    diagnostic: KtDiagnostic,
    override val token: KtLifetimeToken,
) : KtCallInfo() {
    public val candidateCalls: List<KtCall> by validityAsserted(candidateCalls)
    public val diagnostic: KtDiagnostic by validityAsserted(diagnostic)
}

public val KtCallInfo.calls: List<KtCall>
    get() = when (this) {
        is KtErrorCallInfo -> candidateCalls
        is KtSuccessCallInfo -> listOf(call)
    }

public inline fun <reified T : KtCall> KtCallInfo.singleCallOrNull(): T? {
    return calls.singleOrNull { it is T } as T?
}

public fun KtCallInfo.singleFunctionCallOrNull(): KtFunctionCall<*>? = singleCallOrNull()
public fun KtCallInfo.singleVariableAccessCall(): KtVariableAccessCall? = singleCallOrNull()

@Suppress("UNCHECKED_CAST")
public fun KtCallInfo.singleConstructorCallOrNull(): KtFunctionCall<KtConstructorSymbol>? =
    singleCallOrNull<KtFunctionCall<*>>()?.takeIf { it.symbol is KtConstructorSymbol } as KtFunctionCall<KtConstructorSymbol>?

public inline fun <reified T : KtCall> KtCallInfo.successfulCallOrNull(): T? {
    return (this as? KtSuccessCallInfo)?.call as? T
}

public fun KtCallInfo.successfulFunctionCallOrNull(): KtFunctionCall<*>? = successfulCallOrNull()
public fun KtCallInfo.successfulVariableAccessCall(): KtVariableAccessCall? = successfulCallOrNull()

@Suppress("UNCHECKED_CAST")
public fun KtCallInfo.successfulConstructorCallOrNull(): KtFunctionCall<KtConstructorSymbol>? =
    successfulCallOrNull<KtFunctionCall<*>>()?.takeIf { it.symbol is KtConstructorSymbol } as KtFunctionCall<KtConstructorSymbol>?

/**
 * A candidate considered for a call. I.e., one of the overload candidates in scope at the call site.
 */
public sealed class KtCallCandidateInfo(
    candidate: KtCall,
    isInBestCandidates: Boolean,
) : KtLifetimeOwner {
    private val backingCandidate: KtCall = candidate

    override val token: KtLifetimeToken get() = backingCandidate.token
    public val candidate: KtCall get() = withValidityAssertion { backingCandidate }

    /**
     * Returns true if the [candidate] is in the final set of candidates that the call is actually resolved to. There can be multiple
     * candidates if the call is ambiguous.
     */
    public val isInBestCandidates: Boolean by validityAsserted(isInBestCandidates)
}

/**
 * A candidate that is applicable for a call. A candidate is applicable if the call's arguments are complete and are assignable to the
 * candidate's parameters, AND the call's type arguments are complete and fit all the constraints of the candidate's type parameters.
 */
public class KtApplicableCallCandidateInfo(
    candidate: KtCall,
    isInBestCandidates: Boolean,
) : KtCallCandidateInfo(candidate, isInBestCandidates)

/**
 * A candidate that is NOT applicable for a call. A candidate is inapplicable if a call argument is missing or is not assignable to the
 * candidate's parameters, OR a call type argument is missing or does not fit the constraints of the candidate's type parameters.
 */
public class KtInapplicableCallCandidateInfo(
    candidate: KtCall,
    isInBestCandidates: Boolean,
    diagnostic: KtDiagnostic,
) : KtCallCandidateInfo(candidate, isInBestCandidates) {
    /**
     * The reason the [candidate] was not applicable for the call (e.g., argument type mismatch, or no value for parameter).
     */
    public val diagnostic: KtDiagnostic by validityAsserted(diagnostic)
}

/**
 * A call to a function, a simple/compound access to a property, or a simple/compound access through `get` and `set` convention.
 */
public sealed class KtCall : KtLifetimeOwner

/**
 * A callable symbol partially applied with receivers and type arguments. Essentially, this is a call that misses some information. For
 * properties, the missing information is the type of access (read, write, or compound access) to this property. For functions, the missing
 * information is the value arguments for the call.
 */
public class KtPartiallyAppliedSymbol<out S : KtCallableSymbol, out C : KtCallableSignature<S>>(
    signature: C,
    dispatchReceiver: KtReceiverValue?,
    extensionReceiver: KtReceiverValue?,
) : KtLifetimeOwner {
    private val backingSignature: C = signature

    override val token: KtLifetimeToken get() = backingSignature.token

    /**
     * The function or variable (property) declaration.
     */
    public val signature: C get() = withValidityAssertion { backingSignature }

    /**
     * The dispatch receiver for this symbol access. Dispatch receiver is available if the symbol is declared inside a class or object.
     */
    public val dispatchReceiver: KtReceiverValue? by validityAsserted(dispatchReceiver)

    /**
     * The extension receiver for this symbol access. Extension receiver is available if the symbol is declared with an extension receiver.
     */
    public val extensionReceiver: KtReceiverValue? by validityAsserted(extensionReceiver)
}

public val <S : KtCallableSymbol, C : KtCallableSignature<S>> KtPartiallyAppliedSymbol<S, C>.symbol: S get() = signature.symbol

/**
 * A call to a function, or a simple/compound access to a property.
 */
public sealed class KtCallableMemberCall<S : KtCallableSymbol, C : KtCallableSignature<S>> : KtCall() {
    public abstract val partiallyAppliedSymbol: KtPartiallyAppliedSymbol<S, C>

    /**
     * This map returns inferred type arguments. If the type placeholders was used, actual inferred type will be used as a value.
     * Keys for this map is from the set [partiallyAppliedSymbol].signature.typeParameters.
     * In case of resolution or inference error could return empty map.
     */
    public abstract val typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType>
}

public val <S : KtCallableSymbol, C : KtCallableSignature<S>> KtCallableMemberCall<S, C>.symbol: S get() = partiallyAppliedSymbol.symbol

public sealed class KtFunctionCall<S : KtFunctionLikeSymbol>(
    argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtCallableMemberCall<S, KtFunctionLikeSignature<S>>() {

    /**
     * The mapping from argument to parameter declaration. In case of vararg parameters, multiple arguments may be mapped to the same
     * `KtValueParameterSymbol`.
     */
    public val argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>
            by validityAsserted(argumentMapping)
}

public typealias KtPartiallyAppliedFunctionSymbol<S> = KtPartiallyAppliedSymbol<S, KtFunctionLikeSignature<S>>

/**
 * A call to a function.
 */
public class KtSimpleFunctionCall(
    partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionLikeSymbol>,
    argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>,
    typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType>,
    isImplicitInvoke: Boolean,
) : KtFunctionCall<KtFunctionLikeSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionLikeSymbol> = partiallyAppliedSymbol

    override val token: KtLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionLikeSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType> by validityAsserted(typeArgumentsMapping)

    /**
     * Whether this function call is an implicit invoke call on a value that has an `invoke` member function. See
     * https://kotlinlang.org/docs/operator-overloading.html#invoke-operator for more details.
     */
    public val isImplicitInvoke: Boolean by validityAsserted(isImplicitInvoke)
}

/**
 * A call to an annotation. For example
 * ```
 * @Deprecated("foo") // call to annotation constructor with single argument `"foo"`.
 * fun foo() {}
 * ```
 */
public class KtAnnotationCall(
    partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol>,
    argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtFunctionCall<KtConstructorSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol> = partiallyAppliedSymbol

    override val token: KtLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType> get() = withValidityAssertion { emptyMap() }
}

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
public class KtDelegatedConstructorCall(
    partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol>,
    kind: Kind,
    argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtFunctionCall<KtConstructorSymbol>(argumentMapping) {
    private val backingPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol> = partiallyAppliedSymbol

    override val token: KtLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType>
        get() = withValidityAssertion {
            check(partiallyAppliedSymbol.symbol.typeParameters.isEmpty()) {
                "Type arguments for delegation constructor to java constructor with type parameters not supported. " +
                        "Symbol: ${partiallyAppliedSymbol.symbol}"
            }
            emptyMap()
        }

    public val kind: Kind by validityAsserted(kind)

    public enum class Kind { SUPER_CALL, THIS_CALL }
}

/**
 * An access to variables (including properties).
 */
public sealed class KtVariableAccessCall : KtCallableMemberCall<KtVariableLikeSymbol, KtVariableLikeSignature<KtVariableLikeSymbol>>()

public typealias KtPartiallyAppliedVariableSymbol<S> = KtPartiallyAppliedSymbol<S, KtVariableLikeSignature<S>>

/**
 * A simple read or write to a variable or property.
 */
public class KtSimpleVariableAccessCall(
    partiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>,
    typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType>,
    simpleAccess: KtSimpleVariableAccess,
) : KtVariableAccessCall() {
    private val backingPartiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol> = partiallyAppliedSymbol

    override val token: KtLifetimeToken get() = backingPartiallyAppliedSymbol.token

    override val partiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType> by validityAsserted(typeArgumentsMapping)

    /**
     * The type of access to this property.
     */
    public val simpleAccess: KtSimpleVariableAccess by validityAsserted(simpleAccess)
}

public sealed class KtSimpleVariableAccess {
    public object Read : KtSimpleVariableAccess()

    public class Write(
        /**
         * [KtExpression] that represents the new value that should be assigned to this variable. Or null if the assignment is incomplete
         * and misses the new value.
         */
        public val value: KtExpression?,
    ) : KtSimpleVariableAccess()
}

public interface KtCompoundAccessCall {
    /**
     * The type of this compound access.
     */
    public val compoundAccess: KtCompoundAccess
}

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
 * Note that if the variable has a `<op>Assign` member, then it's represented as a simple `KtFunctionCall`. For example,
 * ```
 * fun test(m: MutableList<String>) {
 *   m += "a" // A simple `KtFunctionCall` to `MutableList.plusAssign`, not a `KtVariableAccessCall`. However, the dispatch receiver of this
 *            // call, `m`, is a simple read access represented as a `KtVariableAccessCall`
 * }
 * ```
 */
public class KtCompoundVariableAccessCall(
    partiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>,
    typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType>,
    compoundAccess: KtCompoundAccess,
) : KtVariableAccessCall(), KtCompoundAccessCall {
    private val backingPartiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol> = partiallyAppliedSymbol
    override val token: KtLifetimeToken get() = backingPartiallyAppliedSymbol.token

    override val partiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }
    override val typeArgumentsMapping: Map<KtTypeParameterSymbol, KtType> by validityAsserted(typeArgumentsMapping)
    override val compoundAccess: KtCompoundAccess by validityAsserted(compoundAccess)
}

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
 * The above call is represented as a simple `KtFunctionCall` to `MutableList.plusAssign`, with the dispatch receiver referencing the
 * `m["a"]`, which is again a simple `KtFunctionCall` to `ThrowingMap.get`.
 */
public class KtCompoundArrayAccessCall(
    compoundAccess: KtCompoundAccess,
    indexArguments: List<KtExpression>,
    getPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
    setPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
) : KtCall(), KtCompoundAccessCall {
    private val backingCompoundAccess: KtCompoundAccess = compoundAccess

    override val token: KtLifetimeToken get() = backingCompoundAccess.token

    override val compoundAccess: KtCompoundAccess get() = withValidityAssertion { backingCompoundAccess }

    public val indexArguments: List<KtExpression> by validityAsserted(indexArguments)

    /**
     * The `get` function that's invoked when reading values corresponding to the given [indexArguments].
     */
    public val getPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol> by validityAsserted(getPartiallyAppliedSymbol)

    /**
     * The `set` function that's invoked when writing values corresponding to the given [indexArguments] and computed value from the
     * operation.
     */
    public val setPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol> by validityAsserted(setPartiallyAppliedSymbol)
}

/**
 * The type of access to a variable or using the array access convention.
 */
public sealed class KtCompoundAccess(
    operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>
) : KtLifetimeOwner {
    private val backingOperationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol> = operationPartiallyAppliedSymbol

    override val token: KtLifetimeToken get() = backingOperationPartiallyAppliedSymbol.token

    /**
     * The function that compute the value for this compound access. For example, if the access is `+=`, this is the resolved `plus`
     * function. If the access is `++`, this is the resolved `inc` function.
     */
    public val operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol> get() = withValidityAssertion { backingOperationPartiallyAppliedSymbol }

    /**
     * A compound access that read, compute, and write the computed value back. Note that calls to `<op>Assign` is not represented by this.
     */
    public class CompoundAssign(
        operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
        kind: Kind,
        operand: KtExpression,
    ) : KtCompoundAccess(operationPartiallyAppliedSymbol) {
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
        operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
        kind: Kind,
        precedence: Precedence,
    ) : KtCompoundAccess(operationPartiallyAppliedSymbol) {
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

/**
 * A receiver value of a call.
 */
public sealed class KtReceiverValue : KtLifetimeOwner {
    /**
     * Returns inferred [KtType] of the receiver.
     *
     * In case of smart cast on the receiver returns smart cast type.
     *
     * For builder inference in FIR implementation it currently works incorrectly, see KT-50916.
     */
    public abstract val type: KtType
}

/**
 * An explicit expression receiver. For example
 * ```
 *   "".length // explicit receiver `""`
 * ```
 */
public class KtExplicitReceiverValue(
    expression: KtExpression,
    type: KtType,
    isSafeNavigation: Boolean,
    override val token: KtLifetimeToken,
) : KtReceiverValue() {
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

    override val type: KtType by validityAsserted(type)
}

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
 *   length // implicit receiver bound to the `KtReceiverParameterSymbol` of type `String` declared by `test`.
 * }
 * ```
 */
public class KtImplicitReceiverValue(
    symbol: KtSymbol,
    type: KtType,
) : KtReceiverValue() {
    private val backingSymbol: KtSymbol = symbol

    override val token: KtLifetimeToken get() = backingSymbol.token
    public val symbol: KtSymbol get() = withValidityAssertion { backingSymbol }
    override val type: KtType by validityAsserted(type)
}

/**
 * A smart-casted receiver. For example
 * ```
 * fun Any.test() {
 *   if (this is String) {
 *     length // smart-casted implicit receiver bound to the `KtReceiverParameterSymbol` of type `String` declared by `test`.
 *   }
 * }
 * ```
 */
public class KtSmartCastedReceiverValue(
    original: KtReceiverValue,
    smartCastType: KtType,
) : KtReceiverValue() {
    private val backingOriginal: KtReceiverValue = original

    override val token: KtLifetimeToken get() = backingOriginal.token
    public val original: KtReceiverValue get() = withValidityAssertion { backingOriginal }
    public override val type: KtType by validityAsserted(smartCastType)
}
