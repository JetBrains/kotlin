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

public interface KaResolver {
    /**
     * Resolves the given [KtReference] to symbols.
     *
     * Returns an empty collection if a reference cannot be resolved.
     * The function may return multiple symbols if the reference is ambiguous.
     */
    public fun KtReference.resolveToSymbols(): Collection<KaSymbol>

    /**
     * Resolves the given [KtReference] to a symbol.
     * Returns `null` if a reference cannot be resolved, or resolved to multiple symbols because of ambiguity.
     */
    public fun KtReference.resolveToSymbol(): KaSymbol?

    /**
     * Checks if the reference is an implicit reference to a companion object via the containing class.
     *
     * Example:
     * ```
     * class A {
     *    companion object {
     *       fun foo() {}
     *    }
     * }
     * ```
     *
     * For the case provided, inside the call `A.foo()`,
     * the `A` is an implicit reference to the companion object, so `isImplicitReferenceToCompanion` returns `true`
     *
     * @return `true` if the reference is an implicit reference to a companion object, `false` otherwise.
     */
    public fun KtReference.isImplicitReferenceToCompanion(): Boolean

    /**
     * Resolves the given [KtElement] to a [KaCallInfo] object.
     * [KaCallInfo] either contains a successfully resolved call or an error with a list of candidate calls and a diagnostic.
     *
     * Returns `null` if the element does not correspond to a call.
     */
    public fun KtElement.resolveToCall(): KaCallInfo?

    /**
     * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html) for the call
     * corresponding to this [KtElement].
     *
     * To compare, the [resolveToCall] function only returns the final result of overload resolution,
     * i.e., the most specific callable passing all compatibility checks.
     */
    public fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo>
}