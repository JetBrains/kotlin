/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getDefaultValueForParameter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupportedDefaultValueInFunctionType

object FirUnsupportedDefaultValueInFunctionTypeParameterChecker : FirFunctionalTypeParameterSyntaxChecker() {
    override fun checkPsiOrLightTree(
        element: FirFunctionTypeParameter,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val defaultValue = source.getDefaultValueForParameter(context.session) ?: return
        report(defaultValue, reporter, context)
    }

    private fun report(defaultValueSource: KtSourceElement, reporter: DiagnosticReporter, context: CheckerContext) {
        val diagnostic = ConeUnsupportedDefaultValueInFunctionType(defaultValueSource)
        reporter.reportOn(diagnostic.source, FirErrors.UNSUPPORTED, diagnostic.reason, context)
    }
}