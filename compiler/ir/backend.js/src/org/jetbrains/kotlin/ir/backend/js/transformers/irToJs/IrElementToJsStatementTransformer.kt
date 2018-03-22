/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.js.backend.ast.*

class IrElementToJsStatementTransformer : BaseIrElementToJsNodeTransformer<JsStatement, Nothing?> {
    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): JsStatement {
        return JsBlock(body.statements.map { it.accept(this, data) })
    }

    override fun visitExpression(expression: IrExpression, data: Nothing?): JsStatement {
        return JsExpressionStatement(expression.accept(IrElementToJsExpressionTransformer(), data))
    }

    override fun visitBreak(jump: IrBreak, data: Nothing?): JsStatement {
        return JsBreak(jump.label?.let(::JsNameRef))
    }

    override fun visitContinue(jump: IrContinue, data: Nothing?): JsStatement {
        return JsContinue(jump.label?.let(::JsNameRef))
    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): JsStatement {
        return JsReturn(expression.value.accept(IrElementToJsExpressionTransformer(), data))
    }

    override fun visitThrow(expression: IrThrow, data: Nothing?): JsStatement {
        return JsThrow(expression.value.accept(IrElementToJsExpressionTransformer(), data))
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): JsStatement {
        return jsVar(declaration.name, declaration.initializer)
    }
}
