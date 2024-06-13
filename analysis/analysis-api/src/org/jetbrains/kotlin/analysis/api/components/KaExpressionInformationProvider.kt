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
    @KaIdeApi
    public val KtReturnExpression.targetSymbol: KaCallableSymbol?

    @KaIdeApi
    @Deprecated("Use 'targetSymbol' instead.", replaceWith = ReplaceWith("targetSymbol"))
    public fun KtReturnExpression.getReturnTargetSymbol(): KaCallableSymbol? = targetSymbol

    /**
     * Returns cases missing from the branches of [KtWhenExpression].
     *
     * The missing cases of the when-expression in the following example are Direction.WEST and Direction.EAST:
     *
     * enum class Direction {
     *   NORTH, SOUTH, WEST, EAST
     * }
     * foo = when(direction) {
     *   Direction.NORTH -> 1
     *   Direction.SOUTH -> 2
     *   else -> 3
     * }
     *
     * If when-expression has no subject, then else-branch would be reported as missing even if it is explicitly present:
     *
     * fun test() {
     *     when {
     *         true -> {}
     *         else -> {}
     *     }
     * }
     *
     * Note that this function returns the same missing cases regardless of the existence of the else branch.
     * If you have to assume that it does not have the missing cases when it has an else branch,
     * you need a separate check whether it has an else branch or not.
     */
    @KaIdeApi
    public fun KtWhenExpression.computeMissingCases(): List<WhenMissingCase>

    @KaIdeApi
    @Deprecated("Use 'computeMissingCases()' instead.", ReplaceWith("computeMissingCases()"))
    public fun KtWhenExpression.getMissingCases(): List<WhenMissingCase> = computeMissingCases()

    /**
     * Compute if the value of a given expression is possibly used. Or,
     * conversely, compute whether the value of an expression is *not* safe to
     * discard.
     *
     * E.g. `x` in the following examples *are* used (`x.isUsedAsExpression == true`)
     *   - `if (x) { ... } else { ... }`
     *   - `val a = x`
     *   - `x + 8`
     *   - `when (x) { 1 -> ...; else -> ... }
     *
     * E.g. `x` in the following example is definitely *not* used (`x.isUsedAsExpression == false`)
     *   - `run { x; println(50) }`
     *   - `when (x) { else -> ... }`
     *
     * **Note!** This is a conservative check, not a control-flow analysis.
     * E.g. `x` in the following example *is possibly used*, even though the
     * value is never consumed at runtime.
     *   - `x + try { throw Exception() } finally { return }`
     *
     */
    public val KtExpression.isUsedAsExpression: Boolean
}