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

// See old FE's [DeclarationsChecker]
object FirTopLevelFunctionChecker : FirFileChecker() {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        for (topLevelDeclaration in declaration.declarations) {
            if (topLevelDeclaration is FirSimpleFunction) {
                checkFunction(topLevelDeclaration, reporter, context)
            }
        }
    }

    private fun checkFunction(function: FirSimpleFunction, reporter: DiagnosticReporter, context: CheckerContext) {
        val source = function.source ?: return
        if (source.kind is FirFakeSourceElementKind) return
        // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
        // So, our source of truth should be the full modifier list retrieved from the source.
        val modifierList = with(FirModifierList) { source.getModifierList() }
        if (modifierList?.modifiers?.any { it.token == KtTokens.ABSTRACT_KEYWORD } == true) return
        if (function.isExternal || modifierList?.modifiers?.any { it.token == KtTokens.EXTERNAL_KEYWORD } == true) return
        val isExpect = function.isExpect || modifierList?.modifiers?.any { it.token == KtTokens.EXPECT_KEYWORD } == true
        if (!function.hasBody && !isExpect) {
            reporter.reportOn(source, FirErrors.NON_MEMBER_FUNCTION_NO_BODY, function, context)
        }

        checkExpectDeclarationVisibilityAndBody(function, source, modifierList, reporter, context)
    }
}
