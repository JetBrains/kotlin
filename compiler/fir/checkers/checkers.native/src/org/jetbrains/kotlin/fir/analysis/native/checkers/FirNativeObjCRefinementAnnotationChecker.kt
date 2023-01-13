/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeObjCRefinementChecker.hidesFromObjCClassId
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeObjCRefinementChecker.refinesInSwiftClassId
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation

object FirNativeObjCRefinementAnnotationChecker : FirRegularClassChecker() {

    private val supportedTargets = arrayOf(KotlinTarget.FUNCTION, KotlinTarget.PROPERTY)

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.classKind != ClassKind.ANNOTATION_CLASS) return
        val session = context.session
        val (objCAnnotation, swiftAnnotation) = declaration.findMetaAnnotations(session)
        if (objCAnnotation == null && swiftAnnotation == null) return
        if (objCAnnotation != null && swiftAnnotation != null) {
            reporter.reportOn(
                swiftAnnotation.source,
                FirNativeErrors.REDUNDANT_SWIFT_REFINEMENT,
                context
            )
        }
        val targets = declaration.getAllowedAnnotationTargets(session)
        val unsupportedTargets = targets - supportedTargets
        if (unsupportedTargets.isNotEmpty()) {
            objCAnnotation?.let { reporter.reportOn(it.source, FirNativeErrors.INVALID_OBJC_REFINEMENT_TARGETS, context) }
            swiftAnnotation?.let { reporter.reportOn(it.source, FirNativeErrors.INVALID_OBJC_REFINEMENT_TARGETS, context) }
        }
    }

    private fun FirRegularClass.findMetaAnnotations(session: FirSession): Pair<FirAnnotation?, FirAnnotation?> {
        var objCAnnotation: FirAnnotation? = null
        var swiftAnnotation: FirAnnotation? = null
        for (annotation in annotations) {
            when (annotation.toAnnotationClassId(session)) {
                hidesFromObjCClassId -> objCAnnotation = annotation
                refinesInSwiftClassId -> swiftAnnotation = annotation
            }
            if (objCAnnotation != null && swiftAnnotation != null) break
        }
        return objCAnnotation to swiftAnnotation
    }
}
