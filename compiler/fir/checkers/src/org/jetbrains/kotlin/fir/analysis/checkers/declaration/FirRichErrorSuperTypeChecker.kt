/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isRichError
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.StandardClassIds

object FirRichErrorSuperTypeChecker : FirClassChecker(Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration is FirRegularClass && declaration.isRichError) return

        for (superTypeRef in declaration.superTypeRefs) {
            if (superTypeRef.coneType.classId == StandardClassIds.RichError) {
                reporter.reportOn(
                    superTypeRef.source,
                    FirErrors.NON_ERROR_CLASS_EXTENDS_RICH_ERROR,
                )
            }
        }
    }
}
