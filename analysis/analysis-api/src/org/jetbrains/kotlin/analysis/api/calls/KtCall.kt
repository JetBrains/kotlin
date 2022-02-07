/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Call information at call site.
 */
public sealed class KtCallInfo : ValidityTokenOwner

/**
 * Successfully resolved call.
 */
public class KtSuccessCallInfo(private val _call: KtCall) : KtCallInfo() {
    override val token: ValidityToken
        get() = _call.token
    public val call: KtCall get() = withValidityAssertion { _call }
}

/**
 * Call that contains errors.
 */
public class KtErrorCallInfo(
    private val _candidateCalls: List<KtCall>,
    private val _diagnostic: KtDiagnostic,
    override val token: ValidityToken
) : KtCallInfo() {
    public val candidateCalls: List<KtCall> get() = withValidityAssertion { _candidateCalls }
    public val diagnostic: KtDiagnostic get() = withValidityAssertion { _diagnostic }
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
    private val _candidate: KtCall,
    private val _isInBestCandidates: Boolean,
) : ValidityTokenOwner {
    override val token: ValidityToken
        get() = _candidate.token
    public val candidate: KtCall get() = withValidityAssertion { _candidate }

    /**
     * Returns true if the [candidate] is in the final set of candidates that the call is actually resolved to. There can be multiple
     * candidates if the call is ambiguous.
     */
    public val isInBestCandidates: Boolean get() = withValidityAssertion { _isInBestCandidates }
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
    private val _diagnostic: KtDiagnostic,
) : KtCallCandidateInfo(candidate, isInBestCandidates) {
    /**
     * The reason the [candidate] was not applicable for the call (e.g., argument type mismatch, or no value for parameter).
     */
    public val diagnostic: KtDiagnostic get() = withValidityAssertion { _diagnostic }
}

/**
 * A call to a function, a simple/compound access to a property, or a simple/compound access through `get` and `set` convention.
 */
public sealed class KtCall : ValidityTokenOwner

/**
 * A callable symbol partially applied with receivers and type arguments. Essentially, this is a call that misses some information. For
 * properties, the missing information is the type of access (read, write, or compound access) to this property. For functions, the missing
 * information is the value arguments for the call.
 */
public class KtPartiallyAppliedSymbol<out S : KtCallableSymbol, out C : KtSignature<S>>(
    private val _signature: C,
    private val _dispatchReceiver: KtReceiverValue?,
    private val _extensionReceiver: KtReceiverValue?,
) : ValidityTokenOwner {

    override val token: ValidityToken get() = _signature.token

    /**
     * The function or variable (property) declaration.
     */
    public val signature: C get() = withValidityAssertion { _signature }

    /**
     * The dispatch receiver for this symbol access. Dispatch receiver is available if the symbol is declared inside a class or object.
     */
    public val dispatchReceiver: KtReceiverValue? get() = withValidityAssertion { _dispatchReceiver }

    /**
     * The extension receiver for this symbol access. Extension receiver is available if the symbol is declared with an extension receiver.
     */
    public val extensionReceiver: KtReceiverValue? get() = withValidityAssertion { _extensionReceiver }
}

public val <S : KtCallableSymbol, C : KtSignature<S>> KtPartiallyAppliedSymbol<S, C>.symbol: S get() = signature.symbol

/**
 * A synthetic call to assert an expression is not null. For example
 * ```
 * fun test(s: String?) {
 *   s!!.length // Here the receiver is a `KtCheckNotNullCall` with base expression `s`.
 * }
 * ```
 */
public class KtCheckNotNullCall(
    override val token: ValidityToken,
    private val _baseExpression: KtExpression,
) : KtCall() {
    public val baseExpression: KtExpression get() = withValidityAssertion { _baseExpression }
}

/**
 * A call to a function, or a simple/compound access to a property.
 */
public sealed class KtCallableMemberCall<S : KtCallableSymbol, C : KtSignature<S>> : KtCall() {
    public abstract val partiallyAppliedSymbol: KtPartiallyAppliedSymbol<S, C>
}

public val <S : KtCallableSymbol, C : KtSignature<S>> KtCallableMemberCall<S, C>.symbol: S get() = partiallyAppliedSymbol.symbol

public sealed class KtFunctionCall<S : KtFunctionLikeSymbol>(
    private val _argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtCallableMemberCall<S, KtFunctionLikeSignature<S>>() {

    /**
     * The mapping from argument to parameter declaration. In case of vararg parameters, multiple arguments may be mapped to the same
     * `KtValueParameterSymbol`.
     */
    public val argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>> get() = withValidityAssertion { _argumentMapping }
}

public typealias KtPartiallyAppliedFunctionSymbol<S> = KtPartiallyAppliedSymbol<S, KtFunctionLikeSignature<S>>

/**
 * A call to a function.
 */
public class KtSimpleFunctionCall(
    private val _partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionLikeSymbol>,
    argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>,
    private val _isImplicitInvoke: Boolean,
) : KtFunctionCall<KtFunctionLikeSymbol>(argumentMapping) {
    override val token: ValidityToken get() = _partiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionLikeSymbol> get() = withValidityAssertion { _partiallyAppliedSymbol }

    /**
     * Whether this function call is an implicit invoke call on a value that has an `invoke` member function. See
     * https://kotlinlang.org/docs/operator-overloading.html#invoke-operator for more details.
     */
    public val isImplicitInvoke: Boolean get() = withValidityAssertion { _isImplicitInvoke }
}

/**
 * A call to an annotation. For example
 * ```
 * @Deprecated("foo") // call to annotation constructor with single argument `"foo"`.
 * fun foo() {}
 * ```
 */
public class KtAnnotationCall(
    private val _partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol>,
    argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtFunctionCall<KtConstructorSymbol>(argumentMapping) {
    override val token: ValidityToken get() = _partiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol> get() = withValidityAssertion { _partiallyAppliedSymbol }
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
    private val _partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol>,
    private val _kind: Kind,
    argumentMapping: LinkedHashMap<KtExpression, KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtFunctionCall<KtConstructorSymbol>(argumentMapping) {
    override val token: ValidityToken get() = _partiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtConstructorSymbol> get() = withValidityAssertion { _partiallyAppliedSymbol }

    public val kind: Kind get() = withValidityAssertion { _kind }

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
    private val _partiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>,
    private val _simpleAccess: KtSimpleVariableAccess
) : KtVariableAccessCall() {

    override val token: ValidityToken get() = _partiallyAppliedSymbol.token

    override val partiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol> get() = withValidityAssertion { _partiallyAppliedSymbol }

    /**
     * The type of access to this property.
     */
    public val simpleAccess: KtSimpleVariableAccess get() = withValidityAssertion { _simpleAccess }
}

public sealed class KtSimpleVariableAccess {
    public object Read : KtSimpleVariableAccess()

    public class Write(
        /**
         * [KtExpression] that represents the new value that should be assigned to this variable. Or null if the assignment is incomplete
         * and misses the new value.
         */
        public val value: KtExpression?
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
    private val _partiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol>,
    private val _compoundAccess: KtCompoundAccess
) : KtVariableAccessCall(), KtCompoundAccessCall {
    override val token: ValidityToken
        get() = _partiallyAppliedSymbol.token
    override val partiallyAppliedSymbol: KtPartiallyAppliedVariableSymbol<KtVariableLikeSymbol> get() = withValidityAssertion { _partiallyAppliedSymbol }
    override val compoundAccess: KtCompoundAccess get() = withValidityAssertion { _compoundAccess }
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
    private val _compoundAccess: KtCompoundAccess,
    private val _indexArguments: List<KtExpression>,
    private val _getPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
    private val _setPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,

    ) : KtCall(), KtCompoundAccessCall {

    override val token: ValidityToken get() = _compoundAccess.token

    override val compoundAccess: KtCompoundAccess get() = withValidityAssertion { _compoundAccess }

    public val indexArguments: List<KtExpression> get() = withValidityAssertion { _indexArguments }

    /**
     * The `get` function that's invoked when reading values corresponding to the given [indexArguments].
     */
    public val getPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol> get() = withValidityAssertion { _getPartiallyAppliedSymbol }

    /**
     * The `set` function that's invoked when writing values corresponding to the given [indexArguments] and computed value from the
     * operation.
     */
    public val setPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol> get() = withValidityAssertion { _setPartiallyAppliedSymbol }
}

/**
 * The type of access to a variable or using the array access convention.
 */
public sealed class KtCompoundAccess(private val _operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>) :
    ValidityTokenOwner {

    override val token: ValidityToken
        get() = _operationPartiallyAppliedSymbol.token

    /**
     * The function that compute the value for this compound access. For example, if the access is `+=`, this is the resolved `plus`
     * function. If the access is `++`, this is the resolved `inc` function.
     */
    public val operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol> get() = withValidityAssertion { _operationPartiallyAppliedSymbol }

    /**
     * A compound access that read, compute, and write the computed value back. Note that calls to `<op>Assign` is not represented by this.
     */
    public class CompoundAssign(
        operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
        private val _kind: Kind,
        private val _operand: KtExpression
    ) : KtCompoundAccess(operationPartiallyAppliedSymbol) {
        public val kind: Kind get() = withValidityAssertion { _kind }
        public val operand: KtExpression get() = withValidityAssertion { _operand }

        public enum class Kind {
            PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN
        }

    }

    /**
     * A compound access that read, increment or decrement, and write the computed value back.
     */
    public class IncOrDecOperation(
        operationPartiallyAppliedSymbol: KtPartiallyAppliedFunctionSymbol<KtFunctionSymbol>,
        private val _kind: Kind,
        private val _precedence: Precedence,
    ) : KtCompoundAccess(operationPartiallyAppliedSymbol) {
        public val kind: Kind get() = withValidityAssertion { _kind }
        public val precedence: Precedence get() = withValidityAssertion { _precedence }

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
public sealed class KtReceiverValue : ValidityTokenOwner

/**
 * An explicit expression receiver. For example
 * ```
 *   "".length // explicit receiver `""`
 * ```
 */
public class KtExplicitReceiverValue(
    private val _expression: KtExpression,
    private val _isSafeNavigation: Boolean,
    override val token: ValidityToken
) : KtReceiverValue() {
    public val expression: KtExpression get() = withValidityAssertion { _expression }

    /**
     * Whether safe navigation is used on this receiver. For example
     * ```
     * fun test(s1: String?, s2: String) {
     *   s1?.length // explicit receiver `s1` has `isSafeNavigation = true`
     *   s2.length // explicit receiver `s2` has `isSafeNavigation = false`
     * }
     * ```
     */
    public val isSafeNavigation: Boolean get() = withValidityAssertion { _isSafeNavigation }
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
public class KtImplicitReceiverValue(private val _symbol: KtSymbol) : KtReceiverValue() {
    override val token: ValidityToken get() = _symbol.token
    public val symbol: KtSymbol get() = withValidityAssertion { _symbol }
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
public class KtSmartCastedReceiverValue(private val _original: KtReceiverValue, private val _smartCastType: KtType) : KtReceiverValue() {
    override val token: ValidityToken
        get() = _original.token
    public val original: KtReceiverValue get() = withValidityAssertion { _original }
    public val smartCastType: KtType get() = withValidityAssertion { _smartCastType }
}
