/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.syntax.withSyntax

object FirUnsupportedModifiersInFunctionTypeParameterChecker : FirFunctionalTypeParameterSyntaxChecker() {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsiOrLightTree(
        element: FirFunctionTypeParameter,
        source: KtSourceElement,
    ) {
        checkModifiers(source)
        checkAnnotations(source)
        checkValOrVarKeyword(source)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkValOrVarKeyword(source: KtSourceElement) {
        val keyword = withSyntax { source.findValOrVarKeywordSource() } ?: return

        reporter.reportOn(
            keyword,
            FirErrors.UNSUPPORTED,
            "Function type parameters cannot be 'val' or 'var'."
        )
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkModifiers(source: KtSourceElement): Boolean {
        val modifiersList = source.getModifierList() ?: return true
        for (modifier in modifiersList.modifiers) {
            reporter.reportOn(
                modifier.source,
                FirErrors.UNSUPPORTED,
                "Function type parameters cannot have modifiers."
            )
        }
        return false
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkAnnotations(source: KtSourceElement): Boolean {
        val commonModifiersList = source.getModifierList() ?: return true
        val annotationSources = commonModifiersList.annotationSources
        for (ann in annotationSources) {
            reporter.reportOn(
                ann,
                FirErrors.UNSUPPORTED,
                "Function type parameters cannot have annotations."
            )
        }
        return false
    }
}
