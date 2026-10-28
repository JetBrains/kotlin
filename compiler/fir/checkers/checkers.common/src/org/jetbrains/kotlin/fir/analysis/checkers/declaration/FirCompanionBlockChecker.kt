/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.companionBlocks
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.lexer.KtTokens

object FirCompanionBlockChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val companionBlocks = declaration.companionBlocks ?: return
        val firstCompanionBlock = companionBlocks.validCompanionBlocks.first()

        if (LanguageFeature.CompanionBlocks.isDisabled()) {
            reporter.reportOn(
                firstCompanionBlock.companionModifierSource(),
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.CompanionBlocks to context.languageVersionSettings
            )
        }

        val errorKind = when {
            declaration.classKind == ClassKind.ENUM_ENTRY -> "enum entry"
            declaration is FirAnonymousObject -> "anonymous object"
            declaration.classKind == ClassKind.OBJECT -> "object"
            declaration.isLocal -> "local class"
            else -> null
        }

        if (errorKind != null) {
            reporter.reportOn(
                firstCompanionBlock.companionModifierSource(),
                FirErrors.ILLEGAL_COMPANION_BLOCK,
                errorKind,
            )
        }

        companionBlocks.nestedCompanionBlocks.forEach {
            reporter.reportOn(it.companionModifierSource(), FirErrors.COMPANION_BLOCK_NESTED)
        }
    }

    private fun KtSourceElement.companionModifierSource(): KtSourceElement? {
        return getChild(KtNodeTypes.MODIFIER_LIST)?.getChild(KtTokens.COMPANION_KEYWORD)
    }
}

