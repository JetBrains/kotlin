/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.RequireKotlinConstants

object FirAnnotationChecker : FirAnnotationCallChecker() {
    private val deprecatedSinceKotlinClassId = ClassId.fromString("kotlin/DeprecatedSinceKotlin")

    private val annotationClassIdsWithVersion = setOf(
        ClassId.fromString("kotlin/internal/RequireKotlin"),
        ClassId.fromString("kotlin/SinceKotlin"),
        deprecatedSinceKotlinClassId
    )

    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val classId = ((expression.annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeClassLikeType)?.lookupTag?.classId
        if (annotationClassIdsWithVersion.contains(classId)) {
            for (argIndex in expression.argumentList.arguments.withIndex()) {
                if (argIndex.index > 0 && classId != deprecatedSinceKotlinClassId) {
                    continue
                }

                val arg = argIndex.value
                val argSource = arg.source
                if (argSource != null) {
                    val constExpression = (arg as? FirConstExpression<*>)
                        ?: ((arg as? FirNamedArgumentExpression)?.expression as? FirConstExpression<*>)
                    if ((constExpression?.value as? String)?.matches(RequireKotlinConstants.VERSION_REGEX) == false) {
                        reporter.reportOn(argSource, FirErrors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE, context)
                    }
                }
            }
        }

        val args = expression.argumentList.arguments
        for (arg in args) {
            for (ann in arg.unwrapArgument().annotations) {
                reporter.reportOn(ann.source, FirErrors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT, context)
            }
        }
    }
}