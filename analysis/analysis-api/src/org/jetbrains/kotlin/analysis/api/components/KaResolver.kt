/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.resolution.KaCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtElement

public interface KaResolver : KaSessionComponent {
    /**
     * Resolves the given [KtReference] to symbols.
     *
     * Returns an empty collection if the reference cannot be resolved, or multiple symbols if the reference is ambiguous.
     */
    public fun KtReference.resolveToSymbols(): Collection<KaSymbol>

    /**
     * Resolves the given [KtReference] to a symbol.
     *
     * Returns `null` if the reference cannot be resolved, or resolves to multiple symbols due to being ambiguous.
     */
    public fun KtReference.resolveToSymbol(): KaSymbol?

    /**
     * Checks if the [KtReference] is an implicit reference to a companion object via the containing class.
     *
     * #### Example
     *
     * ```
     * class A {
     *    companion object {
     *       fun foo() {}
     *    }
     * }
     * ```
     *
     * Given a call `A.foo()`, `A` is an implicit reference to the companion object, so `isImplicitReferenceToCompanion` returns `true`.
     */
    public fun KtReference.isImplicitReferenceToCompanion(): Boolean

    /**
     * Resolves the given [KtElement] to a [KaCallInfo] object. [KaCallInfo] either contains a successfully resolved call or an error with
     * a list of candidate calls and a diagnostic.
     *
     * Returns `null` if the element does not correspond to a call.
     */
    public fun KtElement.resolveToCall(): KaCallInfo?

    @Deprecated(
        message = "The API will be changed soon. Use 'resolveToCall()' in a transit period",
        replaceWith = ReplaceWith("resolveToCall()"),
        level = DeprecationLevel.HIDDEN,
    )
    public fun KtElement.resolveCall(): KaCallInfo? = resolveToCall()

    /**
     * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html) for the call
     * corresponding to this [KtElement].
     *
     * To compare, the [resolveToCall] function only returns the final result of overload resolution, i.e. the most specific callable
     * passing all compatibility checks.
     */
    public fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo>
}
