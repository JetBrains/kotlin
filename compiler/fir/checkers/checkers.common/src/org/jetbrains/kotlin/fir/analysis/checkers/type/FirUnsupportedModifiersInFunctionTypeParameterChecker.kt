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
import org.jetbrains.kotlin.fir.analysis.checkers.extractModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.syntax.withSyntax

object FirUnsupportedModifiersInFunctionTypeParameterChecker : FirFunctionalTypeParameterSyntaxChecker() {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsiOrLightTree(
        element: FirFunctionTypeParameter,
        source: KtSourceElement,
    ) {
        val modifiersList = source.extractModifierList()

        if (modifiersList != null) {
            for (modifier in modifiersList.modifiers) {
                reporter.reportOn(
                    modifier.source,
                    FirErrors.UNSUPPORTED,
                    "Function type parameters cannot have modifiers."
                )
            }

            for (annSource in modifiersList.annotationSources) {
                reporter.reportOn(
                    annSource,
                    FirErrors.UNSUPPORTED,
                    "Function type parameters cannot have annotations."
                )
            }
        }

        withSyntax { source.findValOrVarKeywordSource() }?.let {
            reporter.reportOn(
                it,
                FirErrors.UNSUPPORTED,
                "Function type parameters cannot be 'val' or 'var'."
            )
        }
    }
}
