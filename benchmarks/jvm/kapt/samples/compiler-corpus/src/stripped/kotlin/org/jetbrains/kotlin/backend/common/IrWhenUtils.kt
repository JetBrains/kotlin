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
//
// @return a list of the leaf conditions, given that they all are of the same type T, or 'null' if the conditions are not matched
    inline fun <reified T : IrExpression> matchConditions(ororSymbol: IrFunctionSymbol, condition: IrExpression): List<T>? {
        return matchConditions(ororSymbol, condition) { it is T }?.map { it as T }
    }

    fun matchConditions(
        ororSymbol: IrFunctionSymbol, condition: IrExpression, leafConditionPredicate: (IrExpression) -> Boolean,
    ): List<IrExpression>? { TODO() }
}
