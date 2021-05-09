/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name

/**
 * Lowers String concatenations to a chain of String::plus
 */
class StringConcatenationLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(StringConcatenationTransformer(context), container)
    }
}

private class StringConcatenationTransformer(private val context: JsIrBackendContext) : IrElementTransformer<IrDeclaration> {

    private val stringPlus = context.ir.symbols.string.functions.single { it.owner.name == Name.identifier("plus") }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: IrDeclaration): IrExpression {
        expression.transformChildren(this, data)
        return expression
            .arguments
            .mapIndexed { index: Int, argument: IrExpression ->
                when {
                    index == 0 && argument.type.isString().not() -> wrapWithToString(argument)
                    else -> argument
                }
            }
            .reduceOrNull { acc, next ->
                JsIrBuilder.buildCall(stringPlus).apply {
                    dispatchReceiver = acc
                    putValueArgument(0, next)
                }
            }
            ?: JsIrBuilder.buildString(context.irBuiltIns.stringType, "");
    }

    private fun wrapWithToString(expression: IrExpression): IrCall {
        val typeToString = expression.type.classOrNull?.functions?.single { it.owner.name == Name.identifier("toString") }
        if (!expression.type.isNullable() && typeToString != null) {
            return JsIrBuilder.buildCall(typeToString).apply {
                dispatchReceiver = expression
            }
        }

        return JsIrBuilder.buildCall(context.ir.symbols.extensionToString).apply {
            extensionReceiver = expression
        }
    }

}
