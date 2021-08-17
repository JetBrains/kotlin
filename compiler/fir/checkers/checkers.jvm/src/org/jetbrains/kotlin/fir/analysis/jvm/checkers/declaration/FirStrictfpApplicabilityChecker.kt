/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByFqName
import org.jetbrains.kotlin.name.FqName

object FirStrictfpApplicabilityChecker : FirClassChecker() {
    private val STRICTFP_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Strictfp")

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByFqName(STRICTFP_ANNOTATION_FQ_NAME) ?: return
        reporter.reportOn(annotation.source, FirJvmErrors.STRICTFP_ON_CLASS, context)
    }
}