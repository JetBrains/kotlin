/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol

object FirActualTypeAliasChecker : FirTypeAliasChecker() {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isActual) return

        checkActualTypeAliasNotToClass(declaration, context, reporter)
    }

    private fun checkActualTypeAliasNotToClass(
        declaration: FirTypeAlias,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (declaration.expandedTypeRef.toClassLikeSymbol(context.session) is FirTypeAliasSymbol) {
            reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_NOT_TO_CLASS, context)
        }
    }
}