/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.firstCompanionBlock
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.lexer.KtTokens

object FirCompanionBlockChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val firstCompanionBlock = declaration.firstCompanionBlock ?: return
        if (LanguageFeature.CompanionBlocksAndExtensions.isDisabled()) {
            reporter.reportOn(
                firstCompanionBlock.getChild(KtNodeTypes.MODIFIER_LIST)?.getChild(KtTokens.COMPANION_KEYWORD),
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.CompanionBlocksAndExtensions to context.languageVersionSettings
            )
        }
    }
}

