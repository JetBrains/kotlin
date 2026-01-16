/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaExpressionInformationProvider : KaSessionComponent {
    /**
     * The [symbol][KaCallableSymbol] of the callable which the given [KtReturnExpression] returns from.
     */
    @Deprecated("The API is obsolete. Use `resolveSymbol()` instead.", ReplaceWith("resolveSymbol()"))
    @KaIdeApi
    public val KtReturnExpression.targetSymbol: KaCallableSymbol?

    /**
     * Computes the missing cases of the given [KtWhenExpression].
     *
     * The computed missing cases are not affected by the existence or absence of an `else` branch.
     *
     * #### Example
     *
     * In the following code, `Direction.WEST` and `Direction.EAST` are missing branches:
     *
     * ```
     * enum class Direction {
     *   NORTH, SOUTH, WEST, EAST
     * }
     *
     * foo = when(direction) {
     *   Direction.NORTH -> 1
     *   Direction.SOUTH -> 2
     *   else -> 3
     * }
     * ```
     *
     * If the [KtWhenExpression] has no subject, then the `else` is reported as missing even if it is explicitly present:
     *
     * ```
     * fun test() {
     *     when {
     *         true -> {}
     *         else -> {}
     *     }
     * }
     * ```
     */
    @KaIdeApi
    public fun KtWhenExpression.computeMissingCases(): List<WhenMissingCase>

    /**
     * Whether the value of the given [KtExpression] is used. In other words, returns `true` if the value of the expression is not safe to
     * discard.
     *
     * [isUsedAsExpression] performs a **conservative check** instead of exhaustive control-flow analysis. For example, `x` in the following
     * example *is possibly used*, even though the value is never consumed at runtime: `x + try { throw Exception() } finally { return }`.
     *
     * #### Example
     *
     * In the following examples, `x` is used as a value (`x.isUsedAsExpression == true`):
     *
     *   - `if (x) { ... } else { ... }`
     *   - `val a = x`
     *   - `x + 8`
     *   - `when (x) { 1 -> ...; else -> ... }`
     *
     * In these expressions, `x` is not used as a value (`x.isUsedAsExpression == false`)
     *
     *   - `run { x; println(50) }`
     *   - `when (x) { else -> ... }`
     */
    public val KtExpression.isUsedAsExpression: Boolean

    /**
     * Whether the value of the given [KtExpression] is used as the resulting expression of some lambda block.
     *
     * Note that [isUsedAsResultOfLambda] performs a **conservative check** instead of exhaustive control-flow analysis and
     * `isUsedAsResultOfLambda` being `true` doesn't imply that the containing lambda itself is used.
     *
     * It's also vital to not confuse lambda expressions with regular scope blocks (like `if` branches).
     * #### Example
     *
     * In the following examples, `x` is used as a result of a lambda (`x.isUsedAsResultOfLambda == true`):
     *
     *   - `{ x -> println(0); x }`
     *   - `{ { x }; 5 }`
     *
     * In these expressions, `x` is not used as a result of a lambda (`x.isUsedAsResultOfLambda == false`)
     *
     *   - `{ x -> println(0); x + 1 }`
     *   - `{ x; println(50) }`
     *   - `{ if (true) { x } else { x } }`
     *   - `fun(x: Int) = x`
     */
    @KaExperimentalApi
    public val KtExpression.isUsedAsResultOfLambda: Boolean

    /**
     * Indicates whether this expression (property reference) is [stable](https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability) for smart casting purposes.
     *
     * An expression is considered stable if its value cannot be changed via means external to the control flow graph
     * at the current program point. Stability is a prerequisite for smart casts: only stable expressions can be
     * smart-cast after a type check.
     *
     * #### Stable expressions
     *
     * The following expressions are considered stable (`isStable == true`):
     *
     *   - Immutable local variables (`val`) without delegation or custom getters
     *   - Mutable local variables (`var`) that are *effectively immutable* at the usage site (i.e., not modified
     *     between the type check and usage, and not captured by a modifying closure)
     *   - Immutable properties (`val`) without delegation, custom getters, or `open` modifier, when accessed
     *     on a stable receiver within the same module
     *
     * #### Unstable expressions
     *
     * The following expressions are considered unstable (`isStable == false`):
     *
     *   - Mutable properties (`var`)
     *   - Properties with custom getters
     *   - Delegated properties
     *   - `open` properties (may be overridden with a custom getter)
     *   - Properties from other modules (may have different implementation at runtime)
     *   - Mutable local variables captured and modified by a closure
     *
     * #### Example
     *
     * ```kotlin
     * class Container(val value: Any)
     *
     * open class OpenContainer(open val value: Any)
     *
     * fun test(c: Container, oc: OpenContainer) {
     *     val local: Any = ""
     *
     *     // local.isStable == true (immutable local val)
     *     // c.value.isStable == true (final val in same module)
     *     // oc.value.isStable == false (open property)
     * }
     * ```
     */
    @KaExperimentalApi
    public val KtExpression.isStable: Boolean
}

/**
 * The [symbol][KaCallableSymbol] of the callable which the given [KtReturnExpression] returns from.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@Deprecated("The API is obsolete. Use `resolveSymbol()` instead.", ReplaceWith("resolveSymbol()"))
@KaIdeApi
@KaContextParameterApi
context(session: KaSession)
public val KtReturnExpression.targetSymbol: KaCallableSymbol?
    @Suppress("DEPRECATION")
    get() = with(session) { targetSymbol }

/**
 * Computes the missing cases of the given [KtWhenExpression].
 *
 * The computed missing cases are not affected by the existence or absence of an `else` branch.
 *
 * #### Example
 *
 * In the following code, `Direction.WEST` and `Direction.EAST` are missing branches:
 *
 * ```
 * enum class Direction {
 *   NORTH, SOUTH, WEST, EAST
 * }
 *
 * foo = when(direction) {
 *   Direction.NORTH -> 1
 *   Direction.SOUTH -> 2
 *   else -> 3
 * }
 * ```
 *
 * If the [KtWhenExpression] has no subject, then the `else` is reported as missing even if it is explicitly present:
 *
 * ```
 * fun test() {
 *     when {
 *         true -> {}
 *         else -> {}
 *     }
 * }
 * ```
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaIdeApi
@KaContextParameterApi
context(session: KaSession)
public fun KtWhenExpression.computeMissingCases(): List<WhenMissingCase> {
    return with(session) {
        computeMissingCases()
    }
}

/**
 * Whether the value of the given [KtExpression] is used. In other words, returns `true` if the value of the expression is not safe to
 * discard.
 *
 * [isUsedAsExpression] performs a **conservative check** instead of exhaustive control-flow analysis. For example, `x` in the following
 * example *is possibly used*, even though the value is never consumed at runtime: `x + try { throw Exception() } finally { return }`.
 *
 * #### Example
 *
 * In the following examples, `x` is used as a value (`x.isUsedAsExpression == true`):
 *
 *   - `if (x) { ... } else { ... }`
 *   - `val a = x`
 *   - `x + 8`
 *   - `when (x) { 1 -> ...; else -> ... }`
 *
 * In these expressions, `x` is not used as a value (`x.isUsedAsExpression == false`)
 *
 *   - `run { x; println(50) }`
 *   - `when (x) { else -> ... }`
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(session: KaSession)
public val KtExpression.isUsedAsExpression: Boolean
    get() = with(session) { isUsedAsExpression }

/**
 * Whether the value of the given [KtExpression] is used as the resulting expression of some lambda block.
 *
 * Note that [isUsedAsResultOfLambda] performs a **conservative check** instead of exhaustive control-flow analysis and
 * `isUsedAsResultOfLambda` being `true` doesn't imply that the containing lambda itself is used.
 *
 * It's also vital to not confuse lambda expressions with regular scope blocks (like `if` branches).
 * #### Example
 *
 * In the following examples, `x` is used as a result of a lambda (`x.isUsedAsResultOfLambda == true`):
 *
 *   - `{ x -> println(0); x }`
 *   - `{ { x }; 5 }`
 *
 * In these expressions, `x` is not used as a result of a lambda (`x.isUsedAsResultOfLambda == false`)
 *
 *   - `{ x -> println(0); x + 1 }`
 *   - `{ x; println(50) }`
 *   - `{ if (true) { x } else { x } }`
 *   - `fun(x: Int) = x`
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KtExpression.isUsedAsResultOfLambda: Boolean
    get() = with(session) { isUsedAsResultOfLambda }

/**
 * Indicates whether this expression (property reference) is [stable](https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability) for smart casting purposes.
 *
 * An expression is considered stable if its value cannot be changed via means external to the control flow graph
 * at the current program point. Stability is a prerequisite for smart casts: only stable expressions can be
 * smart-cast after a type check.
 *
 * #### Stable expressions
 *
 * The following expressions are considered stable (`isStable == true`):
 *
 *   - Immutable local variables (`val`) without delegation or custom getters
 *   - Mutable local variables (`var`) that are *effectively immutable* at the usage site (i.e., not modified
 *     between the type check and usage, and not captured by a modifying closure)
 *   - Immutable properties (`val`) without delegation, custom getters, or `open` modifier, when accessed
 *     on a stable receiver within the same module
 *
 * #### Unstable expressions
 *
 * The following expressions are considered unstable (`isStable == false`):
 *
 *   - Mutable properties (`var`)
 *   - Properties with custom getters
 *   - Delegated properties
 *   - `open` properties (may be overridden with a custom getter)
 *   - Properties from other modules (may have different implementation at runtime)
 *   - Mutable local variables captured and modified by a closure
 *
 * #### Example
 *
 * ```kotlin
 * class Container(val value: Any)
 *
 * open class OpenContainer(open val value: Any)
 *
 * fun test(c: Container, oc: OpenContainer) {
 *     val local: Any = ""
 *
 *     // local.isStable == true (immutable local val)
 *     // c.value.isStable == true (final val in same module)
 *     // oc.value.isStable == false (open property)
 * }
 * ```
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val KtExpression.isStable: Boolean
    get() = with(session) { isStable }
