/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractClassesFromArgument
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirOptInAnnotationCallChecker : FirAnnotationCallChecker() {
    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val lookupTag = expression.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag ?: return
        val classId = lookupTag.classId
        val isRequiresOptIn = classId == OptInNames.REQUIRES_OPT_IN_CLASS_ID
        val isOptIn = classId == OptInNames.OPT_IN_CLASS_ID
        if (isRequiresOptIn || isOptIn) {
            checkOptInIsEnabled(expression.source, context, reporter)
            if (isOptIn) {
                val arguments = expression.arguments
                if (arguments.isEmpty()) {
                    reporter.reportOn(expression.source, FirErrors.OPT_IN_WITHOUT_ARGUMENTS, context)
                } else {
                    val annotationClasses = expression.findArgumentByName(OptInNames.USE_EXPERIMENTAL_ANNOTATION_CLASS)
                    for (classSymbol in annotationClasses?.extractClassesFromArgument().orEmpty()) {
                        with(FirOptInUsageBaseChecker) {
                            if (classSymbol.loadExperimentalityForMarkerAnnotation() == null) {
                                reporter.reportOn(
                                    expression.source,
                                    FirErrors.OPT_IN_ARGUMENT_IS_NOT_MARKER,
                                    classSymbol.classId.asSingleFqName(),
                                    context
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkOptInIsEnabled(
        element: FirSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val languageVersionSettings = context.session.languageVersionSettings
        val useExperimentalFqNames = languageVersionSettings.getFlag(AnalysisFlags.optIn)
        if (!languageVersionSettings.supportsFeature(LanguageFeature.OptInRelease) &&
            OptInNames.REQUIRES_OPT_IN_FQ_NAME.asString() !in useExperimentalFqNames
        ) {
            reporter.reportOn(element, FirErrors.OPT_IN_IS_NOT_ENABLED, context)
        }
    }
}
