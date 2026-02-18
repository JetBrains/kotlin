/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol

object FirContextualPropertyWithBackingFieldChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!LanguageFeature.ContextReceivers.isEnabled() &&
            !LanguageFeature.ContextParameters.isEnabled()
        ) {
            return
        }
        if (declaration.contextParameters.isEmpty()) return

        if (declaration.hasBackingField && !declaration.isLateInit && declaration.symbol is FirRegularPropertySymbol) {
            reporter.reportOn(
                declaration.source,
                FirErrors.CONTEXT_PARAMETERS_WITH_BACKING_FIELD
            )
        }
    }
}
