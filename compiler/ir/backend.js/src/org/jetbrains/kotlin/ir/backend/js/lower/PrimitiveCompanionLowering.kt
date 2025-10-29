/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
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
import org.jetbrains.kotlin.ir.util.isBoxedArray
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Replaces common companion object access with platform one.
 */
class PrimitiveCompanionLowering(val context: JsIrBackendContext) : BodyLoweringPass {

    private fun getActualPrimitiveCompanion(irClass: IrClass): IrClass? {
        if (!irClass.isCompanion)
            return null

        val parent = irClass.parent as IrClass
        if (parent.defaultType.run { !isPrimitiveType() && !isString() && !isPrimitiveArray() && !isBoxedArray })
            return null

        return context.symbols.primitiveCompanionObjects[parent.name]?.owner
    }

    private fun IrSimpleFunction.isMatchingFunctionInActualPrimitiveCompanionFor(function: IrSimpleFunction): Boolean {
        return name == function.name
    }

    private fun IrClass.getMatchingFunctionInActualPrimitiveCompanionFor(function: IrSimpleFunction): IrSimpleFunction {
        return declarations.filterIsInstance<IrSimpleFunction>().single {
            it.isMatchingFunctionInActualPrimitiveCompanionFor(function)
        }
    }

    private fun getActualCompanionFunction(function: IrSimpleFunction): IrSimpleFunction? {
        function.correspondingPropertySymbol?.owner?.let { property ->
            return getActualPrimitiveCompanionPropertyAccessor(function, property)
        }

        // companion overrides `Any` so it still has `toString` etc. which is irrelevant here
        if (function.isFakeOverride) return null
        val companion = function.parent as? IrClass ?: return null
        val actualCompanion = getActualPrimitiveCompanion(companion) ?: return null

        return actualCompanion.getMatchingFunctionInActualPrimitiveCompanionFor(function)
    }

    private fun getActualPrimitiveCompanionPropertyAccessor(function: IrSimpleFunction, property: IrProperty): IrSimpleFunction? {
        val companion = property.parent as? IrClass
            ?: return null

        val actualCompanion = getActualPrimitiveCompanion(companion)
            ?: return null

        for (p in actualCompanion.properties) {
            p.getter?.let { if (it.name == function.name) return it }
            p.setter?.let { if (it.name == function.name) return it }
        }

        return actualCompanion.getMatchingFunctionInActualPrimitiveCompanionFor(function)
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
                val actualFunction = getActualCompanionFunction(expression.symbol.owner) ?: return newCall

                return irCall(newCall, actualFunction)
            }
        })
    }
}
