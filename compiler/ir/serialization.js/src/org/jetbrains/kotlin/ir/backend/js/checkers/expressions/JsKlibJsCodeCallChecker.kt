/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers.expressions

import com.google.gwt.dev.js.parserExceptions.AbortParsingException
import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibCallChecker
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibDiagnosticContext
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibErrors
import org.jetbrains.kotlin.ir.backend.js.checkers.at
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.js.backend.ast.JsFunctionScope
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.backend.ast.JsRootScope
import org.jetbrains.kotlin.js.parser.parseExpressionOrStatement
import org.jetbrains.kotlin.name.JsStandardClassIds

object JsKlibJsCodeCallChecker : JsKlibCallChecker {
    private val jsCodeFqName = JsStandardClassIds.Callables.JsCode.asSingleFqName()

    override fun check(expression: IrCall, context: JsKlibDiagnosticContext, reporter: IrDiagnosticReporter) {
        // Do not check IR from K1, because there are corresponding K1 FE checks in JsCallChecker
        if (!context.compilerConfiguration.languageVersionSettings.languageVersion.usesK2) {
            return
        }
        if (expression.symbol.owner.fqNameWhenAvailable != jsCodeFqName) {
            return
        }

        val jsCodeExpr = expression.getValueArgument(0)
        // K2 frontend checker FirJsCodeConstantArgumentChecker checks that the passing argument is a constant.
        // IrConstOnlyNecessaryTransformer must evaluate an argument expression and fold it into a string constant.
        if (jsCodeExpr !is IrConst<*> || jsCodeExpr.kind != IrConstKind.String) {
            // Do not ignore the error if the argument does not fit the expectations.
            // The IR can be generated in the plugin, avoiding FE checks and IrConstOnlyNecessaryTransformer.
            // Bugs are also possible in IrConstOnlyNecessaryTransformer.
            reporter.at(jsCodeExpr ?: expression, context).report(JsKlibErrors.JSCODE_CAN_NOT_VERIFY_JAVASCRIPT)
            return
        }

        val jsCodeStr = IrConstKind.String.valueOf(jsCodeExpr)

        try {
            val parserScope = JsFunctionScope(JsRootScope(JsProgram()), "<js fun>")
            val fileName = context.containingFile?.fileEntry?.name ?: "<unknown file>"
            val jsErrorReporter = JsErrorReporter(jsCodeExpr, context, reporter)
            val statements = parseExpressionOrStatement(jsCodeStr, jsErrorReporter, parserScope, CodePosition(0, 0), fileName)
            if (statements.isNullOrEmpty()) {
                reporter.at(jsCodeExpr, context).report(JsKlibErrors.JSCODE_NO_JAVASCRIPT_PRODUCED)
            }
        } catch (e: AbortParsingException) {
            // ignore
        }
    }

    private class JsErrorReporter(
        val codeExpression: IrExpression,
        val context: JsKlibDiagnosticContext,
        val reporter: IrDiagnosticReporter,
    ) : ErrorReporter {
        override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) {
            reporter.at(codeExpression, context).report(JsKlibErrors.JSCODE_WARNING, message)
        }

        override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) {
            reporter.at(codeExpression, context).report(JsKlibErrors.JSCODE_ERROR, message)
            throw AbortParsingException()
        }
    }
}
