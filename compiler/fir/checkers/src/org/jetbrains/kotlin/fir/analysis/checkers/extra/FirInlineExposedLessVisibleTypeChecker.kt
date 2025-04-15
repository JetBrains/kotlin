/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

object FirInlineExposedLessVisibleTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val inlineFunctionBodyContext = context.inlineFunctionBodyContext ?: return

        val fullyExpandedType = typeRef.coneType.fullyExpandedType(inlineFunctionBodyContext.session)
        val symbolEffectiveVisibility = fullyExpandedType.toClassLikeSymbol(inlineFunctionBodyContext.session)?.effectiveVisibility ?: return
        if (inlineFunctionBodyContext.isLessVisibleThanInlineFunction(symbolEffectiveVisibility)) {
            reporter.reportOn(
                typeRef.source,
                FirErrors.LESS_VISIBLE_TYPE_ACCESS_IN_INLINE,
                symbolEffectiveVisibility,
                fullyExpandedType,
                inlineFunctionBodyContext.inlineFunEffectiveVisibility
            )
        }
    }
}
