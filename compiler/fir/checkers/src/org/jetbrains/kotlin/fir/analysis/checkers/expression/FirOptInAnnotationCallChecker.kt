/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractClassFromArgument
import org.jetbrains.kotlin.fir.analysis.checkers.extractClassesFromArgument
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.utils.isFun
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_ANNOTATION_CLASS

object FirOptInAnnotationCallChecker : FirAnnotationCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val lookupTag = expression.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag ?: return
        val classId = lookupTag.classId
        val isRequiresOptIn = classId == OptInNames.REQUIRES_OPT_IN_CLASS_ID
        val isOptIn = classId == OptInNames.OPT_IN_CLASS_ID
        val isSubclassOptIn = classId == OptInNames.SUBCLASS_OPT_IN_REQUIRED_CLASS_ID
        if (isRequiresOptIn || isOptIn) {
            checkOptInIsEnabled(expression.source, context, reporter)
            if (isOptIn) {
                val arguments = expression.arguments
                if (arguments.isEmpty()) {
                    reporter.reportOn(expression.source, FirErrors.OPT_IN_WITHOUT_ARGUMENTS, context)
                } else {
                    val annotationClasses = expression.findArgumentByName(OPT_IN_ANNOTATION_CLASS)
                    for (classSymbol in annotationClasses?.extractClassesFromArgument(context.session).orEmpty()) {
                        checkOptInArgumentIsMarker(classSymbol, expression.source, reporter, context)
                    }
                }
            }
        } else if (isSubclassOptIn) {
            val declaration = context.containingDeclarations.lastOrNull() as? FirClass
            if (declaration != null) {
                val kind = declaration.classKind
                val classKindRepresentation = kind.representation
                if (kind == ClassKind.ENUM_CLASS || kind == ClassKind.OBJECT || kind == ClassKind.ANNOTATION_CLASS) {
                    reporter.reportOn(expression.source, FirErrors.SUBCLASS_OPT_IN_INAPPLICABLE, classKindRepresentation, context)
                    return
                }
                val modality = declaration.modality()
                if (modality == Modality.FINAL || modality == Modality.SEALED) {
                    reporter.reportOn(
                        expression.source,
                        FirErrors.SUBCLASS_OPT_IN_INAPPLICABLE,
                        "${modality.name.lowercase()} $classKindRepresentation",
                        context,
                    )
                    return
                }
                if (declaration.isFun) {
                    reporter.reportOn(expression.source, FirErrors.SUBCLASS_OPT_IN_INAPPLICABLE, "fun interface", context)
                    return
                }
                if (declaration.isLocal) {
                    reporter.reportOn(expression.source, FirErrors.SUBCLASS_OPT_IN_INAPPLICABLE, "local $classKindRepresentation", context)
                    return
                }
            }
            val classSymbol = expression.findArgumentByName(OPT_IN_ANNOTATION_CLASS)?.extractClassFromArgument(context.session) ?: return
            checkOptInArgumentIsMarker(classSymbol, expression.source, reporter, context)
        }
    }

    private val ClassKind.representation: String
        get() = when (this) {
            ClassKind.ENUM_ENTRY -> "enum entry"
            else -> codeRepresentation!!
        }

    private fun checkOptInIsEnabled(
        element: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val languageVersionSettings = context.session.languageVersionSettings
        val optInFqNames = languageVersionSettings.getFlag(AnalysisFlags.optIn)
        if (!languageVersionSettings.supportsFeature(LanguageFeature.OptInRelease) &&
            OptInNames.REQUIRES_OPT_IN_FQ_NAME.asString() !in optInFqNames
        ) {
            reporter.reportOn(element, FirErrors.OPT_IN_IS_NOT_ENABLED, context)
        }
    }

    private fun checkOptInArgumentIsMarker(
        classSymbol: FirRegularClassSymbol,
        source: KtSourceElement?,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        with(FirOptInUsageBaseChecker) {
            if (classSymbol.loadExperimentalityForMarkerAnnotation(context.session) == null) {
                reporter.reportOn(
                    source,
                    FirErrors.OPT_IN_ARGUMENT_IS_NOT_MARKER,
                    classSymbol.classId,
                    context
                )
            }
        }
    }
}
