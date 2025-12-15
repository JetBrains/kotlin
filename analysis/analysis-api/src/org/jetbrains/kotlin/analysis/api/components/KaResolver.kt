/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KDocReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaResolver : KaSessionComponent {
    /**
     * Attempts to resolve a symbol for the given [KtResolvable].
     *
     * Returns a [KaSymbolResolutionAttempt] that describes either success ([KaSymbolResolutionSuccess])
     * or failure ([KaSymbolResolutionError]), or `null` if no result is available
     *
     * @see KaSymbolResolutionSuccess
     * @see KaSymbolResolutionError
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvable.tryResolveSymbol(): KaSymbolResolutionAttempt?

    /**
     * Resolves symbols for the given [KtResolvable].
     *
     * Returns all resolved [KaSymbol]s if successful; otherwise, an empty list. Might contain multiple symbols
     * for a multiple result ([KaMultiSymbolResolutionSuccess])
     *
     * @see tryResolveSymbol
     * @see resolveSymbol
     * @see KaSingleSymbolResolutionSuccess
     * @see KaMultiSymbolResolutionSuccess
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvable.resolveSymbols(): Collection<KaSymbol>

    /**
     * Resolves a single symbol for the given [KtResolvable].
     *
     * Returns the [KaSymbol] if there is exactly one target ([KaSingleSymbolResolutionSuccess]); otherwise, `null`
     *
     * @see tryResolveSymbol
     * @see resolveSymbols
     * @see KaSingleSymbolResolutionSuccess
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvable.resolveSymbol(): KaSymbol?

    /**
     * Resolves the constructor symbol of the annotation referenced by the given [KtAnnotationEntry].
     *
     * #### Example
     *
     * ```kotlin
     * annotation class Anno(val x: Int)
     *
     * @Anno(42)
     * fun foo() {}
     * ```
     *
     * Calling `resolveSymbol()` on the [KtAnnotationEntry] (`@Anno(42)`) returns the [KaConstructorSymbol] of `Anno`'s
     * annotation constructor if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on annotation entries
     *
     * @see tryResolveSymbol
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol?

    /**
     * Resolves the constructor symbol by the given [KtSuperTypeCallEntry].
     *
     * #### Example
     *
     * ```kotlin
     * open class Base(i: Int)
     *
     * class Derived : Base(1)
     * //              ^^^^^^^
     * ```
     *
     * Calling `resolveSymbol()` on the [KtSuperTypeCallEntry] (`Base(1)`) returns the [KaConstructorSymbol] of `Base`'s
     * constructor if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on supertype constructor calls
     *
     * @see tryResolveSymbol
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol?

    /**
     * Resolves the constructor symbol referenced by the given [KtConstructorDelegationCall].
     *
     * #### Example
     *
     * ```kotlin
     * open class Base(val i: Int)
     *
     * class Derived : Base {
     *     constructor() : this(0)
     *     //              ^^^^^^^
     *
     *     constructor(x: Int) : super(x)
     *     //                    ^^^^^^^^
     * }
     * ```
     *
     * Calling `resolveSymbol()` on a [KtConstructorDelegationCall] (either `this(...)` or `super(...)`) returns the
     * [KaConstructorSymbol] of the target constructor if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on constructor delegation calls
     *
     * @see tryResolveSymbol
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol?

    /**
     * Resolves the callable symbol targeted by the given [KtCallElement].
     *
     * #### Example
     *
     * ```kotlin
     * fun foo(x: Int) {}
     *
     * fun test() {
     *     foo(42)
     * //  ^^^^^^^
     * }
     * ```
     *
     * Calling `resolveSymbol()` on the [KtCallElement] (`foo(42)`) returns the [KaCallableSymbol] of `foo`
     * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on call elements
     *
     * @see tryResolveSymbol
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtCallElement.resolveSymbol(): KaCallableSymbol?

    /**
     * Resolves the callable symbol targeted by the given [KtCallableReferenceExpression].
     *
     * #### Example
     *
     * ```kotlin
     * fun foo(x: Int) {}
     *
     * val ref = ::foo
     * //        ^^^^^
     * ```
     *
     * Calling `resolveSymbol()` on the [KtCallableReferenceExpression] (`::foo`) returns the [KaCallableSymbol] of `foo`
     * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on callable reference expressions
     *
     * @see tryResolveSymbol
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol?

    /**
     * Resolves the operator function symbol targeted by the given [KtArrayAccessExpression].
     *
     * #### Example
     *
     * ```kotlin
     * class A {
     *     operator fun get(i: Int): Int = i
     *     operator fun set(i: Int, value: Int) {}
     * }
     *
     * fun test(a: A) {
     *     a[0]
     * //  ^^^^  resolves to `get`
     *     a[0] = 1
     * //  ^^^^ resolves to `set`
     * }
     * ```
     *
     * Calling `resolveSymbol()` on a [KtArrayAccessExpression] (`a[0]`) returns the [KaNamedFunctionSymbol] of the corresponding
     * `get`/`set` operator if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on array access operations.
     *
     * **Note**: the `get` call is prefered in the case of a compound assignent
     *
     * ```kotlin
     * fun test(m: MyMap<String, Int>) {
     *     m["a"] += 1
     * //  ^^^^^^
     * }
     * ```
     *
     * @see tryResolveSymbol
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtArrayAccessExpression.resolveSymbol(): KaNamedFunctionSymbol?

    /**
     * Attempts to resolve the call for the given [KtResolvableCall].
     *
     * ### Usage Example:
     * ```kotlin
     * fun KaSession.findResolutionDiagnostic(expression: KtCallExpression): KaDiagnostic? {
     *   val attempt = expression.tryResolveCall() ?: return null
     *   val error = attempt as? KaCallResolutionError ?: return null
     *   return error.diagnostic
     * }
     * ```
     *
     * Returns a [KaCallResolutionAttempt], or `null` if no result is available
     *
     * @see resolveCall
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvableCall.tryResolveCall(): KaCallResolutionAttempt?

    /**
     * Resolves the call for the given [KtResolvableCall].
     *
     * ### Usage Example:
     * ```kotlin
     * fun KaSession.resolveSymbol(expression: KtCallExpression): KaSymbol? {
     *   val successfulCall = expression.resolveCall() ?: return null
     *   val callableCall = successfulCall as? KaCallableMemberCall ?: return null
     *   return callableCall.symbol
     * }
     * ```
     *
     * Returns the resolved [KaCall] on success; otherwise, `null`
     *
     * @see tryResolveCall
     * @see collectCallCandidates
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvableCall.resolveCall(): KaCall?

    /**
     * Resolves the given [KtAnnotationEntry] to an annotation constructor call.
     *
     * #### Example
     *
     * ```kotlin
     * annotation class Anno(val x: Int)
     *
     * @Anno(42)
     * fun foo() {}
     * ```
     *
     * Returns the corresponding [KaAnnotationCall] if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on annotation entries.
     * Use [collectCallCandidates] to inspect all candidates considered during overload resolution
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtAnnotationEntry.resolveCall(): KaAnnotationCall?

    /**
     * Resolves the given [KtSuperTypeCallEntry] to a constructor call of the referenced supertype.
     *
     * #### Example
     *
     * ```kotlin
     * open class Base(i: Int)
     *
     * class Derived : Base(1)
     * //              ^^^^^^^
     * ```
     *
     * Returns the corresponding [KaFunctionCall] if resolution succeeds;
     * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on supertype constructor calls
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>?

    /**
     * Resolves the given [KtConstructorDelegationCall] to a delegated constructor call.
     *
     * #### Example
     *
     * ```kotlin
     * open class Base(val i: Int)
     *
     * class Derived : Base {
     *     constructor() : this(0)
     *     //              ^^^^^^^
     *
     *     constructor(x: Int) : super(x)
     *     //                    ^^^^^^^^
     * }
     * ```
     *
     * Returns the corresponding [KaDelegatedConstructorCall] if resolution succeeds;
     * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on constructor delegation calls
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall?

    /**
     * Resolves the given [KtCallElement] to a callable member call.
     *
     * #### Example
     *
     * ```kotlin
     * fun foo(x: Int) {}
     *
     * fun test() {
     *     foo(42)
     * //  ^^^^^^^
     * }
     * ```
     *
     * Returns the corresponding [KaCallableMemberCall] if resolution succeeds;
     * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on call elements
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtCallElement.resolveCall(): KaCallableMemberCall<*, *>?

    /**
     * Resolves the given [KtCallableReferenceExpression] to a callable member call.
     *
     * #### Example
     *
     * ```kotlin
     * class A { fun foo() {} }
     *
     * val ref = A::foo
     * //        ^^^^^^
     * ```
     *
     * Returns the corresponding [KaCallableMemberCall] if resolution succeeds;
     * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on callable reference expressions
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtCallableReferenceExpression.resolveCall(): KaCallableMemberCall<*, *>?

    /**
     * Resolves the given [KtArrayAccessExpression] to a simple function call representing `get`/`set` operator invocation.
     *
     * #### Example
     *
     * ```kotlin
     * class A {
     *     operator fun get(i: Int): Int = i
     *     operator fun set(i: Int, value: Int) {}
     * }
     *
     * fun test(a: A) {
     *     a[0]
     * //  ^^^^  resolves to `get`
     *     a[0] = 1
     * //  ^^^^ resolves to `set`
     * }
     * ```
     *
     * Returns the corresponding [KaSimpleFunctionCall] if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on array access operations.
     *
     * **Note**: the `get` call is prefered in the case of a compound assignent
     *
     * ```kotlin
     * fun test(m: MyMap<String, Int>) {
     *     m["a"] += 1
     * //  ^^^^^^
     * }
     * ```
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtArrayAccessExpression.resolveCall(): KaSimpleFunctionCall?

    /**
     * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html)
     * for the call corresponding to the given [KtResolvableCall].
     *
     * In contrast, [resolveCall] returns only the final result, i.e., the most specific callable that passes all
     * compatibility checks
     *
     * @see resolveCall
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidateInfo>

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

    /**
     * Resolves [this] using the classic KDoc resolution logic.
     */
    @KaNonPublicApi
    @KaK1Unsupported
    public fun KDocReference.resolveToSymbolWithClassicKDocResolver(): KaSymbol?
}

/**
 * Attempts to resolve a symbol for the given [KtResolvable].
 *
 * Returns a [KaSymbolResolutionAttempt] that describes either success ([KaSymbolResolutionSuccess])
 * or failure ([KaSymbolResolutionError]), or `null` if no result is available
 *
 * @see KaSymbolResolutionSuccess
 * @see KaSymbolResolutionError
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(s: KaSession)
public fun KtResolvable.tryResolveSymbol(): KaSymbolResolutionAttempt? {
    return with(s) {
        tryResolveSymbol()
    }
}

/**
 * Resolves symbols for the given [KtResolvable].
 *
 * Returns all resolved [KaSymbol]s if successful; otherwise, an empty list. Might contain multiple symbols
 * for a multiple result ([KaMultiSymbolResolutionSuccess])
 *
 * @see tryResolveSymbol
 * @see resolveSymbol
 * @see KaSingleSymbolResolutionSuccess
 * @see KaMultiSymbolResolutionSuccess
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(s: KaSession)
public fun KtResolvable.resolveSymbols(): Collection<KaSymbol> {
    return with(s) {
        resolveSymbols()
    }
}

/**
 * Resolves a single symbol for the given [KtResolvable].
 *
 * Returns the [KaSymbol] if there is exactly one target ([KaSingleSymbolResolutionSuccess]); otherwise, `null`
 *
 * @see tryResolveSymbol
 * @see resolveSymbols
 * @see KaSingleSymbolResolutionSuccess
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(s: KaSession)
public fun KtResolvable.resolveSymbol(): KaSymbol? {
    return with(s) {
        resolveSymbol()
    }
}

/**
 * Resolves the constructor symbol of the annotation referenced by the given [KtAnnotationEntry].
 *
 * #### Example
 *
 * ```kotlin
 * annotation class Anno(val x: Int)
 *
 * @Anno(42)
 * fun foo() {}
 * ```
 *
 * Calling `resolveSymbol()` on the [KtAnnotationEntry] (`@Anno(42)`) returns the [KaConstructorSymbol] of `Anno`'s
 * annotation constructor if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on annotation entries
 *
 * @see tryResolveSymbol
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol? {
    return with(s) {
        resolveSymbol()
    }
}

/**
 * Resolves the constructor symbol by the given [KtSuperTypeCallEntry].
 *
 * #### Example
 *
 * ```kotlin
 * open class Base(i: Int)
 *
 * class Derived : Base(1)
 * //              ^^^^^^^
 * ```
 *
 * Calling `resolveSymbol()` on the [KtSuperTypeCallEntry] (`Base(1)`) returns the [KaConstructorSymbol] of `Base`'s
 * constructor if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on supertype constructor calls
 *
 * @see tryResolveSymbol
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol? {
    return with(s) {
        resolveSymbol()
    }
}

/**
 * Resolves the constructor symbol referenced by the given [KtConstructorDelegationCall].
 *
 * #### Example
 *
 * ```kotlin
 * open class Base(val i: Int)
 *
 * class Derived : Base {
 *     constructor() : this(0)
 *     //              ^^^^^^^
 *
 *     constructor(x: Int) : super(x)
 *     //                    ^^^^^^^^
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtConstructorDelegationCall] (either `this(...)` or `super(...)`) returns the
 * [KaConstructorSymbol] of the target constructor if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on constructor delegation calls
 *
 * @see tryResolveSymbol
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol? {
    return with(s) {
        resolveSymbol()
    }
}

/**
 * Resolves the callable symbol targeted by the given [KtCallElement].
 *
 * #### Example
 *
 * ```kotlin
 * fun foo(x: Int) {}
 *
 * fun test() {
 *     foo(42)
 * //  ^^^^^^^
 * }
 * ```
 *
 * Calling `resolveSymbol()` on the [KtCallElement] (`foo(42)`) returns the [KaCallableSymbol] of `foo`
 * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on call elements
 *
 * @see tryResolveSymbol
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtCallElement.resolveSymbol(): KaCallableSymbol? {
    return with(s) {
        resolveSymbol()
    }
}

/**
 * Resolves the callable symbol targeted by the given [KtCallableReferenceExpression].
 *
 * #### Example
 *
 * ```kotlin
 * fun foo(x: Int) {}
 *
 * val ref = ::foo
 * //        ^^^^^
 * ```
 *
 * Calling `resolveSymbol()` on the [KtCallableReferenceExpression] (`::foo`) returns the [KaCallableSymbol] of `foo`
 * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on callable reference expressions
 *
 * @see tryResolveSymbol
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol? {
    return with(s) {
        resolveSymbol()
    }
}

/**
 * Resolves the operator function symbol targeted by the given [KtArrayAccessExpression].
 *
 * #### Example
 *
 * ```kotlin
 * class A {
 *     operator fun get(i: Int): Int = i
 *     operator fun set(i: Int, value: Int) {}
 * }
 *
 * fun test(a: A) {
 *     a[0]
 * //  ^^^^  resolves to `get`
 *     a[0] = 1
 * //  ^^^^ resolves to `set`
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtArrayAccessExpression] (`a[0]`) returns the [KaNamedFunctionSymbol] of the corresponding
 * `get`/`set` operator if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on array access operations.
 *
 * **Note**: the `get` call is prefered in the case of a compound assignent
 *
 * ```kotlin
 * fun test(m: MyMap<String, Int>) {
 *     m["a"] += 1
 * //  ^^^^^^
 * }
 * ```
 *
 * @see tryResolveSymbol
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtArrayAccessExpression.resolveSymbol(): KaNamedFunctionSymbol? {
    return with(s) {
        resolveSymbol()
    }
}

/**
 * Attempts to resolve the call for the given [KtResolvableCall].
 *
 * ### Usage Example:
 * ```kotlin
 * fun KaSession.findResolutionDiagnostic(expression: KtCallExpression): KaDiagnostic? {
 *   val attempt = expression.tryResolveCall() ?: return null
 *   val error = attempt as? KaCallResolutionError ?: return null
 *   return error.diagnostic
 * }
 * ```
 *
 * Returns a [KaCallResolutionAttempt], or `null` if no result is available
 *
 * @see resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(s: KaSession)
public fun KtResolvableCall.tryResolveCall(): KaCallResolutionAttempt? {
    return with(s) {
        tryResolveCall()
    }
}

/**
 * Resolves the call for the given [KtResolvableCall].
 *
 * ### Usage Example:
 * ```kotlin
 * fun KaSession.resolveSymbol(expression: KtCallExpression): KaSymbol? {
 *   val successfulCall = expression.resolveCall() ?: return null
 *   val callableCall = successfulCall as? KaCallableMemberCall ?: return null
 *   return callableCall.symbol
 * }
 * ```
 *
 * Returns the resolved [KaCall] on success; otherwise, `null`
 *
 * @see tryResolveCall
 * @see collectCallCandidates
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(s: KaSession)
public fun KtResolvableCall.resolveCall(): KaCall? {
    return with(s) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtAnnotationEntry] to an annotation constructor call.
 *
 * #### Example
 *
 * ```kotlin
 * annotation class Anno(val x: Int)
 *
 * @Anno(42)
 * fun foo() {}
 * ```
 *
 * Returns the corresponding [KaAnnotationCall] if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on annotation entries.
 * Use [collectCallCandidates] to inspect all candidates considered during overload resolution
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtAnnotationEntry.resolveCall(): KaAnnotationCall? {
    return with(s) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtSuperTypeCallEntry] to a constructor call of the referenced supertype.
 *
 * #### Example
 *
 * ```kotlin
 * open class Base(i: Int)
 *
 * class Derived : Base(1)
 * //              ^^^^^^^
 * ```
 *
 * Returns the corresponding [KaFunctionCall] if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on supertype constructor calls
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>? {
    return with(s) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtConstructorDelegationCall] to a delegated constructor call.
 *
 * #### Example
 *
 * ```kotlin
 * open class Base(val i: Int)
 *
 * class Derived : Base {
 *     constructor() : this(0)
 *     //              ^^^^^^^
 *
 *     constructor(x: Int) : super(x)
 *     //                    ^^^^^^^^
 * }
 * ```
 *
 * Returns the corresponding [KaDelegatedConstructorCall] if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on constructor delegation calls
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall? {
    return with(s) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtCallElement] to a callable member call.
 *
 * #### Example
 *
 * ```kotlin
 * fun foo(x: Int) {}
 *
 * fun test() {
 *     foo(42)
 * //  ^^^^^^^
 * }
 * ```
 *
 * Returns the corresponding [KaCallableMemberCall] if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on call elements
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtCallElement.resolveCall(): KaCallableMemberCall<*, *>? {
    return with(s) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtCallableReferenceExpression] to a callable member call.
 *
 * #### Example
 *
 * ```kotlin
 * class A { fun foo() {} }
 *
 * val ref = A::foo
 * //        ^^^^^^
 * ```
 *
 * Returns the corresponding [KaCallableMemberCall] if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on callable reference expressions
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtCallableReferenceExpression.resolveCall(): KaCallableMemberCall<*, *>? {
    return with(s) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtArrayAccessExpression] to a simple function call representing `get`/`set` operator invocation.
 *
 * #### Example
 *
 * ```kotlin
 * class A {
 *     operator fun get(i: Int): Int = i
 *     operator fun set(i: Int, value: Int) {}
 * }
 *
 * fun test(a: A) {
 *     a[0]
 * //  ^^^^  resolves to `get`
 *     a[0] = 1
 * //  ^^^^ resolves to `set`
 * }
 * ```
 *
 * Returns the corresponding [KaSimpleFunctionCall] if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on array access operations.
 *
 * **Note**: the `get` call is prefered in the case of a compound assignent
 *
 * ```kotlin
 * fun test(m: MyMap<String, Int>) {
 *     m["a"] += 1
 * //  ^^^^^^
 * }
 * ```
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KtArrayAccessExpression.resolveCall(): KaSimpleFunctionCall? {
    return with(s) {
        resolveCall()
    }
}

/**
 * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html)
 * for the call corresponding to the given [KtResolvableCall].
 *
 * In contrast, [resolveCall] returns only the final result, i.e., the most specific callable that passes all
 * compatibility checks
 *
 * @see resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(s: KaSession)
public fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidateInfo> {
    return with(s) {
        collectCallCandidates()
    }
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

/**
 * Resolves [this] using the classic KDoc resolution logic.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaNonPublicApi
@KaK1Unsupported
@KaContextParameterApi
context(s: KaSession)
public fun KDocReference.resolveToSymbolWithClassicKDocResolver(): KaSymbol? {
    return with(s) {
        resolveToSymbolWithClassicKDocResolver()
    }
}
