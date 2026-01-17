/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.forAllReifiedTypeParameters
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.JsStandardClassIds

@OptIn(SymbolInternals::class)
object FirJsReifiedJsNoRuntimeChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        expression.forAllReifiedTypeParameters { type, typeArgument ->
            val typeSymbol = type.toSymbol() ?: return@forAllReifiedTypeParameters
            val fir = typeSymbol.fir
            if ((fir as? FirClass)?.classKind == ClassKind.INTERFACE &&
                fir.hasAnnotation(JsStandardClassIds.Annotations.JsNoRuntime, context.session)
            ) {
                reporter.reportOn(
                    typeArgument.source ?: expression.source,
                    FirJsErrors.JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT,
                    type
                )
            }
        }
    }
}
