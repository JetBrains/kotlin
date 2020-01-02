/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.js.backend.ast.*

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrElementToJsStatementTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {

    override fun visitFunction(declaration: IrFunction, data: JsGenerationContext) = JsEmpty.also {
        assert(declaration.origin == JsIrBackendContext.callableClosureOrigin) {
            "The only possible Function Declaration is one composed in Callable Reference Lowering"
        }
    }

    override fun visitBlockBody(body: IrBlockBody, context: JsGenerationContext): JsStatement {
        return JsBlock(body.statements.map { it.accept(this, context) })
    }

    override fun visitBlock(expression: IrBlock, context: JsGenerationContext): JsBlock {
        return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitComposite(expression: IrComposite, context: JsGenerationContext): JsStatement {
        // TODO introduce JsCompositeBlock?
        return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitExpression(expression: IrExpression, context: JsGenerationContext): JsStatement {
        return JsExpressionStatement(expression.accept(IrElementToJsExpressionTransformer(), context))
    }

    override fun visitBreak(jump: IrBreak, context: JsGenerationContext): JsStatement {
        return JsBreak(context.getNameForLoop(jump.loop)?.let { JsNameRef(it) })
    }

    override fun visitContinue(jump: IrContinue, context: JsGenerationContext): JsStatement {
        return JsContinue(context.getNameForLoop(jump.loop)?.let { JsNameRef(it) })
    }

    private fun IrExpression.maybeOptimizeIntoSwitch(context: JsGenerationContext, transformer: (JsExpression) -> JsStatement): JsStatement {
        if (this is IrWhen) {
            val stmtTransformer = { stmt: JsStatement -> transformer((stmt as JsExpressionStatement).expression) }
            SwitchOptimizer(context, stmtTransformer).tryOptimize(this)?.let { return it }
        }

        return transformer(accept(IrElementToJsExpressionTransformer(), context))
    }

    override fun visitSetField(expression: IrSetField, context: JsGenerationContext): JsStatement {
        val fieldName = context.getNameForField(expression.symbol.owner)
        val expressionTransformer = IrElementToJsExpressionTransformer()
        val dest = JsNameRef(fieldName, expression.receiver?.accept(expressionTransformer, context))
        return expression.value.maybeOptimizeIntoSwitch(context) { jsAssignment(dest, it).makeStmt() }
    }

    override fun visitSetVariable(expression: IrSetVariable, context: JsGenerationContext): JsStatement {
        val ref = JsNameRef(context.getNameForValueDeclaration(expression.symbol.owner))
        return expression.value.maybeOptimizeIntoSwitch(context) { JsBinaryOperation(JsBinaryOperator.ASG, ref, it).makeStmt() }
    }

    override fun visitReturn(expression: IrReturn, context: JsGenerationContext): JsStatement {
        return expression.value.maybeOptimizeIntoSwitch(context) { JsReturn(it) }
    }

    override fun visitThrow(expression: IrThrow, context: JsGenerationContext): JsStatement {
        return expression.value.maybeOptimizeIntoSwitch(context) { JsThrow(it) }
    }

    override fun visitVariable(declaration: IrVariable, context: JsGenerationContext): JsStatement {
        val varName = context.getNameForValueDeclaration(declaration)
        val value = declaration.initializer

        if (value is IrWhen) {
            val varRef = varName.makeRef()
            val transformer = { stmt: JsStatement ->
                val expr = (stmt as JsExpressionStatement).expression
                JsBinaryOperation(JsBinaryOperator.ASG, varRef, expr).makeStmt()
            }

            SwitchOptimizer(context, transformer).tryOptimize(value)?.let {
                return JsBlock(JsVars(JsVars.JsVar(varName)), it)
            }
        }

        return jsVar(varName, value, context)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsStatement {
        if (expression.symbol.owner.constructedClassType.isAny()) {
            return JsEmpty
        }
        return expression.accept(IrElementToJsExpressionTransformer(), context).makeStmt()
    }

    override fun visitCall(expression: IrCall, data: JsGenerationContext): JsStatement {
        if (data.checkIfJsCode(expression.symbol)) {
            val statements = translateJsCodeIntoStatementList(expression.getValueArgument(0) ?: error("JsCode is expected"))
            return when (statements.size) {
                0 -> JsEmpty
                1 -> statements.single()
                // TODO: use transparent block (e.g. JsCompositeBlock)
                else -> JsBlock(statements)
            }
        }
        return translateCall(expression, data, IrElementToJsExpressionTransformer()).makeStmt()
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, context: JsGenerationContext): JsStatement {

        // TODO: implement
        return JsEmpty
    }

    override fun visitTry(aTry: IrTry, context: JsGenerationContext): JsStatement {

        val jsTryBlock = aTry.tryResult.accept(this, context).asBlock()

        val jsCatch = aTry.catches.singleOrNull()?.let {
            val name = context.getNameForValueDeclaration(it.catchParameter)
            val jsCatchBlock = it.result.accept(this, context)
            JsCatch(emptyScope, name.ident, jsCatchBlock)
        }

        val jsFinallyBlock = aTry.finallyExpression?.accept(this, context)?.asBlock()

        return JsTry(jsTryBlock, jsCatch, jsFinallyBlock)
    }

    override fun visitWhen(expression: IrWhen, context: JsGenerationContext): JsStatement {
        return SwitchOptimizer(context).tryOptimize(expression) ?: expression.toJsNode(this, context, ::JsIf) ?: JsEmpty
    }

    override fun visitWhileLoop(loop: IrWhileLoop, context: JsGenerationContext): JsStatement {
        //TODO what if body null?
        val label = context.getNameForLoop(loop)
        val loopStatement = JsWhile(loop.condition.accept(IrElementToJsExpressionTransformer(), context), loop.body?.accept(this, context))
        return label?.let { JsLabel(it, loopStatement) } ?: loopStatement
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, context: JsGenerationContext): JsStatement {
        //TODO what if body null?
        val label = context.getNameForLoop(loop)
        val loopStatement =
            JsDoWhile(loop.condition.accept(IrElementToJsExpressionTransformer(), context), loop.body?.accept(this, context))
        return label?.let { JsLabel(it, loopStatement) } ?: loopStatement
    }
}
