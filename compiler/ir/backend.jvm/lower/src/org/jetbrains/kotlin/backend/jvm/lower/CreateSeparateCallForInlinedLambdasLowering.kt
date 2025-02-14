/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.getTmpVariablesForArguments
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Creates a separate call to `singleArgumentInlineFunction` with previously inlined lambda as argument.
 */
@PhaseDescription(
    name = "CreateSeparateCallForInlinedLambdasLowering",
    prerequisite = [JvmIrInliner::class]
)
class CreateSeparateCallForInlinedLambdasLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.config.enableIrInliner) {
            irFile.transformChildrenVoid()
        }
    }

    override fun visitReturnableBlock(expression: IrReturnableBlock): IrExpression {
        val inlinedBlock = expression.statements.lastOrNull() as? IrInlinedFunctionBlock ?: return super.visitReturnableBlock(expression)
        if (inlinedBlock.isFunctionInlining()) {
            val newStatements = inlinedBlock.getOnlyInlinableArguments().map { arg ->
                IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.symbols.singleArgumentInlineFunction)
                    .also { it.putValueArgument(0, arg.transform(this, null)) }
            }

            // we don't need to transform body of original function, just arguments that were extracted as variables
            inlinedBlock.getTmpVariablesForArguments().forEach { it.transformChildrenVoid() }

            expression.statements.addAll(0, newStatements)
            return expression
        }
        return super.visitReturnableBlock(expression)
    }

    private fun IrInlinedFunctionBlock.getOnlyInlinableArguments(): List<IrExpression> {
        return this.inlineCall!!.getArgumentsWithIr()
            .filter { (param, arg) -> param.isInlineParameter() && arg.isInlinableExpression() }
            .map { it.second }
    }

    private fun IrExpression.isInlinableExpression(): Boolean {
        return this is IrFunctionExpression || this is IrFunctionReference || this is IrPropertyReference
                || this.isAdaptedFunctionReference() || this.isLambdaBlock()
    }
}
