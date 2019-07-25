/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.COROUTINE_SWITCH
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.js.backend.ast.*

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrElementToWasmStatementTransformer : BaseIrElementToWasmNodeTransformer<JsStatement, WasmStaticContext> {

    override fun visitFunction(declaration: IrFunction, data: WasmStaticContext) = JsEmpty.also {
        assert(declaration.origin == JsIrBackendContext.callableClosureOrigin) {
            "The only possible Function Declaration is one composed in Callable Reference Lowering"
        }
    }

    override fun visitBlockBody(body: IrBlockBody, context: WasmStaticContext): JsStatement {
        return JsBlock(body.statements.map { it.accept(this, context) })
    }

    override fun visitBlock(expression: IrBlock, context: WasmStaticContext): JsBlock {
        return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitComposite(expression: IrComposite, context: WasmStaticContext): JsStatement {
        // TODO introduce JsCompositeBlock?
        return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitExpression(expression: IrExpression, context: WasmStaticContext): JsStatement {
        return JsExpressionStatement(expression.accept(IrElementToWasmExpressionTransformer(), context))
    }

    override fun visitBreak(jump: IrBreak, context: WasmStaticContext): JsStatement {
        return JsBreak(context.getNameForLoop(jump.loop)?.let { JsNameRef(it) })
    }

    override fun visitContinue(jump: IrContinue, context: WasmStaticContext): JsStatement {
        return JsContinue(context.getNameForLoop(jump.loop)?.let { JsNameRef(it) })
    }

    override fun visitReturn(expression: IrReturn, context: WasmStaticContext): JsStatement {
        return JsReturn(expression.value.accept(IrElementToWasmExpressionTransformer(), context))
    }

    override fun visitThrow(expression: IrThrow, context: WasmStaticContext): JsStatement {
        return JsThrow(expression.value.accept(IrElementToWasmExpressionTransformer(), context))
    }

    override fun visitVariable(declaration: IrVariable, context: WasmStaticContext): JsStatement {
        val varName = context.getNameForValueDeclaration(declaration)
        return jsVar(varName, declaration.initializer, context)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: WasmStaticContext): JsStatement {
        if (expression.symbol.owner.constructedClassType.isAny()) {
            return JsEmpty
        }
        return expression.accept(IrElementToWasmExpressionTransformer(), context).makeStmt()
    }

    override fun visitCall(expression: IrCall, data: WasmStaticContext): JsStatement {
        return translateCall(expression, data, IrElementToWasmExpressionTransformer()).makeStmt()
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, context: WasmStaticContext): JsStatement {

        // TODO: implement
        return JsEmpty
    }

    override fun visitTry(aTry: IrTry, context: WasmStaticContext): JsStatement {

        val jsTryBlock = aTry.tryResult.accept(this, context).asBlock()

        val jsCatch = aTry.catches.singleOrNull()?.let {
            val name = context.getNameForValueDeclaration(it.catchParameter)
            val jsCatchBlock = it.result.accept(this, context)
            JsCatch(emptyScope, name.ident, jsCatchBlock)
        }

        val jsFinallyBlock = aTry.finallyExpression?.accept(this, context)?.asBlock()

        return JsTry(jsTryBlock, jsCatch, jsFinallyBlock)
    }

    override fun visitWhen(expression: IrWhen, context: WasmStaticContext): JsStatement {
        if (expression.origin == COROUTINE_SWITCH) return toSwitch(expression, context)
        return expression.toJsNode(this, context, ::JsIf) ?: JsEmpty
    }

    private fun toSwitch(expression: IrWhen, context: WasmStaticContext): JsStatement {
        var expr: IrExpression? = null
        val cases = expression.branches.map {
            val body = it.result
            val id = if (isElseBranch(it)) null else {
                val call = it.condition as IrCall
                expr = call.getValueArgument(0) as IrExpression
                call.getValueArgument(1)
            }
            Pair(id, body)
        }

        val exprTransformer = IrElementToWasmExpressionTransformer()
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

    override fun visitWhileLoop(loop: IrWhileLoop, context: WasmStaticContext): JsStatement {
        //TODO what if body null?
        val label = context.getNameForLoop(loop)
        val loopStatement = JsWhile(loop.condition.accept(IrElementToWasmExpressionTransformer(), context), loop.body?.accept(this, context))
        return label?.let { JsLabel(it, loopStatement) } ?: loopStatement
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, context: WasmStaticContext): JsStatement {
        //TODO what if body null?
        val label = context.getNameForLoop(loop)
        val loopStatement =
            JsDoWhile(loop.condition.accept(IrElementToWasmExpressionTransformer(), context), loop.body?.accept(this, context))
        return label?.let { JsLabel(it, loopStatement) } ?: loopStatement
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: WasmStaticContext): JsStatement {
        return JsStringLiteral("WASM TODO: Synthetic body").makeStmt()
    }
}
