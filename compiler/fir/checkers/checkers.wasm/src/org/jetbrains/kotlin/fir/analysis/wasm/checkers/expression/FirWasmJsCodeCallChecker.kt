/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.hasValidJsCodeBody
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

object FirWasmJsCodeCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return

        if (symbol.callableId != WebCommonStandardClassIds.Callables.Js) {
            return
        }

        val containingDeclarations = context.containingDeclarations

        val containingDeclaration: FirDeclaration = containingDeclarations.lastOrNull() ?: return

        val containingDeclarationOfContainingDeclaration =
            containingDeclarations.getOrNull(containingDeclarations.size - 2)

        val isContainingDeclarationTopLevel =
            containingDeclarationOfContainingDeclaration is FirFile || containingDeclarationOfContainingDeclaration is FirScript

        val source = expression.calleeReference.source

        if (!isContainingDeclarationTopLevel) {
            reporter.reportOn(source, FirWasmErrors.JSCODE_WRONG_CONTEXT)
            return
        }

        when (containingDeclaration) {
            is FirSimpleFunction -> {
                if (!containingDeclaration.hasValidJsCodeBody()) {
                    reporter.reportOn(source, FirWasmErrors.JSCODE_WRONG_CONTEXT)
                } else {
                    if (containingDeclaration.isSuspend) {
                        reporter.reportOn(source, FirWasmErrors.JSCODE_UNSUPPORTED_FUNCTION_KIND, "suspend function")
                    }
                    if (containingDeclaration.isInline) {
                        reporter.reportOn(source, FirWasmErrors.JSCODE_UNSUPPORTED_FUNCTION_KIND, "inline function")
                    }
                    if (containingDeclaration.isExtension) {
                        reporter.reportOn(
                            source,
                            FirWasmErrors.JSCODE_UNSUPPORTED_FUNCTION_KIND,
                            "function with extension receiver"
                        )
                    }
                    for (parameter in containingDeclaration.valueParameters) {
                        if (parameter.name.identifierOrNullIfSpecial?.isValidES5Identifier() != true) {
                            reporter.reportOn(parameter.source, FirWasmErrors.JSCODE_INVALID_PARAMETER_NAME)
                        }
                    }
                }
            }
            is FirProperty -> {
                if (!containingDeclaration.hasValidJsCodeBody()) {
                    reporter.reportOn(source, FirWasmErrors.JSCODE_WRONG_CONTEXT)
                }
            }
            else -> {
                reporter.reportOn(source, FirWasmErrors.JSCODE_WRONG_CONTEXT)
            }
        }
    }
}
