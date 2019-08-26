/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class GenericReturnTypeLowering(val context: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression = transformGenericCall(super.visitCall(expression) as IrCall)
        })
    }

    private fun IrType.eraseUpperBoundType(): IrType {
        val superTypeParameter = this.classifierOrNull?.owner as? IrTypeParameter
        if (superTypeParameter != null) {
            val ubType = superTypeParameter.eraseUpperBoundType()
            return if (this.isMarkedNullable())
                ubType.makeNullable()
            else
                ubType
        }

        return this
    }

    private fun IrTypeParameter.eraseUpperBoundType(): IrType {
        return superTypes.firstOrNull()?.eraseUpperBoundType() ?: context.irBuiltIns.anyNType
    }

    private fun transformGenericCall(call: IrCall): IrExpression {
        val function: IrSimpleFunction =
            call.symbol.owner as? IrSimpleFunction ?: return call

        if (!function.returnType.isTypeParameter()) return call

        val realReturnType: IrType =
            function.returnType.eraseUpperBoundType()

        if (realReturnType != call.type) {
            if (call.type.isNothing() || call.type.isUnit()) return call
            if (realReturnType.isSubtypeOf(call.type, context.irBuiltIns)) return call
            return IrTypeOperatorCallImpl(
                startOffset = call.startOffset, endOffset = call.endOffset,
                type = call.type,
                operator = IrTypeOperator.IMPLICIT_CAST,
                typeOperand = call.type,
                argument = irCall(call, function.symbol, newReturnType = realReturnType, newSuperQualifierSymbol = call.superQualifierSymbol)
            )
        }
        return call
    }
}