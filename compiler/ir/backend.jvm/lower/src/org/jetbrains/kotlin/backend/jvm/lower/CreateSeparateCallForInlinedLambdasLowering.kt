/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.getAdditionalStatementsFromInlinedBlock
import org.jetbrains.kotlin.backend.common.ir.isFunctionInlining
import org.jetbrains.kotlin.backend.common.ir.putStatementBeforeActualInline
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.functionInliningPhase
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.irInlinerIsEnabled
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal val createSeparateCallForInlinedLambdas = makeIrModulePhase(
    { context ->
        if (!context.irInlinerIsEnabled()) return@makeIrModulePhase FileLoweringPass.Empty
        CreateSeparateCallForInlinedLambdasLowering(context)
    },
    name = "CreateSeparateCallForInlinedLambdasLowering",
    description = "This lowering will create separate call `singleArgumentInlineFunction` with previously inlined lambda as argument",
    prerequisite = setOf(functionInliningPhase)
)

class CreateSeparateCallForInlinedLambdasLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        if (expression is IrInlinedFunctionBlock && expression.isFunctionInlining()) {
            val newCalls = expression.getOnlyInlinableArguments().map { arg ->
                IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.ir.symbols.singleArgumentInlineFunction)
                    .also { it.putValueArgument(0, arg.transform(this, null)) }
            }

            // we don't need to transform body of original function, just arguments that were extracted as variables
            expression.getAdditionalStatementsFromInlinedBlock().forEach { it.transformChildrenVoid() }
            newCalls.reversed().forEach {
                expression.putStatementBeforeActualInline(context.createJvmIrBuilder(it.symbol), it)
            }
            return expression
        }

        return super.visitContainerExpression(expression)
    }

    private fun IrInlinedFunctionBlock.getOnlyInlinableArguments(): List<IrExpression> {
        return this.inlineCall.getArgumentsWithIr()
            .filter { (param, arg) -> param.isInlineParameter() && arg.isInlinableExpression() }
            .map { it.second }
    }

    private fun IrExpression.isInlinableExpression(): Boolean {
        return this is IrFunctionExpression || this is IrFunctionReference || this is IrPropertyReference
                || (this is IrBlock && origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE)
    }
}
