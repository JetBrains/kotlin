/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirWarnOnErasureUnconstrainedByChecker : FirTypeParameterChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirTypeParameter) {
        val annotation = declaration.symbol.getAnnotationByClassId(
            StandardClassIds.Annotations.WarnOnErasureUnconstrainedBy, context.session
        ) ?: return
        val argument = annotation.findArgumentByName(Name.identifier("receiverTypeArg")) ?: return
        for (element in argument.unwrapAndFlattenArgument(flattenArrays = true)) {
            val value = ((element as? FirLiteralExpression)?.value as? Number)?.toInt() ?: continue
            if (value < 0) {
                reporter.reportOn(element.source, FirErrors.WARN_ON_ERASURE_NEGATIVE_RECEIVER_TYPE_ARG)
            }
        }
    }
}
