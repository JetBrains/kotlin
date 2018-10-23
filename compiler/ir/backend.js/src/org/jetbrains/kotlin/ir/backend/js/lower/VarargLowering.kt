/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class VarargLowering(val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(VarargTransformer(context))
    }
}

private class VarargTransformer(
    val context: JsIrBackendContext
) : IrElementTransformerVoid() {

    private fun List<IrExpression>.toArrayLiteral(type: IrType, varargElementType: IrType): IrExpression {
        val intrinsic = context.intrinsics.primitiveArrays[type.classifierOrNull]?.let { primitiveType ->
            context.intrinsics.primitiveToLiteralConstructor[primitiveType]
        } ?: context.intrinsics.arrayLiteral

        val startOffset = firstOrNull()?.startOffset ?: UNDEFINED_OFFSET
        val endOffset = lastOrNull()?.endOffset ?: UNDEFINED_OFFSET

        val irVararg = IrVarargImpl(startOffset, endOffset, type, varargElementType, this)

        return IrCallImpl(startOffset, endOffset, type, intrinsic).apply {
            if (intrinsic.owner.typeParameters.isNotEmpty()) putTypeArgument(0, varargElementType)
            putValueArgument(0, irVararg)
        }
    }

    override fun visitVararg(expression: IrVararg): IrExpression {
        expression.transformChildrenVoid(this)

        val currentList = mutableListOf<IrExpression>()
        val segments = mutableListOf<IrExpression>()

        for (e in expression.elements) {
            if (e is IrSpreadElement) {
                if (!currentList.isEmpty()) {
                    segments.add(currentList.toArrayLiteral(expression.type, expression.varargElementType))
                    currentList.clear()
                }
                segments.add(e.expression)
            } else {
                // IrVarargElement is either IrSpreadElement or IrExpression
                currentList.add(e as IrExpression)
            }
        }
        if (!currentList.isEmpty()) {
            segments.add(currentList.toArrayLiteral(expression.type, expression.varargElementType))
            currentList.clear()
        }

        // empty vararg => empty array literal
        if (segments.isEmpty()) {
            return emptyList().toArrayLiteral(expression.type, expression.varargElementType)
        }

        // vararg with a single segment => no need to concatenate
        if (segments.size == 1) {
            return if (expression.elements.any { it is IrSpreadElement }) {
                // Single spread operator => need to copy the array
                IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    context.intrinsics.jsArraySlice.symbol
                ).apply {
                    putValueArgument(0, segments.first())
                }
            } else {
                segments.first()
            }
        }

        val arrayLiteral = segments.toArrayLiteral(IrSimpleTypeImpl(context.intrinsics.array, false, emptyList(), emptyList()), context.irBuiltIns.anyType)

        val concatFun = if (expression.type.classifierOrNull in context.intrinsics.primitiveArrays.keys) {
            context.intrinsics.primitiveArrayConcat
        } else {
            context.intrinsics.arrayConcat
        }

        return IrCallImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            concatFun
        ).apply {
            putValueArgument(0, arrayLiteral)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()
        val size = expression.valueArgumentsCount

        for (i in 0 until size) {
            val argument = expression.getValueArgument(i)
            val parameter = expression.symbol.owner.valueParameters[i]
            if (argument == null && parameter.varargElementType != null) {
                expression.putValueArgument(i, emptyList().toArrayLiteral(parameter.type, parameter.varargElementType!!))
            }
        }

        return expression
    }
}
