/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.FirModifier
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.toVisibilityOrNull
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens

object FirBackingFieldModifiersChecker : FirBackingFieldChecker() {
    private val ALLOWED_MODIFIERS = setOf(
        KtTokens.PRIVATE_KEYWORD,
        KtTokens.INTERNAL_KEYWORD,
    )

    override fun check(declaration: FirBackingField, context: CheckerContext, reporter: DiagnosticReporter) {
        val modifiers = declaration.source
            ?.getModifierList()
            ?.modifiers
            ?: return

        modifiers.forEach {
            if (it.token in ALLOWED_MODIFIERS) {
                return@forEach
            }

            val diagnostic = reportInapplicable(it)
            reporter.reportOn(it.source, diagnostic, it.token, context)
        }
    }

    private fun reportInapplicable(modifier: FirModifier<*>): FirDiagnosticFactory1<KtModifierKeywordToken> {
        return if (modifier.token.toVisibilityOrNull() != null) {
            FirErrors.INAPPLICABLE_BACKING_FIELD_VISIBILITY
        } else {
            FirErrors.INAPPLICABLE_BACKING_FIELD_MODIFIER
        }
    }
}
