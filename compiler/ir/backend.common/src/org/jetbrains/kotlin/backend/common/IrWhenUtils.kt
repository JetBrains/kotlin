/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isElseBranch
import org.jetbrains.kotlin.ir.util.isTrueConst

object IrWhenUtils {
// psi2ir lowers multiple cases to nested conditions. For example,
//
// when (subject) {
//   a, b, c -> action
// }
//
// is lowered to
//
// if (if (subject == a)
//       true
//     else
//       if (subject == b)
//         true
//       else
//         subject == c) {
//     action
// }
//
// fir2ir lowers the same to an or sequence:
//
// if (((subject == a) || (subject == b)) || (subject = c)) action
//
// The type T shall be specified to narrow the set of the allowed leaf conditions; for example, use 'IrCall' if
//   leaf conditions are expected to be the equality checks of constants.
// Also, an additional (optional) predicate 'leafConditionPredicate' can be specified to further narrow down the allowed leaf conditions.
//
// @return a list of the leaf conditions, given that they all are of the same type T, or 'null' if the conditions are not matched
    inline fun <reified T: IrExpression> matchConditions(ororSymbol: IrFunctionSymbol, condition: IrExpression): List<T>? {
        return matchConditions(ororSymbol, condition) { it is T }?.map { it as T }
    }

    fun matchConditions(ororSymbol: IrFunctionSymbol, condition: IrExpression, leafConditionPredicate: (IrExpression) -> Boolean): List<IrExpression>? {
        if (condition is IrWhen && condition.origin == IrStatementOrigin.WHEN_COMMA) {
            assert(condition.type.isBoolean()) { "WHEN_COMMA should always be a Boolean: ${condition.dump()}" }

            val candidates = ArrayList<IrExpression>()

            // Match the following structure:
            //
            // when() {
            //   cond_1 -> true
            //   cond_2 -> true
            //   ...
            //   else -> cond_N
            // }
            //
            // Namely, the structure which returns true if any one of the condition is true.
            for (branch in condition.branches) {
                candidates += if (isElseBranch(branch)) {
                    assert(branch.condition.isTrueConst()) { "IrElseBranch.condition should be const true: ${branch.condition.dump()}" }
                    matchConditions(ororSymbol, branch.result, leafConditionPredicate) ?: return null
                } else {
                    if (!branch.result.isTrueConst()) return null
                    matchConditions(ororSymbol, branch.condition, leafConditionPredicate) ?: return null
                }
            }
            return candidates.ifEmpty { null }
        } else if (condition is IrCall && condition.symbol == ororSymbol) {
            val candidates = ArrayList<IrExpression>()
            for (argument in condition.arguments) {
                candidates += matchConditions(ororSymbol, argument!!, leafConditionPredicate) ?: return null
            }
            return candidates.ifEmpty { null }
        } else if (leafConditionPredicate.invoke(condition)) {
            return arrayListOf(condition)
        }

        return null
    }
}