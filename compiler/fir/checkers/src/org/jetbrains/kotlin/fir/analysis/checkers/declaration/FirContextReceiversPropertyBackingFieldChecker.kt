/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField

object FirContextReceiversPropertyBackingFieldChecker : FirPropertyChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) return
        if (declaration.contextReceivers.isEmpty()) return

        if (declaration.hasBackingField) {
            reporter.reportOn(
                declaration.initializer?.source,
                FirErrors.CONTEXT_RECEIVERS_WITH_BACKING_FIELD,
                context
            )
        }
    }
}
