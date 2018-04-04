/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.name.Name

// TODO don't use JsDynamicScope
val dummyScope = JsDynamicScope

fun Name.toJsName() =
// TODO sanitize
    dummyScope.declareName(asString())

fun jsVar(name: JsName, initializer: IrExpression?, context: JsGenerationContext): JsVars {
    val jsInitializer = initializer?.accept(IrElementToJsExpressionTransformer(), context)
    return JsVars(JsVars.JsVar(name, jsInitializer))
}

fun <T : JsNode, D : JsGenerationContext> IrWhen.toJsNode(
    tr: BaseIrElementToJsNodeTransformer<T, D>,
    data: D,
    node: (JsExpression, T, T?) -> T
): T? =
    branches.foldRight<IrBranch, T?>(null) { br, n ->
        val body = br.result.accept(tr, data)
        if (br is IrElseBranch) body
        else {
            val condition = br.condition.accept(IrElementToJsExpressionTransformer(), data)
            node(condition, body, n)
        }
    }

fun jsAssignment(left: JsExpression, right: JsExpression) = JsBinaryOperation(JsBinaryOperator.ASG, left, right)

fun prototypeOf(classNameRef: JsExpression) = JsNameRef(Namer.PROTOTYPE_NAME, classNameRef)

fun translateFunction(declaration: IrFunction, name: JsName?, context: JsGenerationContext): JsFunction {
    val functionScope = JsFunctionScope(context.currentScope, "scope for ${name ?: "annon"}")
    val functionContext = context.newDeclaration(functionScope, declaration)
    val body = declaration.body?.accept(IrElementToJsStatementTransformer(), functionContext) as? JsBlock ?: JsBlock()
    val function = JsFunction(functionScope, body, "member function ${name ?: "annon"}")

    function.name = name

    fun JsFunction.addParameter(parameterName: String) {
        val parameter = function.scope.declareName(parameterName)
        parameters.add(JsParameter(parameter))
    }

    declaration.extensionReceiverParameter?.let { function.addParameter(context.getNameForSymbol(it.symbol).ident) }
    declaration.valueParameters.forEach { function.addParameter(context.getNameForSymbol(it.symbol).ident) }

    return function
}

fun translateCallArguments(expression: IrMemberAccessExpression, context: JsGenerationContext): List<JsExpression> {
    val transformer = IrElementToJsExpressionTransformer()
    val size = expression.valueArgumentsCount

    return (0 until size).mapTo(ArrayList(size)) {
        val argument = expression.getValueArgument(it)
        val result = argument?.accept(transformer, context) ?: JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1))
        result
    }
}