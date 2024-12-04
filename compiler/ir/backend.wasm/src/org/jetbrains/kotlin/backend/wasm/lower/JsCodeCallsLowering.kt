/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat

/**
 * Lower calls to `js(code)` into `@JsFun(code) external` functions.
 */
class JsCodeCallsLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val jsRelatedSymbols get() = context.wasmSymbols.jsRelatedSymbols

    override fun lower(irFile: IrFile) {
        if (!context.isWasmJsTarget) return
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
        val statement = body.statements.singleOrNull() ?: return null

        val isSingleExpressionJsCode: Boolean
        val jsCode: String
        when (statement) {
            is IrReturn -> {
                jsCode = statement.value.getJsCode() ?: return null
                isSingleExpressionJsCode = true
            }
            is IrCall -> {
                jsCode = statement.getJsCode() ?: return null
                isSingleExpressionJsCode = false
            }
            else -> return null
        }
        val valueParameters = function.valueParameters
        val jsFunCode = buildString {
            append('(')
            append(valueParameters.joinToString { it.name.identifier })
            append(") => ")
            if (!isSingleExpressionJsCode) append("{ ")
            append(jsCode)
            if (!isSingleExpressionJsCode) append(" }")
        }
        if (function.valueParameters.any { it.defaultValue != null }) {
            // Create a separate external function without default arguments
            // and delegate calls to it.
            val externalFun = context.irFactory.stageController.restrictTo(function) {
                createExternalJsFunction(
                    context,
                    function.name,
                    "_js_code",
                    function.returnType,
                    jsCode = jsFunCode,
                )
            }
            externalFun.copyTypeParametersFrom(function)
            externalFun.valueParameters = function.valueParameters.map { it.copyTo(externalFun, defaultValue = null) }
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                val call = irCall(externalFun.symbol)
                function.valueParameters.forEachIndexed { index, parameter ->
                    call.putValueArgument(index, irGet(parameter))
                }
                function.typeParameters.forEachIndexed { index, typeParameter ->
                    call.putTypeArgument(index, typeParameter.defaultType)
                }
                +irReturn(call)
            }
            return listOf(function, externalFun)
        }

        val builder = context.createIrBuilder(function.symbol)
        function.annotations += builder.irCallConstructor(jsRelatedSymbols.jsFunConstructor, typeArguments = emptyList()).also {
            it.putValueArgument(0, builder.irString(jsFunCode))
        }
        function.body = null
        return null
    }

    private fun transformProperty(property: IrProperty): List<IrDeclaration>? {
        val field = property.backingField ?: return null
        val initializer = field.initializer ?: return null
        val jsCode = initializer.expression.getJsCode() ?: return null
        val externalFun = context.irFactory.stageController.restrictTo(field) {
            createExternalJsFunction(
                context,
                property.name,
                "_js_code",
                field.type,
                jsCode = "() => ($jsCode)",
            )
        }
        val builder = context.createIrBuilder(field.symbol)
        initializer.expression = builder.irCall(externalFun)
        return listOf(property, externalFun)
    }

    private fun IrExpression.getJsCode(): String? {
        val call = this as? IrCall ?: return null
        if (call.symbol != jsRelatedSymbols.jsCode) return null
        return (call.getValueArgument(0) as IrConst).value as String
    }
}