/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByFqName
import org.jetbrains.kotlin.resolve.checkers.Experimentality
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirOptInAnnotationClassChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.classKind != ClassKind.ANNOTATION_CLASS) return
        if (declaration.getAnnotationByFqName(OptInNames.REQUIRES_OPT_IN_FQ_NAME) == null) return
        if (declaration.getRetention() == AnnotationRetention.SOURCE) {
            val target = declaration.getRetentionAnnotation()
            reporter.reportOn(target?.source, FirErrors.EXPERIMENTAL_ANNOTATION_WITH_WRONG_RETENTION, context)

        }
        val wrongTargets = declaration.getAllowedAnnotationTargets().intersect(Experimentality.WRONG_TARGETS_FOR_MARKER)
        if (wrongTargets.isNotEmpty()) {
            val target = declaration.getTargetAnnotation()
            reporter.reportOn(
                target?.source,
                FirErrors.EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET,
                wrongTargets.joinToString(transform = KotlinTarget::description),
                context
            )
        }
    }
}