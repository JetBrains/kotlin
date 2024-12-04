/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyAccessorChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.lexer.KtTokens

object FirWebCommonExternalPropertyAccessorChecker : FirPropertyAccessorChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirPropertyAccessor, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirDefaultPropertyAccessor && declaration.isDirectlyExternal()) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "property accessor", context)
        }
    }

    private fun FirPropertyAccessor.isDirectlyExternal(): Boolean {
        return hasModifier(KtTokens.EXTERNAL_KEYWORD)
    }
}
