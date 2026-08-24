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
 * Returns a [KaCallResolutionAttempt], or `null` if no result is available.
 *
 * See [References and Calls](https://kotlin.github.io/analysis-api/references-and-calls.html) for a top-level overview.
 *
 * @see resolveSuccessfulCall
 */
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
public fun KtResolvableCall.tryResolveCall(): KaCallResolutionAttempt? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.tryResolveCall(this)
}

/**
 * Attempts to resolve the given [KtForExpression] to a [KaForLoopCallResolutionAttempt] containing the individual
 * resolution results for each desugared operator call (`iterator`, `hasNext`, `next`).
 *
 * This is a specialized counterpart of [KtResolvableCall.tryResolveCall] focused specifically on `for` loops.
 *
 * @see KtForExpression.resolveSuccessfulCall
 * @see KtResolvableCall.tryResolveCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtForExpression.tryResolveCall(): KaForLoopCallResolutionAttempt? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.tryResolveCall(this)
}

/**
 * Attempts to resolve the given [KtPropertyDelegate] to a [KaDelegatedPropertyCallResolutionAttempt] containing the individual
 * resolution results for each desugared operator call (`getValue`, `setValue`, `provideDelegate`).
 *
 * This is a specialized counterpart of [KtResolvableCall.tryResolveCall] focused specifically on delegated properties.
 *
 * @see KtPropertyDelegate.resolveSuccessfulCall
 * @see KtResolvableCall.tryResolveCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtPropertyDelegate.tryResolveCall(): KaDelegatedPropertyCallResolutionAttempt? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.tryResolveCall(this)
}

/**
 * Resolves the call for the given [KtResolvableCall].
 *
 * ### Usage Example:
 * ```kotlin
 * fun KaSession.resolveSymbol(expression: KtCallExpression): KaSymbol? {
 *   val call = expression.resolveSuccessfulCall() ?: return null
 *   val callableCall = call as? KaSimpleCall<*, *> ?: return null
 *   return callableCall.symbol
 * }
 * ```
 *
 * Returns the resolved [KaSimpleOrMultiCall] on success; otherwise, `null`
 *
 * @see tryResolveCall
 * @see collectCallCandidates
 */
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
public fun KtResolvableCall.resolveSuccessfulCall(): KaSimpleOrMultiCall? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtResolvableCall.resolveSuccessfulCall].
 *
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
public fun KtResolvableCall.resolveCall(): KaSimpleOrMultiCall? = resolveSuccessfulCall()

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
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on annotation entries.
 * Use [collectCallCandidates] to inspect all candidates considered during overload resolution
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtAnnotationEntry.resolveSuccessfulCall(): KaAnnotationCall? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtAnnotationEntry.resolveSuccessfulCall].
 *
 * @see KtAnnotationEntry.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtAnnotationEntry.resolveCall(): KaAnnotationCall? = resolveSuccessfulCall()

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
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on supertype constructor calls
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtSuperTypeCallEntry.resolveSuccessfulCall(): KaFunctionCall<KaConstructorSymbol>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtSuperTypeCallEntry.resolveSuccessfulCall].
 *
 * @see KtSuperTypeCallEntry.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtSuperTypeCallEntry.resolveCall(): KaFunctionCall<KaConstructorSymbol>? = resolveSuccessfulCall()

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
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on constructor delegation calls
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorDelegationCall.resolveSuccessfulCall(): KaDelegatedConstructorCall? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtConstructorDelegationCall.resolveSuccessfulCall].
 *
 * @see KtConstructorDelegationCall.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorDelegationCall.resolveCall(): KaDelegatedConstructorCall? = resolveSuccessfulCall()

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
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on constructor delegation calls
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorDelegationReferenceExpression.resolveSuccessfulCall(): KaDelegatedConstructorCall? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtConstructorDelegationReferenceExpression.resolveSuccessfulCall].
 *
 * @see KtConstructorDelegationReferenceExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorDelegationReferenceExpression.resolveCall(): KaDelegatedConstructorCall? = resolveSuccessfulCall()

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
 * Returns the corresponding [KaSimpleCall] if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on call elements
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtCallElement.resolveSuccessfulCall(): KaFunctionCall<*>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtCallElement.resolveSuccessfulCall].
 *
 * @see KtCallElement.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtCallElement.resolveCall(): KaFunctionCall<*>? = resolveSuccessfulCall()

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
 * Returns the corresponding [KaCallableReferenceCall] if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on callable reference expressions
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtCallableReferenceExpression.resolveSuccessfulCall(): KaCallableReferenceCall<*, *>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtCallableReferenceExpression.resolveSuccessfulCall].
 *
 * @see KtCallableReferenceExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtCallableReferenceExpression.resolveCall(): KaCallableReferenceCall<*, *>? = resolveSuccessfulCall()

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
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on array access operations.
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
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtArrayAccessExpression.resolveSuccessfulCall(): KaFunctionCall<KaNamedFunctionSymbol>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtArrayAccessExpression.resolveSuccessfulCall].
 *
 * @see KtArrayAccessExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtArrayAccessExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSuccessfulCall()

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
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on collection literal expressions
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtCollectionLiteralExpression.resolveSuccessfulCall(): KaFunctionCall<KaNamedFunctionSymbol>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtCollectionLiteralExpression.resolveSuccessfulCall].
 *
 * @see KtCollectionLiteralExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtCollectionLiteralExpression.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSuccessfulCall()

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
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on enum entry superclass constructor calls
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtEnumEntrySuperclassReferenceExpression.resolveSuccessfulCall(): KaDelegatedConstructorCall? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtEnumEntrySuperclassReferenceExpression.resolveSuccessfulCall].
 *
 * @see KtEnumEntrySuperclassReferenceExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtEnumEntrySuperclassReferenceExpression.resolveCall(): KaDelegatedConstructorCall? = resolveSuccessfulCall()

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
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on `in`/`!in`
 * range conditions inside `when` entries
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtWhenConditionInRange.resolveSuccessfulCall(): KaFunctionCall<KaNamedFunctionSymbol>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtWhenConditionInRange.resolveSuccessfulCall].
 *
 * @see KtWhenConditionInRange.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtWhenConditionInRange.resolveCall(): KaFunctionCall<KaNamedFunctionSymbol>? = resolveSuccessfulCall()

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
 * Returns the corresponding [KaSimpleCall] if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on destructuring declaration entries
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtDestructuringDeclarationEntry.resolveSuccessfulCall(): KaSimpleCall<*, *>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtDestructuringDeclarationEntry.resolveSuccessfulCall].
 *
 * @see KtDestructuringDeclarationEntry.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtDestructuringDeclarationEntry.resolveCall(): KaSimpleCall<*, *>? = resolveSuccessfulCall()

/**
 * Resolves the given [KtQualifiedExpression] to a call representing the member or extension access.
 *
 * #### Example
 *
 * ```kotlin
 * val len = str.length
 * //        ^________^
 * ```
 *
 * Calling `resolveSuccessfulCall()` on the [KtQualifiedExpression] (`str.length`) returns the corresponding [KaSimpleCall]
 * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on qualified expressions
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtQualifiedExpression.resolveSuccessfulCall(): KaSimpleCall<*, *>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtQualifiedExpression.resolveSuccessfulCall].
 *
 * @see KtQualifiedExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtQualifiedExpression.resolveCall(): KaSimpleCall<*, *>? = resolveSuccessfulCall()

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
 * Calling `resolveSuccessfulCall()` on the [KtForExpression] returns a [KaForLoopCall] containing the three
 * desugared operator calls if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on `for` loops
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtForExpression.resolveSuccessfulCall(): KaForLoopCall? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtForExpression.resolveSuccessfulCall].
 *
 * @see KtForExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtForExpression.resolveCall(): KaForLoopCall? = resolveSuccessfulCall()

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
 * Calling `resolveSuccessfulCall()` on the [KtPropertyDelegate] returns a [KaDelegatedPropertyCall] containing the
 * desugared operator calls if resolution succeeds; otherwise, it returns `null`
 * (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on delegated properties
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtPropertyDelegate.resolveSuccessfulCall(): KaDelegatedPropertyCall? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtPropertyDelegate.resolveSuccessfulCall].
 *
 * @see KtPropertyDelegate.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtPropertyDelegate.resolveCall(): KaDelegatedPropertyCall? = resolveSuccessfulCall()

/**
 * Resolves the given [KtConstructorCalleeExpression] to a constructor call.
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
 * Returns the corresponding [KaFunctionCall] if resolution succeeds;
 * otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on constructor callee expressions
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorCalleeExpression.resolveSuccessfulCall(): KaFunctionCall<KaConstructorSymbol>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtConstructorCalleeExpression.resolveSuccessfulCall].
 *
 * @see KtConstructorCalleeExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtConstructorCalleeExpression.resolveCall(): KaFunctionCall<KaConstructorSymbol>? = resolveSuccessfulCall()

/**
 * Resolves the given [KtNameReferenceExpression] to a call representing the referenced declaration.
 *
 * #### Example
 *
 * ```kotlin
 * fun foo() {}
 *
 * val x = foo
 * //      ^^^
 * ```
 *
 * Calling `resolveSuccessfulCall()` on the [KtNameReferenceExpression] (`foo`) returns the corresponding [KaSimpleCall]
 * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * This is a specialized counterpart of [KtResolvableCall.resolveSuccessfulCall] focused specifically on name reference expressions
 *
 * @see tryResolveCall
 * @see KtResolvableCall.resolveSuccessfulCall
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtNameReferenceExpression.resolveSuccessfulCall(): KaSimpleCall<*, *>? {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.resolveCall(this)
}

/**
 * The former name of [KtNameReferenceExpression.resolveSuccessfulCall].
 *
 * @see KtNameReferenceExpression.resolveSuccessfulCall
 */
@Deprecated(
    message = "Use 'resolveSuccessfulCall()' instead",
    replaceWith = ReplaceWith(
        expression = "resolveSuccessfulCall()",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall"],
    ),
)
@KaExperimentalApi
context(session: KaSession)
public fun KtNameReferenceExpression.resolveCall(): KaSimpleCall<*, *>? = resolveSuccessfulCall()

/**
 * Returns all candidates considered during [overload resolution](https://kotlinlang.org/spec/overload-resolution.html)
 * for the call corresponding to the given [KtResolvableCall].
 *
 * In contrast, [resolveSuccessfulCall] returns only the final result, i.e., the most specific callable that passes all
 * compatibility checks.
 *
 * @see resolveSuccessfulCall
 */
@KaExperimentalApi
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
public fun KtResolvableCall.collectCallCandidates(): List<KaCallCandidate> {
    @OptIn(KaImplementationDetail::class)
    return internals.resolver.collectCallCandidates(this)
}
