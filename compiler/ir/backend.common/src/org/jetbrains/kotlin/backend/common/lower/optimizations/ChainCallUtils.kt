/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen

class SafeCallInfo(
    val block: IrBlock,
    val tmpVal: IrVariable,
    val ifNullBranch: IrBranch,
    val ifNotNullBranch: IrBranch
)

fun IrBlock.parseSafeCall(irBuiltIns: IrBuiltIns): SafeCallInfo? {
    //  {
    //      val tmp = <safe_receiver>
    //      when {
    //          tmp == null -> null
    //          else -> <safe_call_result>
    //      }
    //  }

    if (this.statements.size != 2) return null
    val tmpVal = this.statements[0] as? IrVariable ?: return null
    val whenExpr = this.statements[1] as? IrWhen ?: return null
    if (whenExpr.branches.size != 2) return null

    val ifNullBranch = whenExpr.branches[0]
    val ifNullBranchCondition = ifNullBranch.condition
    if (ifNullBranchCondition !is IrCall) return null
    if (ifNullBranchCondition.symbol != irBuiltIns.eqeqSymbol) return null
    val arg0 = ifNullBranchCondition.arguments[0]
    if (arg0 !is IrGetValue || arg0.symbol != tmpVal.symbol) return null
    val arg1 = ifNullBranchCondition.arguments[1]
    if (arg1 !is IrConst || arg1.value != null) return null
    val ifNullBranchResult = ifNullBranch.result
    if (ifNullBranchResult !is IrConst || ifNullBranchResult.value != null) return null

    val ifNotNullBranch = whenExpr.branches[1]
    return SafeCallInfo(this, tmpVal, ifNullBranch, ifNotNullBranch)
}


class ElvisInfo(
    val block: IrBlock,
    val tmpVal: IrVariable,
    val elvisLhs: IrExpression,
    val elvisRhs: IrExpression
)

fun IrBlock.parseElvis(irBuiltIns: IrBuiltIns): ElvisInfo? {
    //  {
    //      val tmp = <elvis_lhs>
    //      when {
    //          tmp == null -> <elvis_rhs>
    //          else -> tmp
    //      }
    //  }

    if (this.statements.size != 2) return null
    val tmpVal = this.statements[0] as? IrVariable ?: return null
    val whenExpr = this.statements[1] as? IrWhen ?: return null
    if (whenExpr.branches.size != 2) return null

    val elvisLhs = tmpVal.initializer ?: return null
    val ifNullBranch = whenExpr.branches[0]
    val ifNullBranchCondition = ifNullBranch.condition
    if (ifNullBranchCondition !is IrCall) return null
    if (ifNullBranchCondition.symbol != irBuiltIns.eqeqSymbol) return null
    val arg0 = ifNullBranchCondition.arguments[0]
    if (arg0 !is IrGetValue || arg0.symbol != tmpVal.symbol) return null
    val arg1 = ifNullBranchCondition.arguments[1]
    if (arg1 !is IrConst || arg1.value != null) return null
    val elvisRhs = ifNullBranch.result

    val ifNonNullBranch = whenExpr.branches[1]
    val ifNonNullBranchResult = ifNonNullBranch.result
    if (ifNonNullBranchResult !is IrGetValue || ifNonNullBranchResult.symbol != tmpVal.symbol) return null

    return ElvisInfo(this, tmpVal, elvisLhs, elvisRhs)
}
