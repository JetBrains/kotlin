/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByFqName
import org.jetbrains.kotlin.name.FqName

object FirVolatileAnnotationChecker : FirPropertyChecker() {

    private val VOLATILE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Volatile")

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.source?.kind != FirRealSourceElementKind) return

        val fieldAnnotation = declaration.backingFieldSymbol.getAnnotationByFqName(VOLATILE_ANNOTATION_FQ_NAME)
        if (fieldAnnotation != null && !declaration.isVar) {
            reporter.reportOn(fieldAnnotation.source, FirJvmErrors.VOLATILE_ON_VALUE, context)
        }
        
        val delegateAnnotation = declaration.delegateFieldSymbol?.getAnnotationByFqName(VOLATILE_ANNOTATION_FQ_NAME)
        if (delegateAnnotation != null) {
            reporter.reportOn(delegateAnnotation.source, FirJvmErrors.VOLATILE_ON_DELEGATE, context)
        }
    }

}