/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.common.ir.isElseBranch
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
        if (isElseBranch(br)) body
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

fun JsStatement.asBlock() = this as? JsBlock ?: JsBlock(this)

fun JsName.objectInstanceName() = "${ident}_instance"

fun defineProperty(receiver: JsExpression, name: String, value: () -> JsExpression): JsInvocation {
    val objectDefineProperty = JsNameRef("defineProperty", Namer.JS_OBJECT)
    return JsInvocation(objectDefineProperty, receiver, JsStringLiteral(name), value())
}


fun defineProperty(receiver: JsExpression, name: String, getter: JsExpression?, setter: JsExpression? = null) =
    defineProperty(receiver, name) {
        val literal = JsObjectLiteral(true)
        literal.apply {
            if (getter != null)
                propertyInitializers += JsPropertyInitializer(JsStringLiteral("get"), getter)
            if (setter != null)
                propertyInitializers += JsPropertyInitializer(JsStringLiteral("set"), setter)
        }
    }


// Partially copied from org.jetbrains.kotlin.js.translate.utils.JsAstUtils
object JsAstUtils {
    private fun deBlockIfPossible(statement: JsStatement): JsStatement {
        return if (statement is JsBlock && statement.statements.size == 1) {
            statement.statements[0]
        } else {
            statement
        }
    }

    fun newJsIf(
        ifExpression: JsExpression,
        thenStatement: JsStatement,
        elseStatement: JsStatement? = null
    ): JsIf {
        var elseStatement = elseStatement
        elseStatement = if (elseStatement != null) deBlockIfPossible(elseStatement) else null
        return JsIf(ifExpression, deBlockIfPossible(thenStatement), elseStatement)
    }

    fun and(op1: JsExpression, op2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.AND, op1, op2)
    }

    fun or(op1: JsExpression, op2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.OR, op1, op2)
    }

    fun equality(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.REF_EQ, arg1, arg2)
    }

    fun inequality(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.REF_NEQ, arg1, arg2)
    }

    fun lessThanEq(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.LTE, arg1, arg2)
    }

    fun lessThan(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.LT, arg1, arg2)
    }

    fun greaterThan(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.GT, arg1, arg2)
    }

    fun greaterThanEq(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.GTE, arg1, arg2)
    }

    fun assignment(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ASG, left, right)
    }

    fun assignmentToThisField(fieldName: String, right: JsExpression): JsStatement {
        return assignment(JsNameRef(fieldName, JsThisRef()), right).source(right.source).makeStmt()
    }

    fun decomposeAssignment(expr: JsExpression): Pair<JsExpression, JsExpression>? {
        if (expr !is JsBinaryOperation) return null

        return if (expr.operator != JsBinaryOperator.ASG) null else Pair(expr.arg1, expr.arg2)

    }

    fun sum(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ADD, left, right)
    }

    fun addAssign(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ASG_ADD, left, right)
    }

    fun subtract(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.SUB, left, right)
    }

    fun mul(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.MUL, left, right)
    }

    fun div(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.DIV, left, right)
    }

    fun mod(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.MOD, left, right)
    }

    fun not(expression: JsExpression): JsPrefixOperation {
        return JsPrefixOperation(JsUnaryOperator.NOT, expression)
    }

    fun typeOfIs(expression: JsExpression, string: JsStringLiteral): JsBinaryOperation {
        return equality(JsPrefixOperation(JsUnaryOperator.TYPEOF, expression), string)
    }

    fun newVar(name: JsName, expr: JsExpression?): JsVars {
        return JsVars(JsVars.JsVar(name, expr))
    }
}