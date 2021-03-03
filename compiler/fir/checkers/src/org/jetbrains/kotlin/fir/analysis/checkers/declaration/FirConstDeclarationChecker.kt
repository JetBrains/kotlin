/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.lexer.KtTokens

object FirConstDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirVariable<*>) return
        val source = declaration.source ?: return
        if (source.kind is FirFakeSourceElementKind) return
        val modifierList = with(FirModifierList) { source.getModifierList() }
        val constModifier = modifierList?.modifiers?.firstOrNull { it.token == KtTokens.CONST_KEYWORD } ?: return

        if (declaration.isVar) {
            reporter.reportOn(constModifier.source, FirErrors.WRONG_MODIFIER_TARGET, constModifier.token, "vars", context)
        }

        // TODO: Implement checkers for these errors (see ConstModifierChecker in FE1.0):
        // - CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT
        // - CONST_VAL_WITH_DELEGATE
        // - CONST_VAL_WITH_GETTER
        // - TYPE_CANT_BE_USED_FOR_CONST_VAL
        // - CONST_VAL_WITHOUT_INITIALIZER
        // - CONST_VAL_WITH_NON_CONST_INITIALIZER
    }
}
