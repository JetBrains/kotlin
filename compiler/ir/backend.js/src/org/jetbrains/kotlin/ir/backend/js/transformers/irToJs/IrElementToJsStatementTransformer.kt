/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.JsReturn
import org.jetbrains.kotlin.js.backend.ast.JsStatement

class IrElementToJsStatementTransformer : IrElementToJsNodeTransformer<JsStatement, Nothing?> {
    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): JsStatement {
        return JsBlock(body.statements.map { it.accept(this, data) })
    }

    override fun visitExpression(expression: IrExpression, data: Nothing?): JsStatement {
        return JsExpressionStatement(expression.accept(IrElementToJsExpressionTransformer(), data))
    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): JsStatement {
        return JsReturn(expression.value.accept(IrElementToJsExpressionTransformer(), null))
    }
}
