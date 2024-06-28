/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.name.StandardClassIds

object FirTypeAnnotationChecker : FirTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef !is FirResolvedTypeRef) return

        for (annotation in typeRef.annotations) {
            if (annotation.source == null) continue
            val useSiteTarget = annotation.useSiteTarget
            val annotationTargets = annotation.getAllowedAnnotationTargets(context.session)

            // Annotations like `@receiver:` go
            // into FirReceiverParameter, not FirTypeRef
            if (useSiteTarget != null) {
                reporter.reportOn(
                    annotation.source, FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET,
                    "type usage", useSiteTarget.renderName, context
                )
            } else if (KotlinTarget.TYPE !in annotationTargets) {
                reporter.reportOn(annotation.source, FirErrors.WRONG_ANNOTATION_TARGET, "type usage", context)
            }
            if (annotation.toAnnotationClassId(context.session) == StandardClassIds.Annotations.ExtensionFunctionType) {
                if (!typeRef.type.isSomeFunctionType(context.session)) {
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
