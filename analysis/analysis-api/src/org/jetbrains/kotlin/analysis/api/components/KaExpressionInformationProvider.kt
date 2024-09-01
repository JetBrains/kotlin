/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

public interface KaExpressionInformationProvider {
    /**
     * Returns the symbol of the callable that the given [KtReturnExpression] returns from.
     */
    @KaIdeApi
    public val KtReturnExpression.targetSymbol: KaCallableSymbol?

    @KaIdeApi
    @Deprecated("Use 'targetSymbol' instead.", replaceWith = ReplaceWith("targetSymbol"))
    public fun KtReturnExpression.getReturnTargetSymbol(): KaCallableSymbol? = targetSymbol

    /**
     * Computes missing case branches of the given [KtWhenExpression].
     *
     * In the following example, `Direction.WEST` and `Direction.EAST` are missing branches:
     *
     * ```
     * enum class Direction {
     *   NORTH, SOUTH, WEST, EAST
     * }
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
     *
     * Note: the function returns the same missing case list, regardless of the existence of the `else` branch.
     */
    @KaIdeApi
    public fun KtWhenExpression.computeMissingCases(): List<WhenMissingCase>

    @KaIdeApi
    @Deprecated("Use 'computeMissingCases()' instead.", ReplaceWith("computeMissingCases()"))
    public fun KtWhenExpression.getMissingCases(): List<WhenMissingCase> = computeMissingCases()

    /**
     * `true` is the value of the given expression is used.
     * In other words, returns `true` if the value of the expression is not safe to discard.
     *
     * In the following examples, `x` is used as a value (`x.isUsedAsExpression == true`):
     *   - `if (x) { ... } else { ... }`
     *   - `val a = x`
     *   - `x + 8`
     *   - `when (x) { 1 -> ...; else -> ... }
     *
     * In these expressions, `x` is not used as a value (`x.isUsedAsExpression == false`)
     *   - `run { x; println(50) }`
     *   - `when (x) { else -> ... }`
     *
     * **Note!** This is a conservative check, and not a control-flow analysis.
     * E.g. `x` in the following example *is possibly used*, even though the
     * value is never consumed at runtime.
     *   - `x + try { throw Exception() } finally { return }`
     */
    public val KtExpression.isUsedAsExpression: Boolean
}