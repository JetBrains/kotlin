/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class PrimitiveCompanionLowering(val context: JsIrBackendContext) : FileLoweringPass {

    private fun getActualPrimitiveCompanion(irClass: IrClass): IrClass? {
        if (!irClass.isCompanion)
            return null

        val parent = irClass.parent as IrClass

        if (!parent.defaultType.isPrimitiveType() && !parent.defaultType.isString())
            return null

        return context.primitiveCompanionObjects[parent.name]?.owner
    }

    private fun getActualPrimitiveCompanionPropertyAccessor(function: IrSimpleFunction): IrSimpleFunction? {
        val property = function.correspondingPropertySymbol?.owner
            ?: return null

        val companion = property.parent as? IrClass
            ?: return null

        val actualCompanion = getActualPrimitiveCompanion(companion)
            ?: return null

        val actualFunction =
            actualCompanion.declarations
                .filterIsInstance<IrSimpleFunction>()
                .find { it.name == function.name }

        return actualFunction!!
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val irClass = expression.symbol.owner
                val actualCompanion = getActualPrimitiveCompanion(irClass) ?: return expression
                return IrGetObjectValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    actualCompanion.defaultType,
                    actualCompanion.symbol
                )
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val newCall = super.visitCall(expression) as IrCall

                val function = expression.symbol.owner as? IrSimpleFunction
                    ?: return newCall

                val actualFunction = getActualPrimitiveCompanionPropertyAccessor(function)
                    ?: return newCall

                return irCall(newCall, actualFunction)
            }
        })
    }
}