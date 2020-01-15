/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isUnsigned
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


class ConstTransformer(private val context: JsIrBackendContext) : IrElementTransformerVoid() {
    private fun <C> lowerConst(
        irClass: IrClassSymbol,
        carrierFactory: (Int, Int, IrType, C) -> IrExpression,
        vararg args: C
    ): IrExpression {
        val constructor = irClass.constructors.single()
        val argType = constructor.owner.valueParameters.first().type
        return IrConstructorCallImpl.fromSymbolOwner(irClass.defaultType, constructor).apply {
            for (i in args.indices) {
                putValueArgument(i, carrierFactory(UNDEFINED_OFFSET, UNDEFINED_OFFSET, argType, args[i]))
            }
        }
    }

    private fun createLong(v: Long): IrExpression =
        lowerConst(context.intrinsics.longClassSymbol, IrConstImpl.Companion::int, v.toInt(), (v shr 32).toInt())

    override fun <T> visitConst(expression: IrConst<T>): IrExpression {
        with(context.intrinsics) {
            if (expression.type.isUnsigned()) {
                return when (expression.type.classifierOrNull) {
                    uByteClassSymbol -> lowerConst(uByteClassSymbol, IrConstImpl.Companion::byte, IrConstKind.Byte.valueOf(expression))

                    uShortClassSymbol -> lowerConst(uShortClassSymbol, IrConstImpl.Companion::short, IrConstKind.Short.valueOf(expression))

                    uIntClassSymbol -> lowerConst(uIntClassSymbol, IrConstImpl.Companion::int, IrConstKind.Int.valueOf(expression))

                    uLongClassSymbol -> lowerConst(uLongClassSymbol, { _, _, _, v -> createLong(v) }, IrConstKind.Long.valueOf(expression))

                    else -> error("Unknown unsigned type")
                }
            }
            return when {
                expression.kind is IrConstKind.Char ->
                    lowerConst(charClassSymbol, IrConstImpl.Companion::int, IrConstKind.Char.valueOf(expression).toInt())

                expression.kind is IrConstKind.Long ->
                    createLong(IrConstKind.Long.valueOf(expression))

                else -> super.visitConst(expression)
            }
        }
    }
}

class ConstLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(ConstTransformer(context))
    }
}
