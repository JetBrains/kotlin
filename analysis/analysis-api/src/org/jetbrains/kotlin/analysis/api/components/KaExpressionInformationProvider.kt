/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaExpressionInformationProvider : KaSessionComponent {
    /**
     * The [symbol][KaCallableSymbol] of the callable which the given [KtReturnExpression] returns from.
     */
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
}

/**
 * @see KaExpressionInformationProvider.targetSymbol
 */
@KaContextParameterApi
@KaIdeApi
context(context: KaExpressionInformationProvider)
public val KtReturnExpression.targetSymbol: KaCallableSymbol?
    get() = with(context) { targetSymbol }

/**
 * @see KaExpressionInformationProvider.computeMissingCases
 */
@KaContextParameterApi
@KaIdeApi
context(context: KaExpressionInformationProvider)
public fun KtWhenExpression.computeMissingCases(): List<WhenMissingCase> {
    return with(context) { computeMissingCases() }
}

/**
 * @see KaExpressionInformationProvider.isUsedAsExpression
 */
@KaContextParameterApi
context(context: KaExpressionInformationProvider)
public val KtExpression.isUsedAsExpression: Boolean
    get() = with(context) { isUsedAsExpression }

/**
 * @see KaExpressionInformationProvider.isUsedAsResultOfLambda
 */
@KaContextParameterApi
@KaExperimentalApi
context(context: KaExpressionInformationProvider)
public val KtExpression.isUsedAsResultOfLambda: Boolean
    get() = with(context) { isUsedAsResultOfLambda }
