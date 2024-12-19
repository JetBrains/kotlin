/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import com.google.gwt.dev.js.rhino.CodePosition
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.backend.js.lower.PropertyLazyInitLowering
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.parseExpressionOrStatement

/**
 * Returns null if constant expression could not be parsed.
 */
fun translateJsCodeIntoStatementList(code: IrExpression, container: IrDeclaration) =
    translateJsCodeIntoStatementList(
        code,
        code.getStartSourceLocation(container) ?: container.fileOrNull?.fileEntry?.let { JsLocation(it.name, 0, 0) }
    )

/**
 * Returns null if constant expression could not be parsed.
 */
fun translateJsCodeIntoStatementList(code: IrExpression, fileEntry: IrFileEntry) =
    translateJsCodeIntoStatementList(code, code.getStartSourceLocation(fileEntry) ?: JsLocation(fileEntry.name, 0, 0))

private fun translateJsCodeIntoStatementList(
    code: IrExpression,
    sourceInfo: JsLocation?
): List<JsStatement>? {
    // TODO: support proper symbol linkage and label clash resolution
    val (fileName, startLine, offset) = sourceInfo ?: JsLocation("<js-code>", 0, 0)
    val jsCode = foldString(code) ?: return null

    // Parser can change local or global scope.
    // In case of js we want to keep new local names,
    // but no new global ones.

    val temporaryRootScope = JsRootScope(JsProgram())
    val currentScope = JsFunctionScope(temporaryRootScope, "js")

    // NOTE: emitting the correct debug info for JS injections is a non-trivial task, because the injection may consist of
    // constant-evaluated strings, whose origin is hard to track in the JS parser. Also, the presence of explicit \n characters in
    // the JS string literal breaks the debug info, because at this level we are unable to distinguish multiline string literals and
    // single-line string literals with explicit \n in it.
    //
    // So we try to generate the debug info on the best-effort basis. It should work correctly with plain string literals without
    // concatenations, interpolations or backslash replacements like \n.
    return parseExpressionOrStatement(jsCode, ThrowExceptionOnErrorReporter, currentScope, CodePosition(startLine, offset), fileName)
}

private var IrField.lazyInitializerExpression: IrExpression? by irAttribute(followAttributeOwner = false)

private fun foldString(expression: IrExpression): String? {
    val builder = StringBuilder()
    var foldingFailed = false
    expression.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            foldingFailed = true
        }

        override fun visitGetValue(expression: IrGetValue) {
            expression.symbol.owner.acceptVoid(this)
        }

        override fun visitVariable(declaration: IrVariable) {
            declaration.initializer?.let {
                it.acceptVoid(this)
                return
            }

            super.visitVariable(declaration)
        }

        override fun visitGetField(expression: IrGetField) {
            val owner = expression.symbol.owner
            (owner.initializer?.expression ?: owner.lazyInitializerExpression)
                ?.acceptVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            val owner = expression.symbol.owner
            val propertySymbol = owner.correspondingPropertySymbol
            return when {
                expression.origin == IrStatementOrigin.PLUS ->
                    expression.acceptChildrenVoid(this)
                expression.origin == PropertyLazyInitLowering.PROPERTY_INIT_FUN_CALL -> {
                    owner.body?.acceptChildrenVoid(InitFunVisitor())
                    expression.acceptChildrenVoid(this)
                }
                propertySymbol != null && owner == propertySymbol.owner.getter -> {
                    if (propertySymbol.owner.isConst) {
                        val initializer = propertySymbol.owner.backingField?.initializer
                        if (initializer != null) {
                            initializer.acceptChildrenVoid(this)
                        } else {
                            foldingFailed = true
                        }
                    } else {
                        owner.body?.acceptChildrenVoid(this)
                    }
                    expression.acceptChildrenVoid(this)
                }
                else -> super.visitCall(expression)
            }
        }

        override fun visitReturn(expression: IrReturn) {
            expression.acceptChildrenVoid(this)
        }

        override fun visitConst(expression: IrConst) {
            builder.append(expression.value)
        }

        override fun visitStringConcatenation(expression: IrStringConcatenation) = expression.acceptChildrenVoid(this)
    })

    if (foldingFailed) return null

    return builder.toString()
}

private class InitFunVisitor : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSetField(expression: IrSetField) {
        expression.symbol.owner.lazyInitializerExpression = expression.value
    }
}
