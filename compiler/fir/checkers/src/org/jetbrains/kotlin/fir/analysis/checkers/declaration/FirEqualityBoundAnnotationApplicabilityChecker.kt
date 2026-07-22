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
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirEqualityBoundAnnotationApplicabilityChecker : FirValueParameterChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirValueParameter) {
        declaration.annotations.forEach { annotation ->
            if (annotation.toAnnotationClassId(context.session) != StandardClassIds.Annotations.EqualityBound) return@forEach

            if (!declaration.containingDeclarationSymbol.isOperatorEquals()) {
                reporter.reportOn(
                    annotation.source,
                    FirErrors.UNSUPPORTED,
                    "'EqualityBound' annotation is only supported for parameters of 'equals' operator."
                )
            } else {
                annotation.requireFeatureSupport(LanguageFeature.StrictEquals)
            }
        }
    }

    private fun FirBasedSymbol<*>.isOperatorEquals(): Boolean {
        return this is FirNamedFunctionSymbol && name == OperatorNameConventions.EQUALS && isOperator
    }
}
