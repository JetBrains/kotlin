/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.js.backend.ast.*

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

fun translateFunction(declaration: IrFunction, name: JsName?, isObjectConstructor: Boolean, context: JsGenerationContext): JsFunction {
    val functionScope = JsFunctionScope(context.currentScope, "scope for ${name ?: "annon"}")
    val functionContext = context.newDeclaration(functionScope, declaration)
    val functionParams = declaration.valueParameters.map { functionContext.getNameForSymbol(it.symbol) }
    val body = declaration.body?.accept(IrElementToJsStatementTransformer(), functionContext) as? JsBlock ?: JsBlock()

    val functionBody = if (isObjectConstructor) {
        val instanceName = context.currentScope.declareName(name!!.objectInstanceName())
        val assignObject = jsAssignment(JsNameRef(instanceName), JsThisRef())
        JsBlock(assignObject.makeStmt(), body)
    } else body

    val function = JsFunction(functionScope, functionBody, "member function ${name ?: "annon"}")

    function.name = name

    fun JsFunction.addParameter(parameter: JsName) {
        parameters.add(JsParameter(parameter))
    }

    declaration.extensionReceiverParameter?.let { function.addParameter(functionContext.getNameForSymbol(it.symbol)) }
    functionParams.forEach { function.addParameter(it) }
    if (declaration.descriptor.isSuspend) {
        function.addParameter(context.currentScope.declareName(Namer.CONTINUATION))
    }

    return function
}

fun translateCallArguments(expression: IrMemberAccessExpression, context: JsGenerationContext): List<JsExpression> {
    val transformer = IrElementToJsExpressionTransformer()
    val size = expression.valueArgumentsCount

    val arguments = (0 until size).mapTo(ArrayList(size)) {
        val argument = expression.getValueArgument(it)
        val result = argument?.accept(transformer, context) ?: JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1))
        result
    }

    return if (expression.descriptor.isSuspend) {
        arguments + context.continuation
    } else arguments
}

val IrFunction.isStatic: Boolean get() = this.dispatchReceiverParameter == null

fun JsStatement.asBlock() = this as? JsBlock ?: JsBlock(this)

fun JsName.objectInstanceName() = "${ident}_instance"