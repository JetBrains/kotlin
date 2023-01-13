/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.name.JvmNames.VOLATILE_ANNOTATION_CLASS_ID

object FirVolatileAnnotationChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.source?.kind != KtRealSourceElementKind) return

        val fieldAnnotation = declaration.getAnnotationByClassId(VOLATILE_ANNOTATION_CLASS_ID, context.session)
        if (fieldAnnotation != null && !declaration.isVar) {
            reporter.reportOn(fieldAnnotation.source, FirJvmErrors.VOLATILE_ON_VALUE, context)
        }

        val delegateAnnotation = declaration.delegateFieldSymbol?.getAnnotationByClassId(VOLATILE_ANNOTATION_CLASS_ID, context.session)
        if (delegateAnnotation != null) {
            reporter.reportOn(delegateAnnotation.source, FirJvmErrors.VOLATILE_ON_DELEGATE, context)
        }
    }

}