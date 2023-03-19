/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isFunctionInlining
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal val tailCallOptimizationPhase = makeIrFilePhase(
    ::TailCallOptimizationLowering,
    "TailCallOptimization",
    "Add or move returns to suspension points on tail-call positions"
)

// Find all tail-calls inside suspend function. We should add IrReturn before them, so the codegen will generate
// code which is understandable by old BE's tail-call optimizer.
private class TailCallOptimizationLowering(private val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : IrElementTransformer<TailCallOptimizationData?> {
            override fun visitSimpleFunction(declaration: IrSimpleFunction, data: TailCallOptimizationData?) =
                super.visitSimpleFunction(declaration, if (declaration.isSuspend) TailCallOptimizationData(declaration) else null)

            override fun visitContainerExpression(expression: IrContainerExpression, data: TailCallOptimizationData?): IrExpression {
                if (expression is IrInlinedFunctionBlock && expression.isFunctionInlining()) {
                    return expression
                }
                return super.visitContainerExpression(expression, data)
            }

            override fun visitCall(expression: IrCall, data: TailCallOptimizationData?): IrExpression {
                val transformed = super.visitCall(expression, data) as IrExpression
                return if (data == null || expression !in data.tailCalls) transformed else IrReturnImpl(
                    data.function.endOffset, data.function.endOffset, context.irBuiltIns.nothingType, data.function.symbol,
                    if (data.returnsUnit) transformed.coerceToUnit() else transformed
                )
            }
        }, null)
    }

    private fun IrExpression.coerceToUnit() = if (type == context.irBuiltIns.unitType) this else IrTypeOperatorCallImpl(
        startOffset, endOffset, context.irBuiltIns.unitType, IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, context.irBuiltIns.unitType, this
    )
}

private class TailCallOptimizationData(val function: IrSimpleFunction) {
    val returnsUnit = function.returnType.isUnit()
    val tailCalls = mutableSetOf<IrCall>()

    // Collect all tail calls, including those nested in `when`s, which are not arguments to `return`s.
    private fun IrStatement.findCallsOnTailPositionWithoutImmediateReturn(immediateReturn: Boolean = false) {
        when {
            this is IrCall && isSuspend && !immediateReturn && (returnsUnit || type == function.returnType) ->
                tailCalls += this
            // We want to avoid tail call optimization in inlined block because it ruins line number generation
            this is IrBlock && !(this is IrInlinedFunctionBlock && this.isFunctionInlining()) ->
                statements.findTailCall(returnsUnit)?.findCallsOnTailPositionWithoutImmediateReturn()
            this is IrWhen ->
                branches.forEach { it.result.findCallsOnTailPositionWithoutImmediateReturn() }
            this is IrReturn ->
                value.findCallsOnTailPositionWithoutImmediateReturn(immediateReturn = true)
            this is IrTypeOperatorCall && operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
                argument.findCallsOnTailPositionWithoutImmediateReturn()
            // TODO: Support binary logical operations and elvis, though. KT-23826 and KT-23825
        }
    }

    init {
        when (val body = function.body) {
            is IrBlockBody -> body.statements.findTailCall(returnsUnit)?.findCallsOnTailPositionWithoutImmediateReturn()
            is IrExpressionBody -> body.expression.findCallsOnTailPositionWithoutImmediateReturn(immediateReturn = true)
        }
    }
}

// Find tail-call inside a single block. This function is needed, since there can be
// return statement in the middle of the function and thus we cannot just assume, that its last statement is tail-call
private fun List<IrStatement>.findTailCall(functionReturnsUnit: Boolean): IrStatement? {
    val mayBeReturn = find { it is IrReturn } as? IrReturn
    return when (val value = mayBeReturn?.value) {
        is IrGetField -> if (functionReturnsUnit && value.isGetFieldOfUnit()) {
            // This is simple `return` in the middle of a function
            // Tail-call should be just before it
            subList(0, indexOf(mayBeReturn)).findTailCall(functionReturnsUnit)
        } else mayBeReturn
        null -> lastOrNull()
        else -> mayBeReturn
    }
}

private fun IrGetField.isGetFieldOfUnit(): Boolean =
    type.isUnit() && symbol.owner.name == Name.identifier("INSTANCE") && symbol.owner.parentAsClass.fqNameWhenAvailable == FqName("kotlin.Unit")
