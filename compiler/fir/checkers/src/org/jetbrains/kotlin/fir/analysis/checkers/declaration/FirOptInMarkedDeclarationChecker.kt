/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationClassForOptInMarker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter

object FirOptInMarkedDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        for (annotation in declaration.annotations) {
            val annotationClass = annotation.getAnnotationClassForOptInMarker(context.session) ?: continue
            withSuppressedDiagnostics(annotation, context) { ctx ->
                val useSiteTarget = annotation.useSiteTarget
                if ((declaration is FirPropertyAccessor && declaration.isGetter) || useSiteTarget == PROPERTY_GETTER) {
                    reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "getter", ctx)
                }
                if (useSiteTarget == SETTER_PARAMETER ||
                    (useSiteTarget != PROPERTY && useSiteTarget != PROPERTY_SETTER && declaration is FirValueParameter &&
                            KotlinTarget.VALUE_PARAMETER in annotationClass.getAllowedAnnotationTargets())
                ) {
                    reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "parameter", ctx)
                }
                if (declaration is FirProperty && declaration.isLocal) {
                    reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "variable", ctx)
                }
                if (useSiteTarget == FIELD || useSiteTarget == PROPERTY_DELEGATE_FIELD) {
                    reporter.reportOn(annotation.source, FirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "field", ctx)
                }
            }
        }
    }
}
