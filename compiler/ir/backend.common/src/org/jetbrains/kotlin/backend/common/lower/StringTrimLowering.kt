/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName

val computeStringTrimPhase = makeIrFilePhase(
    ::StringTrimLowering,
    name = "StringTrimLowering",
    description = "Compute trimIndent and trimMargin operations on constant strings"
)

class StringTrimLowering(val context: CommonBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return when {
            trimIndentMatcher(expression) -> maybeComputeTrimIndent(expression)
            trimMarginMatcher(expression) -> maybeComputeTrimMargin(expression)
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
            receiverString.trimMargin(prefixString)
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

        private val trimIndentMatcher = SimpleCalleeMatcher {
            extensionReceiver { it != null && it.type.isString() }
            fqName { it == TRIM_INDENT_FQ_NAME }
            parameterCount { it == 0 }
        }

        private val trimMarginMatcher = SimpleCalleeMatcher {
            extensionReceiver { it != null && it.type.isString() }
            fqName { it == TRIM_MARGIN_FQ_NAME }
            parameterCount { it == 1 }
            parameter(0) { it.type.isString() }
        }

        private val TRIM_MARGIN_FQ_NAME = FqName.fromSegments(listOf("kotlin", "text", "trimMargin"))
        private val TRIM_INDENT_FQ_NAME = FqName.fromSegments(listOf("kotlin", "text", "trimIndent"))
    }
}
