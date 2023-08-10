/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.getRetention
import org.jetbrains.kotlin.fir.declarations.getRetentionAnnotation
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.resolve.checkers.OptInDescription
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirOptInAnnotationClassChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.classKind != ClassKind.ANNOTATION_CLASS) return
        val session = context.session
        if (declaration.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, session) == null) return
        if (declaration.getRetention(session) == AnnotationRetention.SOURCE) {
            val target = declaration.getRetentionAnnotation(session)
            reporter.reportOn(target?.source, FirErrors.OPT_IN_MARKER_WITH_WRONG_RETENTION, context)

        }
        val wrongTargets = declaration.getAllowedAnnotationTargets(session).intersect(OptInDescription.WRONG_TARGETS_FOR_MARKER)
        if (wrongTargets.isNotEmpty()) {
            val target = declaration.getTargetAnnotation(session)
            reporter.reportOn(
                target?.source,
                FirErrors.OPT_IN_MARKER_WITH_WRONG_TARGET,
                wrongTargets.joinToString(transform = KotlinTarget::description),
                context
            )
        }
    }
}