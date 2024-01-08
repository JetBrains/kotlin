/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds

object FirSuspendLimitationsChecker : FirFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isSuspend) {
            return
        }

        if (declaration.annotations.any { it.isKotlinTestAnnotation(context.session) }) {
            declaration.getModifier(KtTokens.SUSPEND_KEYWORD)?.let {
                reporter.reportOn(it.source, FirErrors.UNSUPPORTED_SUSPEND_TEST, context)
            }
        }
    }

    private fun FirAnnotation.isKotlinTestAnnotation(session: FirSession): Boolean {
        val nonExpandedType = annotationTypeRef.coneType as? ConeClassLikeType
        return nonExpandedType?.lookupTag?.classId == StandardClassIds.Annotations.Test
                || toAnnotationClassId(session) == StandardClassIds.Annotations.Test
    }
}


