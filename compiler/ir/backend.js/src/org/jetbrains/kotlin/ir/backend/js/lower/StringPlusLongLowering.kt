/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name

/**
 * This Lowering step call Long::toString before using a Long as a parameter for String::plus(Any)
 *
 * The current Long implementation implements `valueOf`, which returns the closes JavaScript Number.
 * This can cause a precision loss in case the value is outside of Number.MAX_SAFE_INTEGER and Number.MIN_SAFE_INTEGER
 * The JavaScript `+` operator will call such valueOf functions to determine what to concatenate.
 * `String::plus(Any)` is lowered to `String + Any`.
 * So a call `String::plus(Long)` can cause a precision loss, because `+` will first call valueOf before concatenating the values
 */
class StringPlusLongLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(StringPlusLongLoweringTransformer(context), container)
    }
}

private class StringPlusLongLoweringTransformer(private val context: JsIrBackendContext) : IrElementTransformer<IrDeclaration> {

    private val stringPlus = context.ir.symbols.string.functions.single { it.owner.name == Name.identifier("plus") }

    override fun visitCall(expression: IrCall, data: IrDeclaration): IrElement {
        expression.transformChildren(this, data)

        if (expression.symbol != stringPlus || expression.valueArgumentsCount != 1) return expression
        val argument = expression.getValueArgument(0) ?: return expression
        if (argument.type.canBeLong().not()) return expression

        expression.putValueArgument(0, wrapWithToString(argument))
        return expression
    }

    private fun IrType.canBeLong() = when {
        isLongClassType() || isNumberClassType() || isComparableClassType() || isAnyClassType() -> true
        else -> false
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
