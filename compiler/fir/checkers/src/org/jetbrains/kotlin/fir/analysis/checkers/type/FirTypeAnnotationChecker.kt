/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.isBuiltinFunctionalType
import org.jetbrains.kotlin.name.StandardClassIds

object FirTypeAnnotationChecker : FirTypeRefChecker() {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef !is FirResolvedTypeRef) return

        for (annotation in typeRef.annotations) {
            withSuppressedDiagnostics(annotation, context) {
                val annotationTargets = annotation.getAllowedAnnotationTargets(context.session)
                if (KotlinTarget.TYPE !in annotationTargets) {
                    val useSiteTarget = annotation.useSiteTarget
                    if (useSiteTarget == null || KotlinTarget.USE_SITE_MAPPING[useSiteTarget] !in annotationTargets) {
                        reporter.reportOn(annotation.source, FirErrors.WRONG_ANNOTATION_TARGET, "type usage", it)
                    }
                }
            }
            if (annotation.classId == StandardClassIds.Annotations.ExtensionFunctionType) {
                if (!typeRef.type.isBuiltinFunctionalType(context.session)) {
                    if (context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidExtensionFunctionTypeOnNonFunctionTypes)) {
                        reporter.reportOn(annotation.source, FirErrors.WRONG_EXTENSION_FUNCTION_TYPE, context)
                    } else {
                        reporter.reportOn(annotation.source, FirErrors.WRONG_EXTENSION_FUNCTION_TYPE_WARNING, context)
                    }

                } else if (typeRef.type.typeArguments.size <= 1) {
                    reporter.reportOn(annotation.source, FirErrors.WRONG_EXTENSION_FUNCTION_TYPE, context)
                }
            }
        }
    }
}
