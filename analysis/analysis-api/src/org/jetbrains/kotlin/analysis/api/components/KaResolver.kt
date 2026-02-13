/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.*
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
    public fun KtResolvable.tryResolveSymbols(): KaSymbolResolutionAttempt?

    /**
     * Resolves symbols for the given [KtResolvable].
     *
     * Returns all resolved [KaSymbol]s if successful; otherwise, an empty list. Might contain multiple symbols
     * for a compound case
     *
     * @see tryResolveSymbols
     * @see resolveSymbol
     * @see KaSymbolResolutionSuccess
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvable.resolveSymbols(): Collection<KaSymbol>

    /**
     * Resolves a single symbol for the given [KtResolvable].
     *
     * Returns the [KaSymbol] if there is exactly one target; otherwise, `null`
     *
     * @see tryResolveSymbols
     * @see resolveSymbols
     * @see KaSymbolResolutionSuccess
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
     * @see tryResolveSymbols
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
     * @see tryResolveSymbols
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
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol?

    /**
     * Resolves the constructor symbol referenced by the given [KtConstructorDelegationReferenceExpression].
     *
     * #### Example
     *
     * ```kotlin
     * open class Base(val i: Int)
     *
     * class Derived : Base {
     *     constructor() : this(0)
     *     //              ^^^^
     *
     *     constructor(x: Int) : super(x)
     *     //                    ^^^^^
     * }
     * ```
     *
     * Calling `resolveSymbol()` on a [KtConstructorDelegationReferenceExpression] (either `this` or `super`) returns the
     * [KaConstructorSymbol] of the target constructor if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on constructor delegation calls
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtConstructorDelegationReferenceExpression.resolveSymbol(): KaConstructorSymbol?

    /**
     * Resolves the function symbol targeted by the given [KtCallElement].
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
     * Calling `resolveSymbol()` on the [KtCallElement] (`foo(42)`) returns the [KaFunctionSymbol] of `foo`
     * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on call elements
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtCallElement.resolveSymbol(): KaFunctionSymbol?

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
     * @see tryResolveSymbols
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
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtArrayAccessExpression.resolveSymbol(): KaNamedFunctionSymbol?

    /**
     * Resolves the function symbol targeted by the given [KtCollectionLiteralExpression].
     *
     * #### Example
     *
     * ```kotlin
     * annotation class Anno(val arr: IntArray)
     *
     * @Anno([1, 2, 3])
     * //    ^^^^^^^^^ resolves to the `intArrayOf` function
     * fun use() {}
     * ```
     *
     * Calling `resolveSymbol()` on a [KtCollectionLiteralExpression] (`[1, 2, 3]`) returns the [KaNamedFunctionSymbol]
     * of the corresponding array factory (e.g., `arrayOf`, `intArrayOf`) if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on collection literal expressions
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtCollectionLiteralExpression.resolveSymbol(): KaNamedFunctionSymbol?

    /**
     * Resolves the constructor symbol referenced by the given [KtEnumEntrySuperclassReferenceExpression].
     *
     * #### Example
     *
     * ```kotlin
     * enum class EnumWithConstructor(val x: Int) {
     *     Entry(1)
     * //      ^ resolves to the constructor of `EnumWithConstructor`
     * }
     * ```
     *
     * Calling `resolveSymbol()` on a [KtEnumEntrySuperclassReferenceExpression] (``) returns the
     * [KaConstructorSymbol] of the enum class constructor if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on enum entry superclass constructor calls
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtEnumEntrySuperclassReferenceExpression.resolveSymbol(): KaConstructorSymbol?

    /**
     * Resolves the declaration symbol targeted by the given [KtLabelReferenceExpression].
     *
     * #### Example
     *
     * ```kotlin
     * fun myAction(action: () -> Unit) {
     *     action {
     *         return@action // resolves to the anonymous function
     * //            ^^^^^^^
     *     }
     *
     *     return@main
     * //        ^^^^^
     * }
     * ```
     *
     * Calling `resolveSymbol()` on a [KtLabelReferenceExpression] (`@action` and `@main`) returns the corresponding [KaDeclarationSymbol]
     * of the labeled declaration if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or
     * ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on label references
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtLabelReferenceExpression.resolveSymbol(): KaDeclarationSymbol?

    /**
     * Resolves the function symbol targeted by the given [KtReturnExpression].
     *
     * #### Example
     *
     * ```kotlin
     * fun foo() {
     *     return
     * //  ^^^^^^ resolves to `foo`
     * }
     *
     * fun main() {
     *     listOf(1).forEach label@{
     *         if (it == 0) return@label
     * //                   ^^^^^^^^^^^^ resolves to the anonymous function of this lambda
     *     }
     * }
     * ```
     *
     * Calling `resolveSymbol()` on a [KtReturnExpression] (`return` or `return@label`) returns the [KaFunctionSymbol] of the enclosing function
     * (for unlabeled returns) or of the labeled target (for `return@label`) if resolution succeeds; otherwise, it returns
     * `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on return expressions
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtReturnExpression.resolveSymbol(): KaFunctionSymbol?

    /**
     * Resolves the operator function symbol targeted by the given [KtWhenConditionInRange].
     *
     * #### Example
     *
     * ```kotlin
     * fun test(x: Int) {
     *     when (x) {
     *         in 1..10 -> {}
     * //      ^^^^^^^^ resolves to a call of `IntRange.contains`
     *
     *         !in setOf(1, 2, 3) -> {}
     * //      ^^^^^^^^^^^^^^^^^^ resolves to a call of `Set<Int>.contains`
     *     }
     * }
     * ```
     *
     * Calling `resolveSymbol()` on a [KtWhenConditionInRange] (`in 1..10` or `!in setOf(1, 2, 3)`) returns the [KaNamedFunctionSymbol]
     * of the labeled declaration if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on `in`/`!in`
     * range conditions inside `when` entries
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtWhenConditionInRange.resolveSymbol(): KaNamedFunctionSymbol?

    /**
     * Resolves the callable symbol targeted by the given [KtDestructuringDeclarationEntry].
     *
     * #### Example
     *
     * ```kotlin
     * data class Point(val x: Int, val y: Int)
     *
     * fun test(p: Point) {
     *     val (x, y) = p
     * //       ^ resolves to `component1`
     * //          ^ resolves to `component2`
     * }
     * ```
     *
     * Calling `resolveSymbol()` on a [KtDestructuringDeclarationEntry] returns the [KaCallableSymbol] of the corresponding
     * `componentN` function (for positional destructuring) or the accessed property (for name-based destructuring)
     * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on destructuring declaration entries
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun KtDestructuringDeclarationEntry.resolveSymbol(): KaCallableSymbol?

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
     *   val call = expression.resolveCall() ?: return null
     *   val callableCall = call as? KaSingleCall<*, *> ?: return null
     *   return callableCall.symbol
     * }
     * ```
     *
     * Returns the resolved [KaSingleOrMultiCall] on success; otherwise, `null`
     *
     * @see tryResolveCall
     * @see collectCallCandidates
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvableCall.resolveCall(): KaSingleOrMultiCall?

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
     * Resolves the given [KtConstructorDelegationReferenceExpression] to a delegated constructor call.
     *
     * #### Example
     *
     * ```kotlin
     * open class Base(val i: Int)
     *
     * class Derived : Base {
     *     constructor() : this(0)
     *     //              ^^^^
     *
     *     constructor(x: Int) : super(x)
     *     //                    ^^^^^
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
    public fun KtConstructorDelegationReferenceExpression.resolveCall(): KaDelegatedConstructorCall?

    /**
     * Resolves the given [KtCallElement] to a function call.
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
     * Returns the corresponding [KaSingleCall] if resolution succeeds;
     * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on call elements
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtCallElement.resolveCall(): KaFunctionCall<*>?

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
     * Returns the corresponding [KaSingleCall] if resolution succeeds;
     * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on callable reference expressions
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtCallableReferenceExpression.resolveCall(): KaSingleCall<*, *>?

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
    public fun KtArrayAccessExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>?

    /**
     * Resolves the given [KtCollectionLiteralExpression] to a simple function call representing the corresponding
     * array factory invocation.
     *
     * #### Example
     *
     * ```kotlin
     * annotation class Anno(val arr: IntArray)
     *
     * @Anno([1, 2, 3])
     * //    ^^^^^^^^^ resolves to a call of `intArrayOf`
     * fun use() {}
     * ```
     *
     * Returns the corresponding [KaSimpleFunctionCall] if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on collection literal expressions
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtCollectionLiteralExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>?

    /**
     * Resolves the given [KtEnumEntrySuperclassReferenceExpression] to a delegated constructor call.
     *
     * #### Example
     *
     * ```kotlin
     * enum class EnumWithConstructor(val x: Int) {
     *     Entry(1)
     * //      ^ resolves to the constructor of `EnumWithConstructor`
     * }
     * ```
     *
     * Returns the corresponding [KaDelegatedConstructorCall] if resolution succeeds;
     * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on enum entry superclass constructor calls
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtEnumEntrySuperclassReferenceExpression.resolveCall(): KaDelegatedConstructorCall?

    /**
     * Resolves the given [KtWhenConditionInRange] to a simple function call representing the corresponding
     * `contains` operator invocation used by the `in`/`!in` branch condition.
     *
     * #### Example
     *
     * ```kotlin
     * fun test(x: Int) {
     *     when (x) {
     *         in 1..10 -> {}
     * //      ^^^^^^^^ resolves to a call of `IntRange.contains`
     *
     *         !in setOf(1, 2, 3) -> {}
     * //      ^^^^^^^^^^^^^^^^^^ resolves to a call of `Set<Int>.contains`
     *     }
     * }
     * ```
     *
     * Returns the corresponding [KaSimpleFunctionCall] if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on `in`/`!in`
     * range conditions inside `when` entries
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtWhenConditionInRange.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>?

    /**
     * Resolves the given [KtDestructuringDeclarationEntry] to a call representing the `componentN` invocation
     * (for positional destructuring) or the property access (for name-based destructuring).
     *
     * #### Example
     *
     * ```kotlin
     * data class Point(val x: Int, val y: Int)
     *
     * fun test(p: Point) {
     *     val (x, y) = p
     * //       ^ resolves to a call of `component1`
     * //          ^ resolves to a call of `component2`
     * }
     * ```
     *
     * Returns the corresponding [KaSingleCall] if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on destructuring declaration entries
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtDestructuringDeclarationEntry.resolveCall(): KaSingleCall<*, *>?

    /**
     * Resolves the given [KtForExpression] to a [KaForLoopCall] representing the desugared `for` loop.
     *
     * A `for` loop desugars into three operator calls:
     * - `iterator()` on the loop range expression
     * - `hasNext()` on the iterator
     * - `next()` on the iterator
     *
     * #### Example
     *
     * ```kotlin
     * for (item in list) {
     *     println(item)
     * }
     * ```
     *
     * Calling `resolveCall()` on the [KtForExpression] returns a [KaForLoopCall] containing the three
     * desugared operator calls if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on `for` loops
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtForExpression.resolveCall(): KaForLoopCall?

    /**
     * Resolves the given [KtPropertyDelegate] to a [KaDelegatedPropertyCall] representing the desugared delegated property.
     *
     * A delegated property desugars into up to three operator calls:
     * - `getValue()` on the delegate object
     * - `setValue()` on the delegate object (only for `var` properties)
     * - `provideDelegate()` on the delegate expression (if applicable)
     *
     * #### Example
     *
     * ```kotlin
     * val name: String by lazy { "John" }
     * //               ^________________^
     * ```
     *
     * Calling `resolveCall()` on the [KtPropertyDelegate] returns a [KaDelegatedPropertyCall] containing the
     * desugared operator calls if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on delegated properties
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun KtPropertyDelegate.resolveCall(): KaDelegatedPropertyCall?

    /**
     * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html)
     * for the call corresponding to the given [KtResolvableCall].
     *
     * In contrast, [resolveCall] returns only the final result, i.e., the most specific callable that passes all
     * compatibility checks.
     *
     * @see resolveCall
     */
    @KaExperimentalApi
    @OptIn(KtExperimentalApi::class)
    public fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidate>

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
context(session: KaSession)
public fun KtResolvable.tryResolveSymbols(): KaSymbolResolutionAttempt? {
    return with(session) {
        tryResolveSymbols()
    }
}

/**
 * Resolves symbols for the given [KtResolvable].
 *
 * Returns all resolved [KaSymbol]s if successful; otherwise, an empty list. Might contain multiple symbols
 * for a compound case
 *
 * @see tryResolveSymbols
 * @see resolveSymbol
 * @see KaSymbolResolutionSuccess
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public fun KtResolvable.resolveSymbols(): Collection<KaSymbol> {
    return with(session) {
        resolveSymbols()
    }
}

/**
 * Resolves a single symbol for the given [KtResolvable].
 *
 * Returns the [KaSymbol] if there is exactly one target; otherwise, `null`
 *
 * @see tryResolveSymbols
 * @see resolveSymbols
 * @see KaSymbolResolutionSuccess
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public fun KtResolvable.resolveSymbol(): KaSymbol? {
    return with(session) {
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
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol? {
    return with(session) {
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
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol? {
    return with(session) {
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
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol? {
    return with(session) {
        resolveSymbol()
    }
}

/**
 * Resolves the constructor symbol referenced by the given [KtConstructorDelegationReferenceExpression].
 *
 * #### Example
 *
 * ```kotlin
 * open class Base(val i: Int)
 *
 * class Derived : Base {
 *     constructor() : this(0)
 *     //              ^^^^
 *
 *     constructor(x: Int) : super(x)
 *     //                    ^^^^^
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtConstructorDelegationReferenceExpression] (either `this` or `super`) returns the
 * [KaConstructorSymbol] of the target constructor if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on constructor delegation calls
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtConstructorDelegationReferenceExpression.resolveSymbol(): KaConstructorSymbol? {
    return with(session) {
        resolveSymbol()
    }
}

/**
 * Resolves the function symbol targeted by the given [KtCallElement].
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
 * Calling `resolveSymbol()` on the [KtCallElement] (`foo(42)`) returns the [KaFunctionSymbol] of `foo`
 * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on call elements
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtCallElement.resolveSymbol(): KaFunctionSymbol? {
    return with(session) {
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
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol? {
    return with(session) {
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
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtArrayAccessExpression.resolveSymbol(): KaNamedFunctionSymbol? {
    return with(session) {
        resolveSymbol()
    }
}

/**
 * Resolves the function symbol targeted by the given [KtCollectionLiteralExpression].
 *
 * #### Example
 *
 * ```kotlin
 * annotation class Anno(val arr: IntArray)
 *
 * @Anno([1, 2, 3])
 * //    ^^^^^^^^^ resolves to the `intArrayOf` function
 * fun use() {}
 * ```
 *
 * Calling `resolveSymbol()` on a [KtCollectionLiteralExpression] (`[1, 2, 3]`) returns the [KaNamedFunctionSymbol]
 * of the corresponding array factory (e.g., `arrayOf`, `intArrayOf`) if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on collection literal expressions
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtCollectionLiteralExpression.resolveSymbol(): KaNamedFunctionSymbol? {
    return with(session) {
        resolveSymbol()
    }
}

/**
 * Resolves the constructor symbol referenced by the given [KtEnumEntrySuperclassReferenceExpression].
 *
 * #### Example
 *
 * ```kotlin
 * enum class EnumWithConstructor(val x: Int) {
 *     Entry(1)
 * //      ^ resolves to the constructor of `EnumWithConstructor`
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtEnumEntrySuperclassReferenceExpression] (``) returns the
 * [KaConstructorSymbol] of the enum class constructor if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on enum entry superclass constructor calls
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtEnumEntrySuperclassReferenceExpression.resolveSymbol(): KaConstructorSymbol? {
    return with(session) {
        resolveSymbol()
    }
}

/**
 * Resolves the declaration symbol targeted by the given [KtLabelReferenceExpression].
 *
 * #### Example
 *
 * ```kotlin
 * fun myAction(action: () -> Unit) {
 *     action {
 *         return@action // resolves to the anonymous function
 * //            ^^^^^^^
 *     }
 *
 *     return@main
 * //        ^^^^^
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtLabelReferenceExpression] (`@action` and `@main`) returns the corresponding [KaDeclarationSymbol]
 * of the labeled declaration if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or
 * ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on label references
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtLabelReferenceExpression.resolveSymbol(): KaDeclarationSymbol? {
    return with(session) {
        resolveSymbol()
    }
}

/**
 * Resolves the function symbol targeted by the given [KtReturnExpression].
 *
 * #### Example
 *
 * ```kotlin
 * fun foo() {
 *     return
 * //  ^^^^^^ resolves to `foo`
 * }
 *
 * fun main() {
 *     listOf(1).forEach label@{
 *         if (it == 0) return@label
 * //                   ^^^^^^^^^^^^ resolves to the anonymous function of this lambda
 *     }
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtReturnExpression] (`return` or `return@label`) returns the [KaFunctionSymbol] of the enclosing function
 * (for unlabeled returns) or of the labeled target (for `return@label`) if resolution succeeds; otherwise, it returns
 * `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on return expressions
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtReturnExpression.resolveSymbol(): KaFunctionSymbol? {
    return with(session) {
        resolveSymbol()
    }
}

/**
 * Resolves the operator function symbol targeted by the given [KtWhenConditionInRange].
 *
 * #### Example
 *
 * ```kotlin
 * fun test(x: Int) {
 *     when (x) {
 *         in 1..10 -> {}
 * //      ^^^^^^^^ resolves to a call of `IntRange.contains`
 *
 *         !in setOf(1, 2, 3) -> {}
 * //      ^^^^^^^^^^^^^^^^^^ resolves to a call of `Set<Int>.contains`
 *     }
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtWhenConditionInRange] (`in 1..10` or `!in setOf(1, 2, 3)`) returns the [KaNamedFunctionSymbol]
 * of the labeled declaration if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on `in`/`!in`
 * range conditions inside `when` entries
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtWhenConditionInRange.resolveSymbol(): KaNamedFunctionSymbol? {
    return with(session) {
        resolveSymbol()
    }
}

/**
 * Resolves the callable symbol targeted by the given [KtDestructuringDeclarationEntry].
 *
 * #### Example
 *
 * ```kotlin
 * data class Point(val x: Int, val y: Int)
 *
 * fun test(p: Point) {
 *     val (x, y) = p
 * //       ^ resolves to `component1`
 * //          ^ resolves to `component2`
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtDestructuringDeclarationEntry] returns the [KaCallableSymbol] of the corresponding
 * `componentN` function (for positional destructuring) or the accessed property (for name-based destructuring)
 * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on destructuring declaration entries
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtDestructuringDeclarationEntry.resolveSymbol(): KaCallableSymbol? {
    return with(session) {
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
context(session: KaSession)
public fun KtResolvableCall.tryResolveCall(): KaCallResolutionAttempt? {
    return with(session) {
        tryResolveCall()
    }
}

/**
 * Resolves the call for the given [KtResolvableCall].
 *
 * ### Usage Example:
 * ```kotlin
 * fun KaSession.resolveSymbol(expression: KtCallExpression): KaSymbol? {
 *   val call = expression.resolveCall() ?: return null
 *   val callableCall = call as? KaSingleCall<*, *> ?: return null
 *   return callableCall.symbol
 * }
 * ```
 *
 * Returns the resolved [KaSingleOrMultiCall] on success; otherwise, `null`
 *
 * @see tryResolveCall
 * @see collectCallCandidates
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public fun KtResolvableCall.resolveCall(): KaSingleOrMultiCall? {
    return with(session) {
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
context(session: KaSession)
public fun KtAnnotationEntry.resolveCall(): KaAnnotationCall? {
    return with(session) {
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
context(session: KaSession)
public fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>? {
    return with(session) {
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
context(session: KaSession)
public fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtConstructorDelegationReferenceExpression] to a delegated constructor call.
 *
 * #### Example
 *
 * ```kotlin
 * open class Base(val i: Int)
 *
 * class Derived : Base {
 *     constructor() : this(0)
 *     //              ^^^^
 *
 *     constructor(x: Int) : super(x)
 *     //                    ^^^^^
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
context(session: KaSession)
public fun KtConstructorDelegationReferenceExpression.resolveCall(): KaDelegatedConstructorCall? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtCallElement] to a function call.
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
 * Returns the corresponding [KaSingleCall] if resolution succeeds;
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
context(session: KaSession)
public fun KtCallElement.resolveCall(): KaFunctionCall<*>? {
    return with(session) {
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
 * Returns the corresponding [KaSingleCall] if resolution succeeds;
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
context(session: KaSession)
public fun KtCallableReferenceExpression.resolveCall(): KaSingleCall<*, *>? {
    return with(session) {
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
context(session: KaSession)
public fun KtArrayAccessExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtCollectionLiteralExpression] to a simple function call representing the corresponding
 * array factory invocation.
 *
 * #### Example
 *
 * ```kotlin
 * annotation class Anno(val arr: IntArray)
 *
 * @Anno([1, 2, 3])
 * //    ^^^^^^^^^ resolves to a call of `intArrayOf`
 * fun use() {}
 * ```
 *
 * Returns the corresponding [KaSimpleFunctionCall] if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on collection literal expressions
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtCollectionLiteralExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtEnumEntrySuperclassReferenceExpression] to a delegated constructor call.
 *
 * #### Example
 *
 * ```kotlin
 * enum class EnumWithConstructor(val x: Int) {
 *     Entry(1)
 * //      ^ resolves to the constructor of `EnumWithConstructor`
 * }
 * ```
 *
 * Returns the corresponding [KaDelegatedConstructorCall] if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on enum entry superclass constructor calls
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtEnumEntrySuperclassReferenceExpression.resolveCall(): KaDelegatedConstructorCall? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtWhenConditionInRange] to a simple function call representing the corresponding
 * `contains` operator invocation used by the `in`/`!in` branch condition.
 *
 * #### Example
 *
 * ```kotlin
 * fun test(x: Int) {
 *     when (x) {
 *         in 1..10 -> {}
 * //      ^^^^^^^^ resolves to a call of `IntRange.contains`
 *
 *         !in setOf(1, 2, 3) -> {}
 * //      ^^^^^^^^^^^^^^^^^^ resolves to a call of `Set<Int>.contains`
 *     }
 * }
 * ```
 *
 * Returns the corresponding [KaSimpleFunctionCall] if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on `in`/`!in`
 * range conditions inside `when` entries
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtWhenConditionInRange.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtDestructuringDeclarationEntry] to a call representing the `componentN` invocation
 * (for positional destructuring) or the property access (for name-based destructuring).
 *
 * #### Example
 *
 * ```kotlin
 * data class Point(val x: Int, val y: Int)
 *
 * fun test(p: Point) {
 *     val (x, y) = p
 * //       ^ resolves to a call of `component1`
 * //          ^ resolves to a call of `component2`
 * }
 * ```
 *
 * Returns the corresponding [KaSingleCall] if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on destructuring declaration entries
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtDestructuringDeclarationEntry.resolveCall(): KaSingleCall<*, *>? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtForExpression] to a [KaForLoopCall] representing the desugared `for` loop.
 *
 * A `for` loop desugars into three operator calls:
 * - `iterator()` on the loop range expression
 * - `hasNext()` on the iterator
 * - `next()` on the iterator
 *
 * #### Example
 *
 * ```kotlin
 * for (item in list) {
 *     println(item)
 * }
 * ```
 *
 * Calling `resolveCall()` on the [KtForExpression] returns a [KaForLoopCall] containing the three
 * desugared operator calls if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on `for` loops
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtForExpression.resolveCall(): KaForLoopCall? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Resolves the given [KtPropertyDelegate] to a [KaDelegatedPropertyCall] representing the desugared delegated property.
 *
 * A delegated property desugars into up to three operator calls:
 * - `getValue()` on the delegate object
 * - `setValue()` on the delegate object (only for `var` properties)
 * - `provideDelegate()` on the delegate expression (if applicable)
 *
 * #### Example
 *
 * ```kotlin
 * val name: String by lazy { "John" }
 * //               ^________________^
 * ```
 *
 * Calling `resolveCall()` on the [KtPropertyDelegate] returns a [KaDelegatedPropertyCall] containing the
 * desugared operator calls if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on delegated properties
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KtPropertyDelegate.resolveCall(): KaDelegatedPropertyCall? {
    return with(session) {
        resolveCall()
    }
}

/**
 * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html)
 * for the call corresponding to the given [KtResolvableCall].
 *
 * In contrast, [resolveCall] returns only the final result, i.e., the most specific callable that passes all
 * compatibility checks.
 *
 * @see resolveCall
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidate> {
    return with(session) {
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
context(session: KaSession)
public fun KtReference.resolveToSymbols(): Collection<KaSymbol> {
    return with(session) {
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
context(session: KaSession)
public fun KtReference.resolveToSymbol(): KaSymbol? {
    return with(session) {
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
context(session: KaSession)
public fun KtReference.isImplicitReferenceToCompanion(): Boolean {
    return with(session) {
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
context(session: KaSession)
public val KtReference.usesContextSensitiveResolution: Boolean
    get() = with(session) { usesContextSensitiveResolution }

/**
 * Resolves the given [KtElement] to a [KaCallInfo] object. [KaCallInfo] either contains a successfully resolved call or an error with
 * a list of candidate calls and a diagnostic.
 *
 * Returns `null` if the element does not correspond to a call.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(session: KaSession)
public fun KtElement.resolveToCall(): KaCallInfo? {
    return with(session) {
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
context(session: KaSession)
public fun KtElement.resolveToCallCandidates(): List<KaCallCandidateInfo> {
    return with(session) {
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
context(session: KaSession)
public fun KDocReference.resolveToSymbolWithClassicKDocResolver(): KaSymbol? {
    return with(session) {
        resolveToSymbolWithClassicKDocResolver()
    }
}
