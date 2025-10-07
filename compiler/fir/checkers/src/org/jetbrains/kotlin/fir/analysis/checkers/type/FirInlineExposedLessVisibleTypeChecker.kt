/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirInlineDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

object FirInlineExposedLessVisibleTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val inlineFunctionBodyContext = context.inlineFunctionBodyContext ?: return
        check(typeRef.coneType, typeRef.source, inlineFunctionBodyContext)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun check(
        coneType: ConeKotlinType,
        source: KtSourceElement?,
        inlineFunctionBodyContext: FirInlineDeclarationChecker.InlineFunctionBodyContext,
    ) {
        if (context.callsOrAssignments.any { it is FirAnnotation }) return
        val fullyExpandedType = coneType.fullyExpandedType()
        val classLikeSymbol = fullyExpandedType.toClassLikeSymbol() ?: return

        val symbolEffectiveVisibility = inlineFunctionBodyContext.lessVisibleVisibilityOrNull(classLikeSymbol, ignoreLocal = true) ?: return

        reporter.reportOn(
            source,
            FirErrors.LESS_VISIBLE_TYPE_ACCESS_IN_INLINE,
            symbolEffectiveVisibility,
            fullyExpandedType,
            inlineFunctionBodyContext.inlineFunEffectiveVisibility
        )
    }
}