/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.util.isFalseConst
import org.jetbrains.kotlin.ir.util.isTrueConst
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Remove unreachable branches in `when` expressions
 *
 * 1) It remove constant false branches
 * when {
 *   false -> something1
 *   else -> something2
 * }
 * to
 * when {
 *   else -> something2
 * }
 * ---------------------------------------
 * 2) It remove non-constant false branches
 * when {
 *   { something1; false } -> something2
 *   else -> something3
 * }
 * to
 * when {
 *   { something1; false } -> unreachable
 *   else -> something3
 * }
 * ---------------------------------------
 * 3) It remove unreachable branches
 * when {
 *   { something1; unreachable } -> something2
 *   else -> something3
 * }
 * to
 * when {
 *   else -> { something1; unreachable }
 * }
 * ---------------------------------------
 * 4) It remove all branches after constant true branches
 * when {
 *   true -> something1
 *   else -> something2
 * }
 * to
 * when {
 *   else -> something1
 * }
 * ---------------------------------------
 * 5) It remove all branches after non-constant true branches
 * when {
 *   { something1; true } -> something2
 *   else -> something3
 * }
 * to
 * when {
 *   else -> { something1; something2 }
 * }
 */

class WhenBranchOptimiserLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(WhenBranchOptimiserTransformer(context))
    }
}

private class WhenBranchOptimiserTransformer(
    val context: WasmBackendContext,
) : IrElementTransformerVoidWithContext() {
    private fun createElse(startOffset: Int, endOffset: Int, result: IrExpression): IrElseBranch = IrElseBranchImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        condition = true.toIrConst(context.irBuiltIns.booleanType),
        result = result
    )

    private enum class ConditionValue {
        CONST_TRUE,
        COMPLEX_TRUE,
        CONST_FALSE,
        COMPLEX_FALSE,
        UNREACHABLE,
        OTHER
    }

    private fun branchConditionValue(branch: IrBranch): ConditionValue {
        if (branch is IrElseBranch) return ConditionValue.CONST_TRUE

        var currentExpression: IrExpression? = branch.condition
        var falseCondition = false
        var trueCondition = false
        var complexCondition = false
        while (currentExpression != null) {
            if (currentExpression.isFalseConst()) {
                falseCondition = true
                break
            }
            if (currentExpression.isTrueConst()) {
                trueCondition = true
                break
            }
            if (currentExpression.type == context.irBuiltIns.nothingType) return ConditionValue.UNREACHABLE

            val expressionAsComposite = currentExpression as? IrComposite ?: break
            if (expressionAsComposite.statements.isEmpty()) break
            if (expressionAsComposite.statements.size > 1) {
                complexCondition = true
            }
            currentExpression = expressionAsComposite.statements.last() as? IrExpression
        }

        return if (trueCondition) {
            if (complexCondition) ConditionValue.COMPLEX_TRUE else ConditionValue.CONST_TRUE
        } else if (falseCondition) {
            if (complexCondition) ConditionValue.COMPLEX_FALSE else ConditionValue.CONST_FALSE
        } else {
            ConditionValue.OTHER
        }
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        expression.transformChildrenVoid()

        val iterate = expression.branches.toTypedArray()
        expression.branches.clear()

        for (branch in iterate) {
            when (branchConditionValue(branch)) {
                ConditionValue.CONST_TRUE -> {
                    val elseBranch = createElse(branch.startOffset, branch.endOffset, branch.result)
                    expression.branches.add(elseBranch)
                    return expression
                }
                ConditionValue.COMPLEX_TRUE -> {
                    val result = IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, branch.result.type).also {
                        it.statements.add(branch.condition)
                        it.statements.add(branch.result)
                    }
                    val elseBranch = createElse(branch.startOffset, branch.endOffset, result)
                    expression.branches.add(elseBranch)
                    return expression
                }
                ConditionValue.UNREACHABLE -> {
                    val elseBranch = createElse(branch.startOffset, branch.endOffset, branch.condition)
                    expression.branches.add(elseBranch)
                    return expression
                }
                ConditionValue.CONST_FALSE -> continue
                ConditionValue.COMPLEX_FALSE -> {
                    val unreachable = IrCallImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = context.irBuiltIns.nothingType,
                        symbol = context.wasmSymbols.wasmUnreachable,
                        typeArgumentsCount = 0,
                        valueArgumentsCount = 0
                    )
                    branch.result = unreachable
                    expression.branches.add(branch)
                }
                ConditionValue.OTHER -> {
                    expression.branches.add(branch)
                }
            }
        }

        return expression
    }
}