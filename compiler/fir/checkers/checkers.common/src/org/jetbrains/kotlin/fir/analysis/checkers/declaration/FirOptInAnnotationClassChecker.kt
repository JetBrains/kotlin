/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.getTargetAnnotation
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getRetention
import org.jetbrains.kotlin.fir.declarations.getRetentionAnnotation
import org.jetbrains.kotlin.resolve.checkers.OptInDescription
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirOptInAnnotationClassChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (declaration.classKind != ClassKind.ANNOTATION_CLASS) return
        val session = context.session
        if (declaration.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID, session) == null) return
        if (declaration.getRetention(session) == AnnotationRetention.SOURCE) {
            val retention = declaration.getRetentionAnnotation(session)
            reporter.reportOn(retention?.source, FirErrors.OPT_IN_MARKER_WITH_WRONG_RETENTION)
        }
        // value parameter is a default target but cannot accept opt in markers
        val targetAnnotation = declaration.getTargetAnnotation(session)
        val allowedTargets = declaration.getAllowedAnnotationTargets(session)
        val targetsToCheck = if (targetAnnotation == null) {
            allowedTargets - KotlinTarget.VALUE_PARAMETER
        } else {
            allowedTargets
        }

        val wrongTargets = targetsToCheck.intersect(OptInDescription.WRONG_TARGETS_FOR_MARKER)
        if (wrongTargets.isNotEmpty()) {
            val valueParameterOnly = wrongTargets.singleOrNull() == KotlinTarget.VALUE_PARAMETER
            val diagnostic = when {
                !valueParameterOnly || LanguageFeature.ForbidValueParameterTargetForOptInMarkers.isEnabled() ->
                    FirErrors.OPT_IN_MARKER_WITH_WRONG_TARGET
                else -> FirErrors.OPT_IN_MARKER_WITH_WRONG_TARGET_WARNING
            }
            reporter.reportOn(
                targetAnnotation?.source,
                diagnostic,
                wrongTargets.joinToString(transform = KotlinTarget::description)
            )
        }
    }
}
