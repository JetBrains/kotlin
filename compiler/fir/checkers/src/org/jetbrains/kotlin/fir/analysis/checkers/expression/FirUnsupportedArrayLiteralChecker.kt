/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirUnsupportedArrayLiteralChecker : FirArrayLiteralChecker(MppCheckerKind.Common) {
    override fun check(expression: FirArrayLiteral, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!isInsideAnnotationCall(context) &&
            (context.callsOrAssignments.isNotEmpty() || !isInsideAnnotationClass(context))
        ) {
            reporter.reportOn(
                expression.source,
                FirErrors.UNSUPPORTED,
                "Collection literals outside of annotations",
                context
            )
        }
    }

    private fun isInsideAnnotationCall(context: CheckerContext): Boolean = context.callsOrAssignments.asReversed().any {
        when (it) {
            is FirFunctionCall -> it.resolvedType.toRegularClassSymbol(context.session)?.classKind == ClassKind.ANNOTATION_CLASS
            is FirAnnotationCall -> true
            else -> false
        }
    }

    private fun isInsideAnnotationClass(context: CheckerContext): Boolean {
        for (declaration in context.containingDeclarations.asReversed()) {
            if (declaration is FirRegularClass) {
                if (declaration.isCompanion) {
                    continue
                }

                if (declaration.classKind == ClassKind.ANNOTATION_CLASS) {
                    return true
                }
            } else if (declaration is FirValueParameter || declaration is FirPrimaryConstructor) {
                continue
            }

            break
        }

        return false
    }
}
