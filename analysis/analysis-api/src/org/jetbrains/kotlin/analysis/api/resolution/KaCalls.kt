/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:[JvmName("KaCalls") JvmMultifileClass]

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.resolution.KtResolvableCall

/**
 * Represents a resolved call that can be either a simple call or a multi call.
 *
 * It can be either a [KaSimpleCall] or a [KaMultiCall].
 *
 * ### Example
 * ```kotlin
 * class Foo {
 *    fun function() {}
 *    var int: Int = 1
 * }
 *
 * fun Foo.usage() {
 *    function()
 *    int++
 * }
 * ```
 *
 * `function()` call will be represented as a [KaSimpleCall] (the target is the function),
 * and `int++` call as a [KaMultiCall] (with two targets: the `int` property and the `++` operator function)
 *
 * @see KaSimpleCall
 * @see KaMultiCall
 */
@KaExperimentalApi
public sealed interface KaSimpleOrMultiCall : KaLifetimeOwner

/**
 * The former name of [KaSimpleOrMultiCall].
 *
 * @see KaSimpleOrMultiCall
 */
@Deprecated(
    message = "Use 'KaSimpleOrMultiCall' instead",
    replaceWith = ReplaceWith(
        expression = "KaSimpleOrMultiCall",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaSimpleOrMultiCall"],
    ),
)
@KaExperimentalApi
public typealias KaSingleOrMultiCall = KaSimpleOrMultiCall

/**
 * Represents a successful resolution resulting in a simple call.
 *
 * ### Example
 * ```kotlin
 * class Foo {
 *    fun function() {}
 * }
 *
 * fun Foo.usage() {
 *    function()
 * }
 * ```
 *
 * `function()` call will be represented as a [KaSimpleCall]
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSimpleCall<S : KaCallableSymbol, C : KaCallableSignature<S>> : KaSimpleOrMultiCall {
    /**
     * The function or variable declaration.
     */
    public val signature: C

    /**
     * The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) for this symbol access. A dispatch
     * receiver is available if the callable is declared inside a class or object.
     */
    public val dispatchReceiver: KaReceiverValue?

    /**
     * The [extension receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) for this symbol access. An
     * extension receiver is available if the callable is declared with an extension receiver.
     */
    public val extensionReceiver: KaReceiverValue?

    /**
     * The list of [context parameters](https://github.com/Kotlin/KEEP/issues/367) for this symbol access.
     * The list is available if the callable is declared with context parameters.
     */
    public val contextArguments: List<KaReceiverValue>

    /**
     * A map of inferred type arguments. If type placeholders were used, the actual inferred type will be used as a value. The keys for this
     * map are from [signature]'s type parameters.
     *
     * In case of a resolution or inference error, the map might be empty.
     */
    public val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>
}

/**
 * The former name of [KaSimpleCall].
 *
 * @see KaSimpleCall
 */
@Deprecated(
    message = "Use 'KaSimpleCall' instead",
    replaceWith = ReplaceWith(
        expression = "KaSimpleCall<S, C>",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaSimpleCall"],
    ),
)
@KaExperimentalApi
public typealias KaSingleCall<S, C> = KaSimpleCall<S, C>

/**
 * Represents a successful resolution resulting in multiple calls.
 *
 * ### Example
 * ```kotlin
 * var int: Int = 1
 *
 * fun usage() {
 *    int++
 * }
 * ```
 *
 * `int++` call will be represented as a [KaMultiCall] with two [KaSimpleCall]s inside: one for the `int` property
 * and one for the `++` operator function ([Int.inc])
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 */
@KaExperimentalApi
public sealed interface KaMultiCall : KaSimpleOrMultiCall {
    /**
     * The non-empty list of [KaSimpleCall]s that were discovered during resolution of [KtResolvableCall]
     */
    @KaExperimentalApi
    public val calls: List<KaSimpleCall<*, *>>
}

/**
 * [KaMultiCall] represent a bunch of unrelated compound calls,
 * so the client typically is not expected to handle all possible cases.
 *
 * The usual way to work with compound calls is to get them using a special [KaResolver.resolveCall] overload
 */
@Suppress("unused")
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
private interface KaMultiUnknownCall : KaMultiCall

/**
 * The flattened list of [KaSimpleCall]s.
 *
 * - If [this] is an instance of [KaSimpleCall], the list will contain only [this] call
 * - If [this] is an instance of [KaMultiCall], the list will contain [KaMultiCall.calls]
 */
@KaExperimentalApi
public val KaSimpleOrMultiCall.calls: List<KaSimpleCall<*, *>>
    get() = when (this) {
        is KaSimpleCall<*, *> -> listOf(this)
        is KaMultiCall -> calls
    }

/**
 * The flattened list of [KaSymbol]s for the resolved calls.
 *
 * - If [this] is an instance of [KaSimpleCall], the list will contain only the [KaSimpleCall.signature]'s symbol
 * - If [this] is an instance of [KaMultiCall], the list will contain symbols from all [KaMultiCall.calls]
 */
@KaExperimentalApi
public val KaSimpleOrMultiCall.symbols: List<KaSymbol>
    get() = when (this) {
        is KaSimpleCall<*, *> -> listOf(symbol)
        is KaMultiCall -> calls.map { it.signature.symbol }
    }

/**
 * [this] call as a [KaSimpleCall], or `null` if it is a [KaMultiCall].
 *
 * ### Example
 * ```kotlin
 * var int: Int = 1
 *
 * fun usage() {
 *    int = 2
 *    int++
 * }
 * ```
 *
 * For `int = 2`, [simple] is the write access to `int`. For `int++`, which is a [KaMultiCall], [simple] is `null`.
 *
 * @see KaSimpleCall
 */
@KaExperimentalApi
public val KaSimpleOrMultiCall.simple: KaSimpleCall<*, *>?
    get() = this as? KaSimpleCall<*, *>

/**
 * [this] call as a [KaFunctionCall], or `null` if it is not a call to a function.
 *
 * ### Example
 * ```kotlin
 * class Foo {
 *    fun function() {}
 *    var int: Int = 1
 * }
 *
 * fun Foo.usage() {
 *    function()
 *    int
 * }
 * ```
 *
 * For `function()`, [function] is the call to `function`. For `int`, which is a [KaVariableAccessCall], [function] is `null`.
 *
 * @see KaFunctionCall
 * @see variable
 */
@KaExperimentalApi
public val KaSimpleOrMultiCall.function: KaFunctionCall<*>?
    get() = this as? KaFunctionCall<*>

/**
 * [this] call as a [KaVariableAccessCall], or `null` if it is not an access to a variable.
 *
 * ### Example
 * ```kotlin
 * class Foo {
 *    fun function() {}
 *    var int: Int = 1
 * }
 *
 * fun Foo.usage() {
 *    int
 *    function()
 * }
 * ```
 *
 * For `int`, [variable] is the read access to `int`. For `function()`, which is a [KaFunctionCall], [variable] is `null`.
 *
 * @see KaVariableAccessCall
 * @see function
 */
@KaExperimentalApi
public val KaSimpleOrMultiCall.variable: KaVariableAccessCall?
    get() = this as? KaVariableAccessCall

/**
 * The resolved [KaCallableSymbol] of the [KaSimpleCall].
 *
 * This is a short-cut for [KaCallableSignature.symbol].
 */
@OptIn(KaExperimentalApi::class)
@KaExperimentalApi
// A workaround to provide the helper utility but don't break the use site
// since in most cases it conflicts with `KaCallableMemberCall.symbol`.
// The workaround could be moved to the `KaCallableMemberCall.symbol` side once the API is stabilized.
@Suppress("INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaSimpleCall<S, C>.symbol: S
    get() = signature.symbol
