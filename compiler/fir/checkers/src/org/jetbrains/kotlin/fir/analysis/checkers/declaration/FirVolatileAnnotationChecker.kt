/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassIds

object FirVolatileAnnotationChecker : FirPropertyChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.source?.kind != KtRealSourceElementKind) return

        val volatileAnnotations = context.session.annotationPlatformSupport.volatileAnnotations
        val fieldAnnotation = declaration.backingField?.annotations?.getAnnotationByClassIds(volatileAnnotations, context.session)
            ?: return

        if (!declaration.isVar) {
            reporter.reportOn(fieldAnnotation.source, FirErrors.VOLATILE_ON_VALUE, context)
        }

        if (declaration.delegateFieldSymbol != null) {
            reporter.reportOn(fieldAnnotation.source, FirErrors.VOLATILE_ON_DELEGATE, context)
        }
    }
}
