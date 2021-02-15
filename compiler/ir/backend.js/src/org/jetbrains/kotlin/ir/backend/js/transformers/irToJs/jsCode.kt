/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import com.google.gwt.dev.js.rhino.CodePosition
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.JsFunctionScope
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.backend.ast.JsRootScope
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.parser.parseExpressionOrStatement

// Returns null if constant expression could not be parsed
fun translateJsCodeIntoStatementList(code: IrExpression): List<JsStatement>? {
    // TODO: support proper symbol linkage and label clash resolution

    fun foldString(expression: IrExpression): String? {
        val builder = StringBuilder()
        var foldingFailed = false
        expression.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                foldingFailed = true
            }

            override fun <T> visitConst(expression: IrConst<T>) {
                builder.append(expression.kind.valueOf(expression))
            }

            override fun visitStringConcatenation(expression: IrStringConcatenation) = expression.acceptChildrenVoid(this)
        })

        if (foldingFailed) return null

        return builder.toString()
    }

    return parseJsCode(foldString(code) ?: return null)
}

fun parseJsCode(jsCode: String): List<JsStatement>? {
    // Parser can change local or global scope.
    // In case of js we want to keep new local names,
    // but no new global ones.

    val temporaryRootScope = JsRootScope(JsProgram())
    val currentScope = JsFunctionScope(temporaryRootScope, "js")

    // TODO: write debug info, see how it's done in CallExpressionTranslator.parseJsCode

    return parseExpressionOrStatement(jsCode, ThrowExceptionOnErrorReporter, currentScope, CodePosition(0, 0), "<js-code>")
}