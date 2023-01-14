/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*

/**
 *
 */
class JsCodeCallsLowering(val context: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformDeclarationsFlat { declaration ->
            when (declaration) {
                is IrSimpleFunction -> transformFunction(declaration)
                is IrProperty -> transformProperty(declaration)
                else -> null
            }
        }
    }

    private fun transformFunction(function: IrSimpleFunction): List<IrDeclaration>? {
        val body = function.body ?: return null
        check(body is IrBlockBody)  // Should be lowered to block body
        val returnStatement = body.statements.singleOrNull() as? IrReturn ?: return null
        val jsCode = returnStatement.value.getJsCode() ?: return null
        val valueParameters = function.valueParameters
        val jsFunCode = buildString {
            append('(')
            append(valueParameters.joinToString { it.name.identifier })
            append(") => (")
            append(jsCode)
            append(")")
        }
        val externalFun = createExternalJsFunction(
            context,
            function.name,
            "_js_code",
            function.returnType,
            jsCode = jsFunCode,
        )
        externalFun.valueParameters = valueParameters.map {
            it.copyTo(externalFun, varargElementType = null, defaultValue = null)
        }
        val builder = context.createIrBuilder(function.symbol)
        returnStatement.value = with(builder) {
            irCall(externalFun).apply {
                valueParameters.forEachIndexed { index, parameter ->
                    putValueArgument(index, irGet(parameter))
                }
            }
        }
        return listOf(function, externalFun)
    }

    private fun transformProperty(property: IrProperty): List<IrDeclaration>? {
        val field = property.backingField ?: return null
        val initializer = field.initializer ?: return null
        val jsCode = initializer.expression.getJsCode() ?: return null
        val externalFun = createExternalJsFunction(
            context,
            property.name,
            "_js_code",
            field.type,
            jsCode = "() => ($jsCode)",
        )
        val builder = context.createIrBuilder(field.symbol)
        initializer.expression = builder.irCall(externalFun)
        return listOf(property, externalFun)
    }

    private fun IrExpression.getJsCode(): String? {
        val call = this as? IrCall ?: return null
        if (call.symbol != context.wasmSymbols.jsCode) return null
        @Suppress("UNCHECKED_CAST")
        return (call.getValueArgument(0) as IrConst<String>).value
    }
}