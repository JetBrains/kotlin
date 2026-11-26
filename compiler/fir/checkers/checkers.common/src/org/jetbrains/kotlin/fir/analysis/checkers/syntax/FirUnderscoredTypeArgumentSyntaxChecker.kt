/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.syntax.withSyntax

object FirUnderscoredTypeArgumentSyntaxChecker : FirExpressionSyntaxChecker<FirFunctionCall>() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(
        element: FirFunctionCall,
        source: KtSourceElement,
    ) {
        if (element.typeArguments.isEmpty()) return

        withSyntax {
            for (typeProjection in element.typeArguments) {
                val typeArgumentSource = typeProjection.source ?: continue
                for (annotationSource in source.findUnderscoreTypeArgumentAnnotationSources(typeArgumentSource)) {
                    reporter.reportOn(annotationSource, FirErrors.UNSUPPORTED, "Underscore type arguments cannot be annotated.")
                }
            }
        }
    }
}
