/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkRepeatedAnnotation
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.getDefaultUseSiteTarget
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDesugaredAssignmentValueReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType

object FirExpressionAnnotationChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        // Declarations are checked separately
        // See KT-58723 about annotations on non-expression statements
        if (expression is FirDeclaration ||
            expression is FirErrorExpression ||
            expression is FirBlock && expression.source?.kind == KtRealSourceElementKind
        ) return

        // To prevent double-reporting (we have also a call in this case)
        if (expression is FirVariableAssignment && expression.lValue is FirDesugaredAssignmentValueReferenceExpression) {
            return
        }

        val annotations = expression.annotations
        if (annotations.isEmpty()) return

        val annotationsMap = hashMapOf<ConeKotlinType, MutableList<AnnotationUseSiteTarget?>>()

        for (annotation in annotations) {
            val useSiteTarget = annotation.useSiteTarget ?: expression.getDefaultUseSiteTarget(annotation, context)
            val existingTargetsForAnnotation = annotationsMap.getOrPut(annotation.annotationTypeRef.coneType) { arrayListOf() }

            val allowedAnnotationTargets = annotation.getAllowedAnnotationTargets(context.session)
            if (KotlinTarget.EXPRESSION !in allowedAnnotationTargets) {
                reporter.reportOn(annotation.source, FirErrors.WRONG_ANNOTATION_TARGET, "expression", allowedAnnotationTargets, context)
            }

            checkRepeatedAnnotation(useSiteTarget, existingTargetsForAnnotation, annotation, context, reporter, annotation.source)

            existingTargetsForAnnotation.add(useSiteTarget)
        }
    }
}
