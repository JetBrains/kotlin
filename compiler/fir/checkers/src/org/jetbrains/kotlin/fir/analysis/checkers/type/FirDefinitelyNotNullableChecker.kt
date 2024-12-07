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
import org.jetbrains.kotlin.fir.types.FirIntersectionTypeRef
import org.jetbrains.kotlin.fir.types.isLeftValidForDefinitelyNotNullable
import org.jetbrains.kotlin.fir.types.isRightValidForDefinitelyNotNullable

object FirDefinitelyNotNullableChecker : FirIntersectionTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirIntersectionTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef.isMarkedNullable) {
            reporter.reportOn(typeRef.source, FirErrors.NULLABLE_ON_DEFINITELY_NOT_NULLABLE, context)
        }

        if (!typeRef.isLeftValidForDefinitelyNotNullable(context.session)) {
            reporter.reportOn(typeRef.leftType.source, FirErrors.INCORRECT_LEFT_COMPONENT_OF_INTERSECTION, context)
        }

        if (!typeRef.isRightValidForDefinitelyNotNullable) {
            reporter.reportOn(typeRef.rightType.source, FirErrors.INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION, context)
        }
    }
}
