/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.FirScriptCustomizationKind
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression

object FirForLoopStatementAssignmentChecker : FirLoopExpressionChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(expression: FirLoop, context: CheckerContext, reporter: DiagnosticReporter) {
        // Checks the pattern for desugared for loop.
        val parent = if (context.containingElements.size >= 2) context.containingElements[context.containingElements.size - 2] else return
        if (parent.source?.kind != KtFakeSourceElementKind.DesugaredForLoop) return

        val grandParent = if (context.containingElements.size >= 3)
            context.containingElements[context.containingElements.size - 3]
        else
            return

        if (// It is used as a statement
            grandParent is FirBlock
            // It is used as a single statement in the method body
            || (grandParent is FirReturnExpression && grandParent.source?.kind == KtFakeSourceElementKind.ImplicitReturn.FromLastStatement)
            // It is used in a kotlin script as a last statement
            || (grandParent is FirProperty && (grandParent.origin as? FirDeclarationOrigin.ScriptCustomization)?.kind == FirScriptCustomizationKind.RESULT_PROPERTY)
            // There was a fail before (for example, using two labels before the for loop)
            || (grandParent is FirErrorExpression)
        )
            return

        reporter.reportOn(expression.source, FirErrors.EXPRESSION_EXPECTED, context)
    }
}