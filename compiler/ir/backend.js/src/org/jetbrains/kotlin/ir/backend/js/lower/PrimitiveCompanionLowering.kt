/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class PrimitiveCompanionLowering(val context: JsIrBackendContext) : BodyLoweringPass {

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

        for (p in actualCompanion.properties) {
            p.getter?.let { if (it.name == function.name) return it }
            p.setter?.let { if (it.name == function.name) return it }
        }

        return actualCompanion.declarations
            .filterIsInstance<IrSimpleFunction>()
            .single { it.name == function.name }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
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

                val actualFunction = getActualPrimitiveCompanionPropertyAccessor(expression.symbol.owner)
                    ?: return newCall

                return irCall(newCall, actualFunction)
            }
        })
    }
}
