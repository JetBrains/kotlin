/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

object FirDslMarkerUseSiteChecker : FirAnnotationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirAnnotation) {
        val annotationClass = expression.annotationTypeRef.toRegularClassSymbol(context.session)?.fullyExpandedClass() ?: return
        if (!annotationClass.hasAnnotation(StandardClassIds.Annotations.DslMarker, context.session)) return

        val container = context.containingElements.asReversed().drop(1).firstIsInstanceOrNull<FirAnnotationContainer>() ?: return
        if (container !is FirClassLikeDeclaration && container !is FirTypeRef) {
            val actualTargets = getActualTargetList(container)
            val targetDescription = actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target"
            reporter.reportOn(expression.source, FirErrors.DSL_MARKER_APPLIED_TO_WRONG_TARGET, annotationClass, targetDescription)
        }
    }
}