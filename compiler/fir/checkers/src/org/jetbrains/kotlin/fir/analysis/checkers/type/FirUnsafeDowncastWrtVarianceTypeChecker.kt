/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.unsafeDowncastWrtVariance

object FirUnsafeDowncastWrtVarianceTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        typeRef.coneType.attributes.unsafeDowncastWrtVariance?.let {
            reporter.reportOn(typeRef.source, FirErrors.UNSAFE_DOWNCAST_WRT_VARIANCE, typeRef.coneType, it.coneType, context)
        }
    }
}
