/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.parse


fun translateJsCode(call: IrCall, scope: JsScope): JsNode {
    //TODO check non simple compile time constants (expressions)

    fun foldString(expression: IrExpression): String {
        val builder = StringBuilder()
        expression.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) = error("Parameter of js function must be compile time String constant")

            override fun <T> visitConst(expression: IrConst<T>) {
                builder.append(expression.kind.valueOf(expression))
            }

            override fun visitStringConcatenation(expression: IrStringConcatenation) = expression.acceptChildrenVoid(this)
        })

        return builder.toString()
    }

    val code = call.getValueArgument(0)!!
    val statements = parseJsCode(code.run(::foldString), JsFunctionScope(scope, "<js-code>")).orEmpty()
    val size = statements.size

    return when (size) {
        0 -> JsEmpty
        1 -> statements[0].let { (it as? JsExpressionStatement)?.expression ?: it }
        else -> JsBlock(statements)
    }
}

private fun parseJsCode(jsCode: String, currentScope: JsScope): List<JsStatement>? {
    // Parser can change local or global scope.
    // In case of js we want to keep new local names,
    // but no new global ones.
    assert(currentScope is JsFunctionScope) { "Usage of js outside of function is unexpected" }
    val temporaryRootScope = JsRootScope(JsProgram())
    val scope = DelegatingJsFunctionScopeWithTemporaryParent(currentScope as JsFunctionScope, temporaryRootScope)

    // TODO write debug info, see how it's done in CallExpressionTranslator.parseJsCode

    return parse(jsCode, ThrowExceptionOnErrorReporter, scope, "<js-code>")
}