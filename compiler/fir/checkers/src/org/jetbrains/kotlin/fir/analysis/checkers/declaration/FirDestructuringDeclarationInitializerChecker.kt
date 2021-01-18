/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extended.report
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression

object FirDestructuringDeclarationInitializerChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.name.isSpecial || declaration.name.asString() != "<destruct>") return
        val source = declaration.source
        if (source == null || source.kind is FirFakeSourceElementKind) return
        if (source.elementType != KtNodeTypes.DESTRUCTURING_DECLARATION) return
        val needToReport =
            when (val initializer = declaration.initializer) {
                null -> true
                is FirErrorExpression -> (initializer.diagnostic as? ConeSimpleDiagnostic)?.kind == DiagnosticKind.Syntax
                else -> false
            }
        if (needToReport) {
            reporter.report(source, FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION)
        }
    }
}
