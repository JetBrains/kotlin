/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.js.backend.ast.*

class IrDeclarationToJsTransformer : IrElementToJsNodeTransformer<JsStatement, Nothing?> {
    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): JsStatement {
        return JsExpressionStatement(transformIrFunctionToJsFunction(declaration))
    }

    private fun transformIrFunctionToJsFunction(declaration: IrSimpleFunction): JsFunction {
        val funName = declaration.name.asString()
        val body = declaration.body?.accept(IrElementToJsStatementTransformer(), null) as? JsBlock ?: JsBlock()
        val function = JsFunction(JsFunctionScope(JsDynamicScope, "scope for $funName"), body, "function $funName")

        function.name = JsDynamicScope.declareName(funName)

        fun JsFunction.addParameter(parameterName: String) {
            val parameter = function.scope.declareName(parameterName)
            parameters.add(JsParameter(parameter))
        }

        declaration.extensionReceiverParameter?.let { function.addParameter("\$receiver") }
        declaration.valueParameters.forEach {
            function.addParameter(it.name.asString())
        }

        return function
    }

}
