/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.*

/**
 * Attempts to resolve a symbol for the given [KtResolvable].
 *
 * Returns a [KaSymbolResolutionAttempt] that describes either success ([KaSymbolResolutionSuccess])
 * or failure ([KaSymbolResolutionError]), or `null` if no result is available.
 *
 * In contract to [tryResolveCall], it could represent any [KaSymbol], not only [KaCallableSymbol].
 *
 * In most cases, a not-null result of [tryResolveCall] will represent the same symbol. The only exceptions are:
 * - [KtNameReferenceExpression]
 * - [KtOperationReferenceExpression]
 * - [KtEnumEntrySuperclassReferenceExpression]
 *
 * For which the behavior could be different depending on the context.
 *
 * The main idea is that [tryResolveSymbols] could represent more cases, so it prefers exactly the referenced symbol
 * and not the parent call. For more details, see the mentioned elements.
 *
 * See [References and Calls](https://kotlin.github.io/analysis-api/references-and-calls.html) for a top-level overview.
 *
 * @see KaSymbolResolutionSuccess
 * @see KaSymbolResolutionError
 */
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
public fun KtResolvable.tryResolveSymbols(): KaSymbolResolutionAttempt? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.tryResolveSymbols(this)
}

/**
 * Resolves symbols for the given [KtResolvable].
 *
 * Returns all resolved [KaSymbol]s if successful; otherwise, an empty list. Might contain multiple symbols
 * for a compound case
 *
 * In contract to [resolveCall], it could represent any [KaSymbol], not only [KaCallableSymbol].
 *
 * In most cases, a not-null result of [resolveCall] will represent the same symbol. The only exceptions are:
 * - [KtNameReferenceExpression]
 * - [KtOperationReferenceExpression]
 * - [KtEnumEntrySuperclassReferenceExpression]
 *
 * For which the behavior could be different depending on the context.
 *
 * The main idea is that [resolveSymbols] could represent more cases, so it prefers exactly the referenced symbol
 * and not the parent call. For more details, see the mentioned elements.
 *
 * @see tryResolveSymbols
 * @see resolveSymbol
 * @see KaSymbolResolutionSuccess
 */
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
public fun KtResolvable.resolveSymbols(): Collection<KaSymbol> {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbols(this)
}

/**
 * Resolves a single symbol for the given [KtResolvable].
 *
 * Returns the [KaSymbol] if there is exactly one target; otherwise, `null`
 *
 * In contract to [resolveCall], it could represent any [KaSymbol], not only [KaCallableSymbol].
 *
 * In most cases, a not-null result of [resolveCall] will represent the same symbol. The only exceptions are:
 * - [KtNameReferenceExpression]
 * - [KtOperationReferenceExpression]
 * - [KtEnumEntrySuperclassReferenceExpression]
 *
 * For which the behavior could be different depending on the context.
 *
 * The main idea is that [resolveSymbol] could represent more cases, so it prefers exactly the referenced symbol
 * and not the parent call. For more details, see the mentioned elements.
 *
 * @see tryResolveSymbols
 * @see resolveSymbols
 * @see KaSymbolResolutionSuccess
 */
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
public fun KtResolvable.resolveSymbol(): KaSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtAnnotationEntry.resolveSymbol(): KaConstructorSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtSuperTypeCallEntry.resolveSymbol(): KaConstructorSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorDelegationCall.resolveSymbol(): KaConstructorSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorDelegationReferenceExpression.resolveSymbol(): KaConstructorSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtCallElement.resolveSymbol(): KaFunctionSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtCallableReferenceExpression.resolveSymbol(): KaCallableSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtArrayAccessExpression.resolveSymbol(): KaNamedFunctionSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtCollectionLiteralExpression.resolveSymbol(): KaNamedFunctionSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the enum class symbol referenced by the given [KtEnumEntrySuperclassReferenceExpression].
 *
 * #### Example
 *
 * ```kotlin
 * enum class EnumWithConstructor(val x: Int) {
 *     Entry(1)
 * //      ^ resolves to the enum class `EnumWithConstructor`
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtEnumEntrySuperclassReferenceExpression] returns the [KaNamedClassSymbol] of
 * the enclosing enum class if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * Mirrors how [KtNameReferenceExpression] prefers the class over the constructor: while the surrounding
 * super-type call ([resolveCall]) maps to the constructor, the reference itself denotes the class.
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on enum entry super-type references
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtEnumEntrySuperclassReferenceExpression.resolveSymbol(): KaNamedClassSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtLabelReferenceExpression.resolveSymbol(): KaDeclarationSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtReturnExpression.resolveSymbol(): KaFunctionSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtWhenConditionInRange.resolveSymbol(): KaNamedFunctionSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
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
@KaExperimentalApi
context(session: KaSession)
public fun KtDestructuringDeclarationEntry.resolveSymbol(): KaCallableSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the callable symbol targeted by the given [KtQualifiedExpression].
 *
 * #### Example
 *
 * ```kotlin
 * val len = str.length
 * //        ^________^
 * ```
 *
 * Calling `resolveSymbol()` on the [KtQualifiedExpression] (`str.length`) returns the [KaCallableSymbol] of `length`
 * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on qualified expressions
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtQualifiedExpression.resolveSymbol(): KaCallableSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the constructor symbol referenced by the given [KtConstructorCalleeExpression].
 *
 * #### Example
 *
 * ```kotlin
 * open class Base(i: Int)
 *
 * class Derived : Base(1)
 * //              ^^^^
 * ```
 *
 * Calling `resolveSymbol()` on the [KtConstructorCalleeExpression] (`Base`) returns the [KaConstructorSymbol] of `Base`'s
 * constructor if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on constructor callee expressions
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorCalleeExpression.resolveSymbol(): KaConstructorSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the declaration symbol referenced by the given [KtInstanceExpressionWithLabel].
 *
 * #### Example
 *
 * ```kotlin
 * class Foo {
 *     fun bar() = this
 * //              ^^^^  resolves to the class `Foo`
 * }
 *
 * fun String.ext() = this
 * //                 ^^^^  resolves to the receiver parameter of `ext`
 *
 * open class Base {
 *     open fun baz() {}
 * }
 *
 * class Derived : Base() {
 *     override fun baz() {
 *         super.baz()
 * //      ^^^^^  resolves to the class `Base`
 *     }
 * }
 * ```
 *
 * Calling `resolveSymbol()` on a [KtInstanceExpressionWithLabel] (`this` or `super`) returns the [KaDeclarationSymbol]
 * of the referenced class, receiver, or other target declaration if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on instance expressions
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtInstanceExpressionWithLabel.resolveSymbol(): KaDeclarationSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the classifier symbol referenced by the given [KtNullableType].
 *
 * #### Example
 *
 * ```kotlin
 * val name: String? = null
 * //        ^^^^^^^  resolves to `kotlin.String`
 * ```
 *
 * Resolution unwraps the nullability marker and recurses into the inner type element. The result is the
 * [KaClassifierSymbol] of the underlying class, type alias, or type parameter if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or when the inner element has no single classifier).
 *
 * Unlike [KtUserType], a [KtNullableType] cannot stand for a package qualifier, so the result is always a
 * classifier when present.
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on nullable types
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtNullableType.resolveSymbol(): KaClassifierSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the synthetic function class symbol referenced by the given [KtFunctionType].
 *
 * #### Example
 *
 * ```kotlin
 * val a: (Int, String) -> Boolean = TODO()
 * //     ^^^^^^^^^^^^^^^^^^^^^^^   resolves to `kotlin.Function2`
 *
 * val b: suspend () -> Unit = TODO()
 * //     ^^^^^^^^^^^^^^^^^   resolves to `kotlin.coroutines.SuspendFunction0`
 * ```
 *
 * Returns the [KaClassSymbol] of the corresponding `FunctionN`/`SuspendFunctionN` class (the receiver and
 * context parameters count as parameters towards the arity), or `null` if resolution fails.
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on function types
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtFunctionType.resolveSymbol(): KaClassSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the classifier symbol referenced by the given [KtTypeReference].
 *
 * #### Example
 *
 * ```kotlin
 * val a: String = ""
 * //     ^^^^^^  resolves to `kotlin.String`
 *
 * val b: List<Int>? = null
 * //     ^^^^^^^^^^  resolves to `kotlin.collections.List`
 *
 * val c: (Int) -> Int = { it }
 * //     ^^^^^^^^^^^^  resolves to `kotlin.Function1`
 * ```
 *
 * Resolution delegates to the inner [KtTypeReference.typeElement][org.jetbrains.kotlin.psi.KtTypeReference.typeElement]
 * and returns the underlying [KaClassifierSymbol] (a class, type alias, or type parameter), or `null`
 * for type elements that don't denote a single classifier (e.g. `dynamic` and intersection types).
 *
 * Unlike [KtUserType], a [KtTypeReference] never stands for the package portion of a qualified path:
 * the inner qualifier chain is built from raw `KtUserType` nodes and is never wrapped in its own
 * type reference, so the result is always a classifier when present.
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on type references
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtTypeReference.resolveSymbol(): KaClassifierSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the classifier symbol referenced by the given [KtClassLiteralExpression] (`Foo::class`).
 *
 * #### Example
 *
 * ```kotlin
 * val a = String::class
 * //      ^^^^^^^^^^^^^   resolves to `kotlin.String`
 *
 * val b = kotlin.String::class
 * //      ^^^^^^^^^^^^^^^^^^^^   resolves to `kotlin.String`
 * ```
 *
 * Resolution delegates to the receiver expression on the left of `::class`. Returns the underlying
 * [KaClassifierSymbol] of the referenced class, type alias, or type parameter if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on class literal expressions
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtClassLiteralExpression.resolveSymbol(): KaClassifierSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the classifier symbol referenced by the given [KtSuperTypeEntry] (the no-parens form `class Foo : Bar`).
 *
 * #### Example
 *
 * ```kotlin
 * class Foo : Runnable
 * //          ^^^^^^^^  resolves to `java.lang.Runnable`
 * ```
 *
 * Resolution delegates to the entry's [KtSuperTypeEntry.getTypeReference]. Returns the underlying
 * [KaClassifierSymbol] of the supertype if resolution succeeds; otherwise, it returns `null`.
 *
 * Companion to [KtSuperTypeCallEntry.resolveSymbol], which returns the [KaConstructorSymbol] for the
 * `class Foo : Bar()` form.
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on supertype entries
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtSuperTypeEntry.resolveSymbol(): KaClassifierSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}

/**
 * Resolves the classifier symbol referenced by the given [KtDelegatedSuperTypeEntry] (`class Foo : Bar by baz`).
 *
 * #### Example
 *
 * ```kotlin
 * class Foo(b: Base) : Base by b
 * //                   ^^^^      resolves to `Base`
 * ```
 *
 * Resolution delegates to the entry's [KtDelegatedSuperTypeEntry.getTypeReference] — the supertype side of the
 * `by` clause, not the delegate expression. Returns the underlying [KaClassifierSymbol] if resolution succeeds;
 * otherwise, it returns `null`.
 *
 * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on delegated supertype entries
 *
 * @see tryResolveSymbols
 * @see KtResolvable.resolveSymbol
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtDelegatedSuperTypeEntry.resolveSymbol(): KaClassifierSymbol? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveSymbol(this)
}
