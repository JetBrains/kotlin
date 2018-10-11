/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.COROUTINE_SWITCH
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.js.backend.ast.*

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrElementToJsStatementTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {

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
        return JsBreak(context.getNameForLoop(jump.loop)?.makeRef())
    }

    override fun visitContinue(jump: IrContinue, context: JsGenerationContext): JsStatement {
        return JsContinue(context.getNameForLoop(jump.loop)?.makeRef())
    }

    override fun visitReturn(expression: IrReturn, context: JsGenerationContext): JsStatement {
        return JsReturn(expression.value.accept(IrElementToJsExpressionTransformer(), context))
    }

    override fun visitThrow(expression: IrThrow, context: JsGenerationContext): JsStatement {
        return JsThrow(expression.value.accept(IrElementToJsExpressionTransformer(), context))
    }

    override fun visitVariable(declaration: IrVariable, context: JsGenerationContext): JsStatement {
        val varName = context.getNameForSymbol(declaration.symbol)
        return jsVar(varName, declaration.initializer, context)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsStatement {
        if (expression.symbol.owner.constructedClassType.isAny()) {
            return JsEmpty
        }
        return expression.accept(IrElementToJsExpressionTransformer(), context).makeStmt()
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, context: JsGenerationContext): JsStatement {

        // TODO: implement
        return JsEmpty
    }

    override fun visitTry(aTry: IrTry, context: JsGenerationContext): JsStatement {

        val jsTryBlock = aTry.tryResult.accept(this, context).asBlock()

        val jsCatch = aTry.catches.singleOrNull()?.let {
            val name = context.getNameForSymbol(it.catchParameter.symbol)
            val jsCatchBlock = it.result.accept(this, context)
            JsCatch(context.currentScope, name.ident, jsCatchBlock)
        }

        val jsFinallyBlock = aTry.finallyExpression?.accept(this, context)?.asBlock()

        return JsTry(jsTryBlock, jsCatch, jsFinallyBlock)
    }

    override fun visitWhen(expression: IrWhen, context: JsGenerationContext): JsStatement {
        if (expression.origin == COROUTINE_SWITCH) return toSwitch(expression, context)
        return expression.toJsNode(this, context, ::JsIf) ?: JsEmpty
    }

    private fun toSwitch(expression: IrWhen, context: JsGenerationContext): JsStatement {
        var expr: IrExpression? = null
        val cases = expression.branches.map {
            val body = it.result
            val id = if (it is IrElseBranch) null else {
                val call = it.condition as IrCall
                expr = call.getValueArgument(0) as IrExpression
                call.getValueArgument(1)
            }
            Pair(id, body)
        }

        val exprTransformer = IrElementToJsExpressionTransformer()
        val jsExpr = expr!!.accept(exprTransformer, context)

        return JsSwitch(jsExpr, cases.map { (id, body) ->

            val jsId = id?.accept(exprTransformer, context)
            val jsBody = body.accept(this, context).asBlock()
            val case: JsSwitchMember
            if (jsId == null) {
                case = JsDefault()
            } else {
                case = JsCase().also { it.caseExpression = jsId }
            }

            case.also { it.statements += jsBody.statements }
        })
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
