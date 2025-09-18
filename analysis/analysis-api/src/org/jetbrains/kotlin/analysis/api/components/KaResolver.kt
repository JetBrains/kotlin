/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
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
     * Whether the [KtReference] uses [context-sensitive resolution](https://github.com/Kotlin/KEEP/issues/379) feature under the hood.
     *
     * #### Example
     *
     * ```
     * enum class MyEnum {
     *     X, Y
     * }
     *
     * fun foo(a: MyEnum) {}
     *
     * fun main() {
     *     foo(X) // An implicit reference to MyEnum.X
     * }
     * ```
     */
    @KaExperimentalApi
    public val KtReference.usesContextSensitiveResolution: Boolean

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
 * Resolves the given [KtReference] to symbols.
 *
 * Returns an empty collection if the reference cannot be resolved, or multiple symbols if the reference is ambiguous.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun KtReference.resolveToSymbols(): Collection<KaSymbol> {
    return with(s) {
        resolveToSymbols()
    }
}

/**
 * Resolves the given [KtReference] to a symbol.
 *
 * Returns `null` if the reference cannot be resolved, or resolves to multiple symbols due to being ambiguous.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun KtReference.resolveToSymbol(): KaSymbol? {
    return with(s) {
        resolveToSymbol()
    }
}

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
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun KtReference.isImplicitReferenceToCompanion(): Boolean {
    return with(s) {
        isImplicitReferenceToCompanion()
    }
}

/**
 * Whether the [KtReference] uses [context-sensitive resolution](https://github.com/Kotlin/KEEP/issues/379) feature under the hood.
 *
 * #### Example
 *
 * ```
 * enum class MyEnum {
 *     X, Y
 * }
 *
 * fun foo(a: MyEnum) {}
 *
 * fun main() {
 *     foo(X) // An implicit reference to MyEnum.X
 * }
 * ```
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public val KtReference.usesContextSensitiveResolution: Boolean
    get() = with(s) { usesContextSensitiveResolution }

/**
 * Resolves the given [KtElement] to a [KaCallInfo] object. [KaCallInfo] either contains a successfully resolved call or an error with
 * a list of candidate calls and a diagnostic.
 *
 * Returns `null` if the element does not correspond to a call.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun KtElement.resolveToCall(): KaCallInfo? {
    return with(s) {
        resolveToCall()
    }
}

/**
 * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html) for the call
 * corresponding to this [KtElement].
 *
 * To compare, the [resolveToCall] function only returns the final result of overload resolution, i.e. the most specific callable
 * passing all compatibility checks.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo> {
    return with(s) {
        resolveToCallCandidates()
    }
}
