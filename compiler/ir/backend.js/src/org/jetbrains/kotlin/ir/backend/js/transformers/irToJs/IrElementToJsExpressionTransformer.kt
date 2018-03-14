/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral

class IrElementToJsExpressionTransformer : IrElementToJsNodeTransformer<JsExpression, Nothing?> {
    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): JsExpression {
        return JsStringLiteral(expression.value.toString())
    }
}