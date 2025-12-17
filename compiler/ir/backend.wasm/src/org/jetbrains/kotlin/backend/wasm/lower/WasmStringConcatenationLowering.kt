/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.types.isStringClassType
import org.jetbrains.kotlin.ir.util.isNullable

/**
 * This lowering pass replaces [IrStringConcatenation]s with String plus operations.
 *
 * Current String implementation for K/Wasm uses js-string builtins in Wasm.
 * As js-strings are more efficient than CharArray, StringConcatenation is done via String `plus` calls, that will be lowered to `concat` js-string builtin calls in Wasm.
 */
class WasmStringConcatenationLowering(context: CommonBackendContext) : FileLoweringPass, IrBuildingTransformer(context) {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    private val symbols = context.symbols

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression.transformChildrenVoid(this)

        builder.at(expression)
        val arguments = expression.arguments
        return when {
            arguments.isEmpty() -> builder.irString("")

            else -> {
                var concatenatedString: IrExpression
                if (!arguments[0].type.isStringClassType() || arguments[0].type.isNullable()) {
                    val functionSymbol =
                        if (arguments[0].type.isNullable()) symbols.extensionToString
                        else symbols.memberToString
                    concatenatedString = builder.irCall(functionSymbol).apply {
                        this.arguments[0] = arguments[0]
                    }
                } else {
                    concatenatedString = arguments[0]
                }

                arguments.drop(1).forEach { arg ->
                    concatenatedString = builder.irCall(symbols.memberStringPlus).apply {
                        this.arguments[0] = concatenatedString
                        this.arguments[1] = arg
                    }
                }
                concatenatedString
            }
        }
    }
}
