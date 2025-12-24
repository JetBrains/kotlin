/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.resolution.KtResolvableCall

/**
 * Represents an attempt to resolve [KtResolvableCall].
 *
 * [KaCallResolutionAttempt] represents either a successful resolution call ([KaCall]) or an error ([KaCallResolutionError])
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 **/
@KaExperimentalApi
public sealed interface KaCallResolutionAttempt : KaLifetimeOwner

/**
 * Represents an error that occurred during the resolution of a [KtResolvableCall]
 *
 * ### Example
 *
 * ```kotlin
 * class Foo {
 *    private fun bar() {}
 * }
 *
 * fun usage(foo: Foo) {
 *    foo.bar()
 * //     ^^^^^
 * }
 * ```
 *
 * `bar()` will be resolved to [KaCallResolutionError] with `INVISIBLE_REFERENCE` diagnostic and the `bar` call
 *
 * @see KaResolver.tryResolveCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCallResolutionError : KaCallResolutionAttempt {
    /**
     * The diagnostic associated with the error
     */
    public val diagnostic: KaDiagnostic

    /**
     * The list of candidate calls that were considered during the resolution. Can be empty
     */
    public val candidateCalls: List<KaSingleCall<*, *>>
}

/**
 * Represents a successful resolution of a [KtResolvableCall].
 *
 * It can be either a [KaSingleCall] or a [KaMultiCall].
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
 * `function()` call will be represented as a [KaSingleCall] (the target is the function),
 * and `int++` call as a [KaMultiCall] (with two targets: the `int` property and the `++` operator function)
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 */
@KaExperimentalApi
public sealed interface KaCallResolutionSuccess : KaCallResolutionAttempt

/**
 * Represents a successful resolution resulting in a single call.
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
 * `function()` call will be represented as a [KaSingleCall]
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSingleCall<S : KaCallableSymbol, C : KaCallableSignature<S>> : KaCallResolutionSuccess {
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
    @KaExperimentalApi
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
 * `int++` call will be represented as a [KaMultiCall] with two [KaSingleCall]s inside: one for the `int` property
 * and one for the `++` operator function ([Int.inc])
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 */
@KaExperimentalApi
public sealed interface KaMultiCall : KaCallResolutionSuccess {
    /**
     * The non-empty list of [KaSingleCall]s that were discovered during resolution of [KtResolvableCall]
     */
    @KaExperimentalApi
    public val calls: List<KaSingleCall<*, *>>
}

/**
 * [KaMultiCall] represent a bunch of unrelated compound calls,
 * so the client typically is not expected to handle all possible cases.
 *
 * The usual way to work with compound calls is to get them using a special [KaResolver.resolveCall] overload
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
private interface KaMultiUnknownCall : KaMultiCall

/**
 * The list of [KaCall].
 *
 * - If [this] is an instance of [KaSingleCall], the list will contain only [this] call
 * - If [this] is an instance of [KaMultiUnknownCall], the list will contain [KaMultiUnknownCall.calls]
 * - If [this] is an instance of [KaCallResolutionError], the list will contain [KaCallResolutionError.candidateCalls]
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.calls: List<KaSingleCall<*, *>>
    get() = when (this) {
        is KaSingleCall<*, *> -> listOf(this)
        is KaMultiCall -> calls
        is KaCallResolutionError -> candidateCalls
    }
