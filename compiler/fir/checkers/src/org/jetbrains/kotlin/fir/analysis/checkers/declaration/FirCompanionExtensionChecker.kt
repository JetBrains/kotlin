/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.lexer.KtTokens

object FirCompanionExtensionChecker : FirCallableDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        if (declaration.isCompanionExtension && LanguageFeature.CompanionBlocksAndExtensions.isDisabled()) {
            reporter.reportOn(
                declaration.getModifier(KtTokens.COMPANION_KEYWORD)?.source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.CompanionBlocksAndExtensions to context.languageVersionSettings
            )
        }
    }
}