/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

/**
 * A call to a function, a simple/compound access to a property, or a simple/compound access through `get` and `set` convention.
 */
public sealed interface KaCall : KaLifetimeOwner

/**
 * A call to a function, or a simple/compound access to a property.
 */
public sealed interface KaCallableMemberCall<S : KaCallableSymbol, C : KaCallableSignature<S>> : KaCall {
    /**
     * A symbol wrapper for the callee, containing a substituted declaration signature (parameter types for functions, return type for
     * functions and properties), and the actual dispatch receiver.
     */
    public val partiallyAppliedSymbol: KaPartiallyAppliedSymbol<S, C>

    /**
     * A map of inferred type arguments. If type placeholders were used, the actual inferred type will be used as a value. The keys for this
     * map are from [partiallyAppliedSymbol]'s type parameters.
     *
     * In case of a resolution or inference error, the map might be empty.
     */
    public val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>
}

/**
 * The [KaCallableSymbol] of the [KaCallableMemberCall]'s callee.
 */
public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaCallableMemberCall<S, C>.symbol: S
    get() = partiallyAppliedSymbol.symbol

/**
 * A call to a function within Kotlin code. This includes calls to regular functions, constructors, constructors of superclasses, and
 * annotations.
 */
public sealed interface KaFunctionCall<S : KaFunctionSymbol> : KaCallableMemberCall<S, KaFunctionSignature<S>> {
    /**
     * A mapping from the call's argument expressions to their associated parameter symbols in a stable order. In case of `vararg`
     * parameters, multiple arguments may be mapped to the same [KaValueParameterSymbol].
     */
    public val argumentMapping: Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>>
}

/**
 * A simple, direct call to a function, without any delegation or special syntax involved.
 */
public interface KaSimpleFunctionCall : KaFunctionCall<KaFunctionSymbol> {
    /**
     * Whether this function call is an [implicit invoke call](https://kotlinlang.org/docs/operator-overloading.html#invoke-operator) on a
     * value that has an `invoke` member function.
     */
    public val isImplicitInvoke: Boolean
}

/**
 * A call to an [annotation constructor](https://kotlinlang.org/docs/annotations.html#constructors).
 *
 * #### Example
 *
 * ```kotlin
 * @Deprecated("foo") // call to the annotation constructor `Foo` with single argument `"foo"`
 * fun foo() {}
 * ```
 */
public interface KaAnnotationCall : KaFunctionCall<KaConstructorSymbol>

/**
 * A call to another constructor within the same class, or to a superclass constructor. This corresponds to the use of `this(...)` or
 * `super(...)` within a constructor's body to delegate initialization to another constructor.
 *
 * #### Example
 *
 * ```kotlin
 * open class SuperClass(i: Int)
 *
 * class SubClass1 : SuperClass(1)      // a call to the constructor of `SuperClass` with a single argument `1`
 *
 * class SubClass2 : SuperClass {
 *   constructor(i: Int) : super(i) {}  // a call to the constructor of `SuperClass` with a single argument `i`
 *   constructor() : this(2) {}         // a call to the constructor of `SubClass2` with a single argument `2`
 * }
 * ```
 */
public interface KaDelegatedConstructorCall : KaFunctionCall<KaConstructorSymbol> {
    /**
     * Determines whether the constructor call is a [`super(...)`][Kind.SUPER_CALL] call or a [`this(...)`][Kind.THIS_CALL] call.
     */
    public val kind: Kind

    public enum class Kind {
        /**
         * A `super(...)` constructor delegation to a superclass constructor.
         */
        SUPER_CALL,

        /**
         * A `this(...)` constructor delegation to another constructor of the same class.
         */
        THIS_CALL,
    }
}

/**
 * An access to variables (including properties).
 */
public sealed interface KaVariableAccessCall : KaCallableMemberCall<KaVariableSymbol, KaVariableSignature<KaVariableSymbol>>

/**
 * A simple read or write to a variable or property.
 */
public interface KaSimpleVariableAccessCall : KaVariableAccessCall {
    /**
     * The kind of access to the variable (read or write), alongside additional information.
     */
    public val simpleAccess: KaSimpleVariableAccess
}

/**
 * A compound access of a [variable][KaCompoundVariableAccessCall] or an [array][KaCompoundArrayAccessCall].
 */
public interface KaCompoundAccessCall {
    /**
     * The corresponding [compound operation][KaCompoundOperation].
     */
    public val compoundOperation: KaCompoundOperation

    @Deprecated("Use `compoundOperation` instead", ReplaceWith("compoundOperation"), level = DeprecationLevel.HIDDEN)
    public val compoundAccess: KaCompoundOperation get() = compoundOperation
}

/**
 * A compound access of a mutable variable. Such accesses combine reading, modifying, and writing to the variable in a single expression,
 * using operators like `+=`, `-=`, `++`, or `--`.
 *
 * #### Example
 *
 * ```kotlin
 * fun test() {
 *   var i = 0
 *   i += 1
 *   // variablePartiallyAppliedSymbol: {
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
 *   // variablePartiallyAppliedSymbol: {
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
 *
 * ### `<op>Assign` function calls
 *
 * If the variable has an [`<op>Assign` operator](https://kotlinlang.org/docs/operator-overloading.html#augmented-assignments), then it's
 * represented as a simple [KaFunctionCall]:
 *
 * ```kotlin
 * fun test(m: MutableList<String>) {
 *   m += "a" //
 * }
 * ```
 *
 * `m += "a"` is a simple `KaFunctionCall` to `MutableList.plusAssign`, not a `KaCompoundVariableAccessCall`. However, the dispatch receiver
 * of this call, `m`, is a simple read access represented as a `KaVariableAccessCall`.
 */
public interface KaCompoundVariableAccessCall : KaCall, KaCompoundAccessCall {
    /**
     * Represents a symbol of the mutated variable.
     */
    public val variablePartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>
}

/**
 * A compound access using the array access convention, involving calls to both the `get()` and `set()` functions. For example,
 * `a[1] += "foo"` is such an array compound access.
 *
 * #### Example
 *
 * ```kotlin
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
 *
 * Such a call always involves both calls to the `get` and `set` functions. With the example above, a call to `String?.plus` is sandwiched
 * between a `get` and a `set` call to compute the new value passed to `set`.
 *
 * ### `<op>Assign` function calls
 *
 * Simple access using the array access convention is not captured by this class. If the collection has an [`<op>Assign` operator](https://kotlinlang.org/docs/operator-overloading.html#augmented-assignments),
 * the call is represented as a simple [KaFunctionCall].
 *
 * For example, assuming `ThrowingMap` throws in case of an absent key instead of returning `null`:
 *
 * ```kotlin
 * fun test(m: ThrowingMap<String, MutableList<String>>) {
 *   m["a"] += "b"
 * }
 * ```
 *
 * The above call is represented as a simple `KaFunctionCall` to `MutableList.plusAssign`, with the dispatch receiver referencing the
 * expression `m["a"]`, which is again a simple `KaFunctionCall` to `ThrowingMap.get`.
 */
public interface KaCompoundArrayAccessCall : KaCall, KaCompoundAccessCall {
    /**
     * The arguments representing the indices in the [index access operator](https://kotlinlang.org/docs/operator-overloading.html#indexed-access-operator)
     * call.
     *
     * #### Example
     *
     * ```kotlin
     * m1["a"] += "b"   // A single index argument `"a"`.
     * m2[1, 5] += 12   // Two index arguments, `1` and `5`.
     * ```
     */
    public val indexArguments: List<KtExpression>

    /**
     * The `get` function that's invoked when reading values corresponding to the given [indexArguments].
     */
    public val getPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>

    /**
     * The `set` function that's invoked when writing values corresponding to the given [indexArguments] and the computed value from the
     * operation.
     */
    public val setPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>
}
