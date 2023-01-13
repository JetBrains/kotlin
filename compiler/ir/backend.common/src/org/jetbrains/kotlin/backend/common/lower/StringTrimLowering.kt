/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName

class StringTrimLowering(val context: CommonBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return when {
            matchTrimIndent(expression) -> maybeComputeTrimIndent(expression)
            matchTrimMargin(expression) -> maybeComputeTrimMargin(expression)
            else -> super.visitCall(expression)
        }
    }

    private fun maybeComputeTrimIndent(call: IrCall): IrExpression {
        val receiverString = call.extensionReceiver!!.getConstantString() ?: return call
        val newString = receiverString.trimIndent()
        return IrConstImpl.string(call.startOffset, call.endOffset, call.type, newString)
    }

    private fun maybeComputeTrimMargin(call: IrCall): IrExpression {
        val receiverString = call.extensionReceiver!!.getConstantString() ?: return call

        val prefixArgument = call.getValueArgument(0)
        val newString = if (prefixArgument != null) {
            val prefixString = prefixArgument.getConstantString() ?: return call
            try {
                receiverString.trimMargin(prefixString)
            } catch (e: IllegalArgumentException) {
                return call
            }
        } else {
            receiverString.trimMargin()
        }

        return IrConstImpl.string(call.startOffset, call.endOffset, call.type, newString)
    }

    companion object {
        private fun IrExpression.getConstantString(): String? {
            if (this is IrConst<*> && kind == IrConstKind.String) {
                return IrConstKind.String.valueOf(this)
            }
            return null
        }

        private fun matchTrimIndent(expression: IrCall): Boolean {
            val callee = expression.symbol.owner
            return callee.valueParameters.isEmpty() &&
                    callee.extensionReceiverParameter?.type?.isString() == true &&
                    callee.kotlinFqName == TRIM_INDENT_FQ_NAME
        }

        private fun matchTrimMargin(expression: IrCall): Boolean {
            val callee = expression.symbol.owner
            return callee.valueParameters.singleOrNull()?.type?.isString() == true &&
                    callee.extensionReceiverParameter?.type?.isString() == true &&
                    callee.kotlinFqName == TRIM_MARGIN_FQ_NAME
        }

        private val TRIM_MARGIN_FQ_NAME = FqName("kotlin.text.trimMargin")
        private val TRIM_INDENT_FQ_NAME = FqName("kotlin.text.trimIndent")
    }
}
