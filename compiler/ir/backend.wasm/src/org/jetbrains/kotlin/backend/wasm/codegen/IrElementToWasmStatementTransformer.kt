/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.WasmInstruction
import org.jetbrains.kotlin.backend.wasm.ast.WasmReturn
import org.jetbrains.kotlin.ir.backend.js.utils.IrNamer
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isUnit

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrElementToWasmStatementTransformer : BaseIrElementToWasmNodeTransformer<WasmInstruction, IrNamer> {
    override fun visitBlockBody(body: IrBlockBody, context: IrNamer): WasmInstruction {
        TODO()
        // return JsBlock(body.statements.map { it.accept(this, context) })
    }

    override fun visitBlock(expression: IrBlock, context: IrNamer): WasmInstruction {
        TODO()
        // return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitComposite(expression: IrComposite, context: IrNamer): WasmInstruction {
        TODO()
        // return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitExpression(expression: IrExpression, context: IrNamer): WasmInstruction {
        TODO()
        // return JsExpressionStatement(expression.accept(IrElementToWasmExpressionTransformer(), context))
    }

    override fun visitBreak(jump: IrBreak, context: IrNamer): WasmInstruction {

        TODO()
        // return JsBreak(context.getNameForLoop(jump.loop)?.let { JsNameRef(it) })
    }

    override fun visitContinue(jump: IrContinue, context: IrNamer): WasmInstruction {
        TODO()
        // return JsContinue(context.getNameForLoop(jump.loop)?.let { JsNameRef(it) })
    }

    override fun visitReturn(expression: IrReturn, context: IrNamer): WasmInstruction {
        if (expression.value.type.isUnit()) return WasmReturn(null)

        return WasmReturn(
            expressionToWasmInstruction(expression.value, context)
        )
    }

    override fun visitThrow(expression: IrThrow, context: IrNamer): WasmInstruction {
        TODO("IrThrow")
    }

    override fun visitVariable(declaration: IrVariable, context: IrNamer): WasmInstruction {
        val varName = context.getNameForValueDeclaration(declaration)
        TODO()
        // return jsVar(varName, declaration.initializer, context)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: IrNamer): WasmInstruction {
        TODO("IrDelegatingConstructorCall")
    }

    override fun visitCall(expression: IrCall, data: IrNamer): WasmInstruction {
        TODO()
        // return translateCall(expression, data, IrElementToWasmExpressionTransformer()).makeStmt()
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, context: IrNamer): WasmInstruction {
        TODO("IrInstanceInitializerCall")
    }

    override fun visitTry(aTry: IrTry, context: IrNamer): WasmInstruction {
        TODO("IrTry")
    }

    override fun visitWhen(expression: IrWhen, context: IrNamer): WasmInstruction {
        TODO()
        // return expression.toJsNode(this, context, ::JsIf) ?: JsEmpty
    }

    override fun visitWhileLoop(loop: IrWhileLoop, context: IrNamer): WasmInstruction {
        //TODO what if body null?
        val label = context.getNameForLoop(loop)
        TODO()
        // val loopStatement = JsWhile(loop.condition.accept(IrElementToWasmExpressionTransformer(), context), loop.body?.accept(this, context))
        // return label?.let { JsLabel(it, loopStatement) } ?: loopStatement
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, context: IrNamer): WasmInstruction {
        TODO()
//        val label = context.getNameForLoop(loop)
//        val loopStatement =
//            JsDoWhile(loop.condition.accept(IrElementToWasmExpressionTransformer(), context), loop.body?.accept(this, context))
//        return label?.let { JsLabel(it, loopStatement) } ?: loopStatement
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: IrNamer): WasmInstruction {
        TODO("IrSyntheticBody")
    }
}
