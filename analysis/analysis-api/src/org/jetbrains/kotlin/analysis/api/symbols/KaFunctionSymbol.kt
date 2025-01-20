/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaContextParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaTypeParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

/**
 * [KaFunctionSymbol] represents a function-like declaration, including named and anonymous functions, constructors, and property accessors.
 */
public sealed class KaFunctionSymbol : KaCallableSymbol() {
    /**
     * The function's value parameters. Value parameters are the "ordinary" parameters found in the
     * [function's parameter list](https://kotlinlang.org/docs/reference/grammar.html#functionDeclaration). They should be differentiated
     * from the [receiverParameter] and [contextReceivers], which are other kinds of parameters.
     */
    public abstract val valueParameters: List<KaValueParameterSymbol>

    /**
     * Whether the function has stable parameter names. Parameters with stable names can be used with the named parameter call syntax (e.g.
     * `User(name = "Joe")` instead of `User("Joe")`).
     *
     * Kotlin functions always have stable parameter names that can be reliably used when calling them with named arguments.
     * Functions loaded from platform definitions (e.g. Java binaries or JS) may have unstable parameter names that vary from
     * one platform installation to another. These names cannot be used reliably for calls with named arguments.
     */
    public abstract val hasStableParameterNames: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaFunctionSymbol>
}

/**
 * [KaAnonymousFunctionSymbol] represents a [lambda or anonymous function declaration](https://kotlinlang.org/docs/lambdas.html#lambda-expressions-and-anonymous-functions).
 *
 * Anonymous functions are always [local][KaSymbolLocation.LOCAL] and have no [callableId] (`null`).
 */
public abstract class KaAnonymousFunctionSymbol : KaFunctionSymbol() {
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.LOCAL }
    final override val callableId: CallableId? get() = withValidityAssertion { null }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }
    final override val hasStableParameterNames: Boolean get() = withValidityAssertion { true }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }

    abstract override fun createPointer(): KaSymbolPointer<KaAnonymousFunctionSymbol>
}

/**
 * [KaSamConstructorSymbol] represents constructors used to build instances of [SAM interfaces](https://kotlinlang.org/docs/fun-interfaces.html#sam-conversions).
 *
 * #### Example
 *
 * ```kotlin
 * fun interface IntPredicate {
 *    fun accept(i: Int): Boolean
 * }
 *
 * val isEven = IntPredicate { it % 2 == 0 }
 * ```
 *
 * The expression `IntPredicate { it % 2 == 0 }` instantiates an object which implements `IntPredicate`. The function used to build this
 * instance is the SAM constructor represented by [KaSamConstructorSymbol], with the following signature:
 *
 * ```kotlin
 * fun IntPredicate(function: Function1<Int, Boolean>): IntPredicate
 * ```
 *
 * @see KaSymbolOrigin.SAM_CONSTRUCTOR
 */
@OptIn(KaImplementationDetail::class)
public abstract class KaSamConstructorSymbol : KaFunctionSymbol(), KaNamedSymbol, KaTypeParameterOwnerSymbol {
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.TOP_LEVEL }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }

    abstract override fun createPointer(): KaSymbolPointer<KaSamConstructorSymbol>
}

/**
 * [KaNamedFunctionSymbol] represents a named [function declaration](https://kotlinlang.org/docs/functions.html), such as a top-level
 * function, a class method, or a named local function.
 */
@OptIn(KaImplementationDetail::class, KaExperimentalApi::class)
public abstract class KaNamedFunctionSymbol : KaFunctionSymbol(), KaNamedSymbol, KaTypeParameterOwnerSymbol, KaContextParameterOwnerSymbol {
    /**
     * Whether the function is a [suspend function](https://kotlinlang.org/spec/asynchronous-programming-with-coroutines.html#suspending-functions).
     */
    public abstract val isSuspend: Boolean

    /**
     * Whether the function is an [operator function](https://kotlinlang.org/docs/operator-overloading.html).
     */
    public abstract val isOperator: Boolean

    /**
     * Whether the function is implemented outside of Kotlin (accessible through [JNI](https://kotlinlang.org/docs/java-interop.html#using-jni-with-kotlin)
     * or [JavaScript](https://kotlinlang.org/docs/js-interop.html#external-modifier)).
     */
    public abstract val isExternal: Boolean

    /**
     * Whether the function is an [inline function](https://kotlinlang.org/docs/inline-functions.html).
     */
    public abstract val isInline: Boolean

    /**
     * Whether the function is an [override method](https://kotlinlang.org/docs/inheritance.html#overriding-methods).
     */
    public abstract val isOverride: Boolean

    /**
     * Whether the function is an [infix function](https://kotlinlang.org/docs/functions.html#infix-notation).
     */
    public abstract val isInfix: Boolean

    /**
     * Whether the function is static. While Kotlin functions cannot be static, the function symbol may represent e.g. a static Java method.
     */
    public abstract val isStatic: Boolean

    /**
     * Whether the function is a [tail-recursive function](https://kotlinlang.org/docs/functions.html#tail-recursive-functions).
     */
    public abstract val isTailRec: Boolean

    /**
     * The list of the function's defined [contract effects](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.contracts/).
     *
     * [contractEffects] is experimental because contracts themselves are an experimental feature.
     */
    @KaExperimentalApi
    public abstract val contractEffects: List<KaContractEffectDeclaration>

    /**
     * Whether this symbol is the `invoke` method defined on the Kotlin builtin functional type.
     */
    public abstract val isBuiltinFunctionInvoke: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaNamedFunctionSymbol>
}

/**
 * [KaConstructorSymbol] represents a class [constructor declaration](https://kotlinlang.org/docs/classes.html#constructors).
 *
 * Constructors do not have a [callableId] (`null`) and cannot have a [receiverParameter] or [contextReceivers].
 */
@OptIn(KaImplementationDetail::class)
public abstract class KaConstructorSymbol : KaFunctionSymbol(), KaTypeParameterOwnerSymbol {
    /**
     * Whether the constructor is the [primary constructor](https://kotlinlang.org/docs/classes.html#constructors) of the class. The primary
     * constructor is declared in the class header, while secondary constructors are declared in the class body.
     */
    public abstract val isPrimary: Boolean

    /**
     * The [ClassId] of the containing class, or `null` if the class is local.
     */
    public abstract val containingClassId: ClassId?

    final override val callableId: CallableId? get() = withValidityAssertion { null }
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.CLASS }
    final override val isExtension: Boolean get() = withValidityAssertion { false }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }

    abstract override fun createPointer(): KaSymbolPointer<KaConstructorSymbol>
}
