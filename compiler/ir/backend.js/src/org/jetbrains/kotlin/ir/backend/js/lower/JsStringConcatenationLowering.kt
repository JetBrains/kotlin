/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class JsStringConcatenationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(JsStringConcatenationTransformer(context))
    }
}

private class JsStringConcatenationTransformer(val context: CommonBackendContext) : IrElementTransformerVoid() {

    private val IrType.shouldExplicitlyConvertToString: Boolean
        get() {
            // If the type is Long or a supertype of Long, we want to call toString() on values of that type.
            // See KT-39891
            if (this !is IrSimpleType) return false
            return when (classifier.signature) {
                IdSignatureValues.any, IdSignatureValues.comparable, IdSignatureValues.number, IdSignatureValues._long -> true
                else -> false
            }
        }

    private fun IrExpression.explicitlyConvertedToString(): IrExpression {
        assert(type.shouldExplicitlyConvertToString)

        return if (type.isNullable()) {
            JsIrBuilder.buildCall(context.ir.symbols.extensionToString).apply {
                extensionReceiver = this@explicitlyConvertedToString
            }
        } else {
            JsIrBuilder.buildCall(context.ir.symbols.memberToString).apply {
                dispatchReceiver = this@explicitlyConvertedToString
            }
        }
    }

    private val IrFunctionSymbol.isStringPlus: Boolean
        get() = context.ir.symbols.isStringPlus(this)

    override fun visitCall(expression: IrCall): IrExpression {
        fun explicitlyConvertToStringIfNeeded(): IrExpression {
            val lastArgIndex = expression.valueArgumentsCount - 1
            val plusArg = expression.getValueArgument(lastArgIndex) ?: return super.visitCall(expression)
            if (!plusArg.type.shouldExplicitlyConvertToString)
                return super.visitCall(expression)

            expression.putValueArgument(lastArgIndex, plusArg.explicitlyConvertedToString())
            return expression
        }

        if (expression.valueArgumentsCount == 0)
            return super.visitCall(expression)

        if (expression.symbol.isStringPlus)
            return explicitlyConvertToStringIfNeeded()

        if (expression.dispatchReceiver.safeAs<IrFunctionReference>()?.symbol?.isStringPlus == true)
            return explicitlyConvertToStringIfNeeded()

        return super.visitCall(expression)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression
            .transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitExpression(expression: IrExpression): IrExpression {
                    if (expression.type.shouldExplicitlyConvertToString)
                        return expression.explicitlyConvertedToString()
                    return expression
                }
            })
        return super.visitStringConcatenation(expression)
    }
}
