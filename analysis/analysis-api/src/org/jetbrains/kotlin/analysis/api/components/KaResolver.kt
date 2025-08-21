/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.resolution.KaCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtElement

@SubclassOptInRequired(KaImplementationDetail::class)
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

    /**
     * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html) for the call
     * corresponding to this [KtElement].
     *
     * To compare, the [resolveToCall] function only returns the final result of overload resolution, i.e. the most specific callable
     * passing all compatibility checks.
     */
    public fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo>
}

/**
 * @see KaResolver.resolveToSymbols
 */
@KaContextParameterApi
context(context: KaResolver)
public fun KtReference.resolveToSymbols(): Collection<KaSymbol> {
    return with(context) { resolveToSymbols() }
}

/**
 * @see KaResolver.resolveToSymbol
 */
@KaContextParameterApi
context(context: KaResolver)
public fun KtReference.resolveToSymbol(): KaSymbol? {
    return with(context) { resolveToSymbol() }
}

/**
 * @see KaResolver.isImplicitReferenceToCompanion
 */
@KaContextParameterApi
context(context: KaResolver)
public fun KtReference.isImplicitReferenceToCompanion(): Boolean {
    return with(context) { isImplicitReferenceToCompanion() }
}

/**
 * @see KaResolver.resolveToCall
 */
@KaContextParameterApi
context(context: KaResolver)
public fun KtElement.resolveToCall(): KaCallInfo? {
    return with(context) { resolveToCall() }
}

/**
 * @see KaResolver.resolveToCallCandidates
 */
@KaContextParameterApi
context(context: KaResolver)
public fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo> {
    return with(context) { resolveToCallCandidates() }
}
