/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.render


class ReplaceCallsWithInvalidTypeArgumentForReifiedParameters(val context: JsIrBackendContext) : CallsTransformer {
    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        if (!context.errorPolicy.allowErrors) return call

        val function = call.symbol.owner

        for (typeParameter in function.typeParameters) {
            if (!typeParameter.isReified) continue
            val typeArgument = call.getTypeArgument(typeParameter.index)

            if (typeArgument?.classOrNull == null) {
                val args = call.getArgumentsWithIr().map { it.second }

                val callErrorCode = JsIrBuilder.buildCall(context.errorCodeSymbol!!).apply {
                    putValueArgument(
                        0,
                        IrConstImpl.string(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.stringType,
                            "Invalid type argument (${typeArgument?.render()}) for reified type parameter (${typeParameter.render()})"
                        )
                    )
                }

                if (args.isEmpty()) return callErrorCode

                return IrCompositeImpl(-1, -1, call.type, call.origin, args + listOf(callErrorCode))
            }
        }

        return call
    }
}
