/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractClassesAndSourcesFromArgument
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.utils.isFun
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_ANNOTATION_CLASS
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_CLASS_ID
import org.jetbrains.kotlin.resolve.checkers.OptInNames.REQUIRES_OPT_IN_CLASS_ID
import org.jetbrains.kotlin.resolve.checkers.OptInNames.SUBCLASS_OPT_IN_REQUIRED_CLASS_ID

object FirOptInAnnotationCallChecker : FirAnnotationCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirAnnotationCall) {
        val lookupTag = expression.annotationTypeRef.coneType.classLikeLookupTagIfAny ?: return
        val classId = lookupTag.classId
        val isRequiresOptIn = classId == REQUIRES_OPT_IN_CLASS_ID
        val isOptIn = classId == OPT_IN_CLASS_ID
        val isSubclassOptIn = classId == SUBCLASS_OPT_IN_REQUIRED_CLASS_ID
        if (isRequiresOptIn || isOptIn) {
            if (isOptIn) {
                val arguments = expression.arguments
                if (arguments.isEmpty()) {
                    reporter.reportOn(expression.source, FirErrors.OPT_IN_WITHOUT_ARGUMENTS)
                } else {
                    for ((classSymbol, source) in expression.findArgumentByName(OPT_IN_ANNOTATION_CLASS)
                        ?.extractClassesAndSourcesFromArgument(context.session).orEmpty()) {
                        checkOptInArgumentIsMarker(classSymbol, classId, source)
                    }
                }
            }
        } else if (isSubclassOptIn) {
            val declaration = context.containingDeclarations.lastOrNull() as? FirClassSymbol
            if (declaration != null) {
                val (isSubclassOptInApplicable, message) = getSubclassOptInApplicabilityAndMessage(declaration)
                if (!isSubclassOptInApplicable && message != null) {
                    reporter.reportOn(expression.source, FirErrors.SUBCLASS_OPT_IN_INAPPLICABLE, message)
                    return
                }
            }

            val classSymbols = expression.findArgumentByName(OPT_IN_ANNOTATION_CLASS)?.extractClassesAndSourcesFromArgument(context.session).orEmpty()

            classSymbols.forEach { (classSymbol, source) ->
                checkOptInArgumentIsMarker(classSymbol, classId, source)
            }
        }
    }

    fun getSubclassOptInApplicabilityAndMessage(classSymbol: FirClassSymbol<*>): Pair<Boolean, String?> {
        val kind = classSymbol.classKind
        val classKindRepresentation = kind.representation
        if (kind == ClassKind.ENUM_CLASS || kind == ClassKind.OBJECT || kind == ClassKind.ANNOTATION_CLASS) {
            return false to classKindRepresentation
        }
        val modality = classSymbol.modality()
        if (modality == Modality.FINAL || modality == Modality.SEALED) {
            return false to "${modality.name.lowercase()} $classKindRepresentation"
        }
        if (classSymbol.isFun) {
            return false to "fun interface"
        }
        if (classSymbol.isLocal) {
            return false to "local $classKindRepresentation"
        }
        return true to null
    }


    private val ClassKind.representation: String
        get() = when (this) {
            ClassKind.ENUM_ENTRY -> "enum entry"
            else -> codeRepresentation!!
        }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkOptInArgumentIsMarker(
        classSymbol: FirRegularClassSymbol,
        annotationClassId: ClassId,
        source: KtSourceElement?,
    ) {
        with(FirOptInUsageBaseChecker) {
            if (classSymbol.loadExperimentalityForMarkerAnnotation(context.session) == null) {
                val diagnostic = when (annotationClassId) {
                    OPT_IN_CLASS_ID -> FirErrors.OPT_IN_ARGUMENT_IS_NOT_MARKER
                    SUBCLASS_OPT_IN_REQUIRED_CLASS_ID -> FirErrors.SUBCLASS_OPT_IN_ARGUMENT_IS_NOT_MARKER
                    else -> return
                }
                reporter.reportOn(
                    source,
                    diagnostic,
                    classSymbol.classId
                )
            }
        }
    }
}
