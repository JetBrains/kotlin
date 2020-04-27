/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isString
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

private class StringConcatenationTransformer(val lower: SimpleStringConcatenationLowering) : IrElementTransformerVoidWithContext() {
    private val context = lower.context
    private val irBuiltIns = context.irBuiltIns
    private val stringPlus = irBuiltIns.stringClass.owner.declarations.filterIsInstance<IrSimpleFunction>().find {
        it.name == Name.identifier("plus")
    }!!

    private val anyToString = irBuiltIns.anyClass.owner.declarations.filterIsInstance<IrSimpleFunction>().find {
        it.name == Name.identifier("toString")
    }!!

    private val anyNToString = context.wasmSymbols.anyNtoString


    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        val transformed = super.visitStringConcatenation(expression) as IrStringConcatenation
        val builder: DeclarationIrBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression)
        return transformed.arguments.fold<IrExpression, IrExpression>(
            builder.irString("")
        ) { acc, el ->
            builder.irCall(stringPlus).apply {
                dispatchReceiver = acc
                putValueArgument(0, expressionToString(el, builder))
            }
        }
    }

    private fun expressionToString(expression: IrExpression, builder: DeclarationIrBuilder): IrExpression {
        if (expression.type.isString()) return expression
        builder.at(expression)
        val klass = expression.type.getClass()

        if (expression.type.isNullable()) {
            return builder.irCall(anyNToString).apply {
                putValueArgument(0, expression)
            }
        }

        val toStringMethod: IrSimpleFunction = if (klass != null) {
            klass.declarations.filterIsInstance<IrSimpleFunction>().find { it.isToStringInheritedFromAny() }!!
        } else {
            anyToString
        }

        return builder.irCall(toStringMethod).apply {
            dispatchReceiver = expression
        }
    }
}

fun IrFunction.isToStringInheritedFromAny() =
    name == Name.identifier("toString") &&
            dispatchReceiverParameter != null &&
            extensionReceiverParameter == null &&
            valueParameters.isEmpty()
