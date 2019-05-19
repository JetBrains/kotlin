/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
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

        // TODO: Use symbols when builtins symbol table is fixes
        val primitiveType = context.intrinsics.primitiveArrays
            .mapKeys { it.key.descriptor }[type.classifierOrNull?.descriptor]

        val intrinsic =
            if (primitiveType != null)
                context.intrinsics.primitiveToLiteralConstructor.getValue(primitiveType)
            else
                context.intrinsics.arrayLiteral

        val startOffset = firstOrNull()?.startOffset ?: UNDEFINED_OFFSET
        val endOffset = lastOrNull()?.endOffset ?: UNDEFINED_OFFSET

        val irVararg = IrVarargImpl(startOffset, endOffset, type, varargElementType, this)

        return IrCallImpl(startOffset, endOffset, type, intrinsic).apply {
            if (intrinsic.owner.typeParameters.isNotEmpty()) putTypeArgument(0, varargElementType)
            putValueArgument(0, irVararg)
        }
    }

    inner class InlineClassArrayInfo(
        val elementType: IrType,
        val arrayType: IrType
    ) {
        val arrayInlineClass = arrayType.getInlinedClass()
        val inlined = arrayInlineClass != null

        val primitiveElementType = when {
            inlined -> getInlineClassUnderlyingType(elementType.getInlinedClass()!!)
            else -> elementType
        }

        val primitiveArrayType = when {
            inlined -> getInlineClassUnderlyingType(arrayInlineClass!!)
            else -> arrayType
        }

        fun boxArrayIfNeeded(array: IrExpression) =
            if (arrayInlineClass == null)
                array
            else with(array) {
                IrConstructorCallImpl.fromSymbolOwner(
                    startOffset,
                    endOffset,
                    arrayInlineClass.defaultType,
                    arrayInlineClass.constructors.single { it.isPrimary }.symbol
                ).also {
                    it.putValueArgument(0, array)
                }
            }

        fun unboxElementIfNeeded(element: IrExpression): IrExpression {
            if (arrayInlineClass == null)
                return element
            else with(element) {
                val inlinedClass = type.getInlinedClass() ?: return element
                val field = getInlineClassBackingField(inlinedClass)
                return IrGetFieldImpl(startOffset, endOffset, field.symbol, field.type, this)
            }
        }

        fun toPrimitiveArrayLiteral(elements: List<IrExpression>) =
            elements.toArrayLiteral(primitiveArrayType, primitiveElementType)
    }

    override fun visitVararg(expression: IrVararg): IrExpression {
        expression.transformChildrenVoid(this)

        val currentList = mutableListOf<IrExpression>()
        val segments = mutableListOf<IrExpression>()

        val arrayInfo = InlineClassArrayInfo(expression.varargElementType, expression.type)

        for (e in expression.elements) {
            when (e) {
                is IrSpreadElement -> {
                    if (!currentList.isEmpty()) {
                        segments.add(arrayInfo.toPrimitiveArrayLiteral(currentList))
                        currentList.clear()
                    }
                    segments.add(arrayInfo.unboxElementIfNeeded(e.expression))
                }

                is IrExpression -> {
                    currentList.add(arrayInfo.unboxElementIfNeeded(e))
                }
            }
        }
        if (!currentList.isEmpty()) {
            segments.add(arrayInfo.toPrimitiveArrayLiteral(currentList))
            currentList.clear()
        }

        // empty vararg => empty array literal
        if (segments.isEmpty()) {
            with (arrayInfo) {
                return boxArrayIfNeeded(toPrimitiveArrayLiteral(emptyList<IrExpression>()))
            }
        }

        // vararg with a single segment => no need to concatenate
        if (segments.size == 1) {
            val segment = segments.first()
            val argument = if (expression.elements.any { it is IrSpreadElement }) {
                val elementType = arrayInfo.primitiveElementType
                val copyFunction =
                    if (elementType.isChar() || elementType.isBoolean() || elementType.isLong())
                        context.intrinsics.taggedArrayCopy
                    else
                        context.intrinsics.jsArraySlice

                IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    arrayInfo.primitiveArrayType,
                    copyFunction
                ).apply {
                    putTypeArgument(0, arrayInfo.primitiveArrayType)
                    putValueArgument(0, segment)
                }
            } else segment

            return arrayInfo.boxArrayIfNeeded(argument)
        }

        val arrayLiteral =
            segments.toArrayLiteral(
                IrSimpleTypeImpl(context.intrinsics.array, false, emptyList(), emptyList()), // TODO: Substitution
                context.irBuiltIns.anyType
            )

        val concatFun = if (arrayInfo.primitiveArrayType.classifierOrNull in context.intrinsics.primitiveArrays.keys) {
            context.intrinsics.primitiveArrayConcat
        } else {
            context.intrinsics.arrayConcat
        }

        val res = IrCallImpl(
            expression.startOffset,
            expression.endOffset,
            arrayInfo.primitiveArrayType,
            concatFun
        ).apply {
            putValueArgument(0, arrayLiteral)
        }

        return arrayInfo.boxArrayIfNeeded(res)
    }

    private fun transformFunctionAccessExpression(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid()
        val size = expression.valueArgumentsCount

        for (i in 0 until size) {
            val argument = expression.getValueArgument(i)
            val parameter = expression.symbol.owner.valueParameters[i]
            val varargElementType = parameter.varargElementType
            if (argument == null && varargElementType != null) {
                val arrayInfo = InlineClassArrayInfo(varargElementType, parameter.type)
                val emptyArray = with (arrayInfo) {
                    boxArrayIfNeeded(toPrimitiveArrayLiteral(emptyList<IrExpression>()))
                }

                expression.putValueArgument(i, emptyArray)
            }
        }

        return expression
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) =
        transformFunctionAccessExpression(expression)
}
