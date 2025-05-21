/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isExplicitParentOfResolvedQualifier
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirInlineExposedLessVisibleTypeChecker
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.defaultType

object FirInlineBodyResolvedQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        val inlineFunctionBodyContext = context.inlineFunctionBodyContext ?: return
        if (expression.isExplicitParentOfResolvedQualifier(context)) return
        val accessedClass = expression.symbol ?: return
        val source = expression.source ?: return

        if (accessedClass.isCompanion) {
            inlineFunctionBodyContext.checkAccessedDeclaration(
                source, expression, accessedClass,
            )
        }

        FirInlineExposedLessVisibleTypeChecker.check(accessedClass.defaultType(), source, inlineFunctionBodyContext)
    }
}
