/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:[JvmName("KaCalls") JvmMultifileClass]

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
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
    public val candidateCalls: List<KaSingleOrMultiCall>
}

/**
 * Represents a successful resolution of a [KtResolvableCall].
 *
 * Use the [call] property to access the resolved [KaSingleOrMultiCall].
 *
 * @see KaResolver.tryResolveCall
 * @see KaResolver.resolveCall
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCallResolutionSuccess : KaCallResolutionAttempt {
    /**
     * The resolved call, which can be either a [KaSingleCall] or a [KaMultiCall].
     */
    public val call: KaSingleOrMultiCall
}

/**
 * The list of [KaSingleOrMultiCall]s.
 *
 * - If [this] is an instance of [KaCallResolutionSuccess], the list will contain only [KaCallResolutionSuccess.call]
 * - If [this] is an instance of [KaCallResolutionError], the list will contain [KaCallResolutionError.candidateCalls]
 */
@KaExperimentalApi
public val KaCallResolutionAttempt.calls: List<KaSingleOrMultiCall>
    get() = when (this) {
        is KaCallResolutionSuccess -> listOf(call)
        is KaCallResolutionError -> candidateCalls
    }
