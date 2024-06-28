/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.*

object FirDefinitelyNotNullableChecker : FirTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val intersection = (typeRef as? FirResolvedTypeRef)?.delegatedTypeRef as? FirIntersectionTypeRef ?: return

        if (intersection.isMarkedNullable) {
            reporter.reportOn(intersection.source, FirErrors.NULLABLE_ON_DEFINITELY_NOT_NULLABLE, context)
        }

        if (!intersection.isLeftValidForDefinitelyNotNullable(context.session)) {
            reporter.reportOn(intersection.leftType.source, FirErrors.INCORRECT_LEFT_COMPONENT_OF_INTERSECTION, context)
        }

        if (!intersection.isRightValidForDefinitelyNotNullable) {
            reporter.reportOn(intersection.rightType.source, FirErrors.INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION, context)
        }
    }
}
