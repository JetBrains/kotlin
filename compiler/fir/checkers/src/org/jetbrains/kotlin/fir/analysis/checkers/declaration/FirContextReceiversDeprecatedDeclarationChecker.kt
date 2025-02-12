/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirContextParametersLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.isLegacyContextReceiver

object FirContextReceiversDeprecatedDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) return

        if (declaration is FirCallableDeclaration &&
            // Skip the lambdas. They don't have `context` explicitly written => `context` is written somewhere else.
            // Plus, I'd not say that lambdas are declarations. They are rather values
            declaration !is FirAnonymousFunction &&
            declaration.contextParameters.onlyLegacyContextReceivers()
        ) {
            if (declaration is FirConstructor && declaration !is FirPrimaryConstructor) {
                reporter.reportOn(declaration.source, FirErrors.CONTEXT_CLASS_OR_CONSTRUCTOR, context)
            } else {
                val message = FirContextParametersLanguageVersionSettingsChecker.getMessage(context.languageVersionSettings)
                reporter.reportOn(declaration.source, FirErrors.CONTEXT_RECEIVERS_DEPRECATED, message, context)
            }
        }
        if (declaration is FirRegularClass && declaration.contextParameters.onlyLegacyContextReceivers()) {
            reporter.reportOn(declaration.source, FirErrors.CONTEXT_CLASS_OR_CONSTRUCTOR, context)
        }
    }

    private fun List<FirValueParameter>.onlyLegacyContextReceivers(): Boolean {
        return isNotEmpty() && all { it.isLegacyContextReceiver() }
    }
}
