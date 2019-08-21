/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * Replace string concatenation with a chain of String.plus calls.
 * TODO: Reuse common StringConcatenationLowering which uses string builder
 */
class SimpleStringConcatenationLowering(val context: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(StringConcatenationTransformer(this))
    }
}

private class StringConcatenationTransformer(val lower: SimpleStringConcatenationLowering) : IrElementTransformerVoid() {
    private val context = lower.context
    private val irBuiltIns = context.irBuiltIns
    private val stringPlus = irBuiltIns.stringClass.owner.declarations.filterIsInstance<IrSimpleFunction>().find {
        it.name == Name.identifier("plus")
    }!!

    private val anyToString = irBuiltIns.anyClass.owner.declarations.filterIsInstance<IrSimpleFunction>().find {
        it.name == Name.identifier("toString")
    }!!


    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        return expression.arguments.fold<IrExpression, IrExpression>(
            JsIrBuilder.buildString(irBuiltIns.stringType, "")
        ) { acc, el ->
            JsIrBuilder.buildCall(stringPlus.symbol).apply {
                dispatchReceiver = acc
                putValueArgument(0, expressionToString(el))
            }
        }
    }

    private fun expressionToString(expression: IrExpression): IrExpression {
        if (expression.type.isString()) return expression
        val klass = expression.type.getClass()
        val toStringMethod: IrSimpleFunction = if (klass != null) {
            klass.declarations.filterIsInstance<IrSimpleFunction>().find { it.isToStringInheritedFromAny() }!!
        } else {
            anyToString
        }

        return JsIrBuilder.buildCall(toStringMethod.symbol).apply {
            dispatchReceiver = expression
        }
    }
}

fun IrFunction.isToStringInheritedFromAny() =
    name == Name.identifier("toString") &&
            dispatchReceiverParameter != null &&
            extensionReceiverParameter == null &&
            valueParameters.size == 0
