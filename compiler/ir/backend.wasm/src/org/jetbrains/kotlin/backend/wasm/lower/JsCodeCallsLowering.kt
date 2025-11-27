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
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

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

    // Wrap a piece of javascript code in a function definition.
    private fun wrapJsCode(jsCode: String, function: IrSimpleFunction, forValue: Boolean): String {
        return buildString {
            append('(')
            append(function.parameters.joinToString { it.name.identifier })
            append(") => ")
            if (!forValue) append("{ ")
            append(jsCode)
            if (!forValue) append(" }")
        }
    }

    private inner class Transformer(val function: IrSimpleFunction, val externalFuns: MutableList<IrDeclaration>) : IrElementTransformerVoid() {
        private var lastDeclaration: IrDeclaration? = null
        override fun visitBody(body: IrBody): IrBody = body

        // Replace a call to 'js' with a call to a JS external
        // fun. The arguments of the external function are the same as
        // the function parameters of the original call, with any
        // default arguments made explicit. The type is also taken
        // from the function return type. This is a kludge as it is
        // not obvious how to infer the `correct` type.
        override fun visitCall(expression: IrCall): IrExpression {
            if (expression.symbol != jsRelatedSymbols.jsCode)
                return super.visitCall(expression)
            val jsCode = (expression.arguments[0] as IrConst).value as String
            // TODO: We could generate slightly more optimal code if
            // we could tell if this call is used for value or not.
            val jsFunCode = wrapJsCode(jsCode, function, true)
            val externalFun = context.irFactory.stageController.restrictTo(lastDeclaration?: function) {
                createExternalJsFunction(
                    context,
                    function.name,
                    "_js_code_${externalFuns.size}",
                    function.returnType,
                    jsCode = jsFunCode,
                )
            }
            lastDeclaration = externalFun
            externalFun.copyTypeParametersFrom(function)
            externalFun.parameters = function.parameters.map { it.copyTo(externalFun, defaultValue = null) }
            externalFuns.add(externalFun)
            return context.createIrBuilder(function.symbol, expression.startOffset, expression.endOffset).run {
                val expression = irCall(externalFun.symbol)
                function.parameters.forEachIndexed { index, parameter ->
                    expression.arguments[index] = irGet(parameter)
                }
                function.typeParameters.forEachIndexed { index, typeParameter ->
                    expression.typeArguments[index] = typeParameter.defaultType
                }
                super.visitCall(expression)
            }
        }
    }

    // If function has no default arguments and the body is a single
    // statement/expression to a call to js(...), lower the call by
    // just changing the function's annotation to that of an external
    // js function. Otherwise, create an external entry point stubs
    // for every call to js(...) in the function, where we capture
    // only the function parameters of the original function. (For
    // now, we only support JS code access to function parameters,
    // with the call type matching the return type of the function.)
    private fun transformFunction(function: IrSimpleFunction): List<IrDeclaration>? {
        val body = function.body ?: return null

        fun generalCase(): List<IrDeclaration>? {
            val declarations = mutableListOf<IrDeclaration>()
            body.transformChildrenVoid(Transformer(function, declarations))
            if (declarations.isEmpty())
                return null
            declarations.add(function)
            return declarations
        }

        val singleExpression = when (body) {
            is IrExpressionBody -> body.expression
            is IrBlockBody -> body.statements.singleOrNull()
            else -> null
        }

        if (singleExpression == null || function.parameters.any { it.defaultValue != null })
            return generalCase()

        val (jsCode: String?, forValue: Boolean) = when (singleExpression) {
            is IrReturn -> singleExpression.value.getJsCode() to true
            is IrCall -> singleExpression.getJsCode() to false
            else -> null to false
        }

        if (jsCode == null)
            return generalCase()

        // At this point, the function is eligible to be marked
        // external directly.
        val jsFunCode = wrapJsCode(jsCode, function, forValue)
        val builder = context.createIrBuilder(function.symbol)
        function.annotations += builder.irCallConstructor(jsRelatedSymbols.jsFunConstructor, typeArguments = emptyList()).also {
            it.arguments[0] = builder.irString(jsFunCode)
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
        return (call.arguments[0] as IrConst).value as String
    }
}