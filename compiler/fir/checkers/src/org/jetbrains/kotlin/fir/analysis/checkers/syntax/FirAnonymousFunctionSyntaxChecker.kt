/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.typeParametersList
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.psi.KtFunction

object FirAnonymousFunctionSyntaxChecker : FirExpressionSyntaxChecker<FirAnonymousFunction, KtFunction>() {
    override fun checkPsi(
        element: FirAnonymousFunction,
        source: FirPsiSourceElement<KtFunction>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (source.psi.typeParameterList != null) {
            reporter.reportOn(
                source,
                FirErrors.TYPE_PARAMETERS_NOT_ALLOWED,
                context
            )
        }
    }

    override fun checkLightTree(
        element: FirAnonymousFunction,
        source: FirSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        source.treeStructure.typeParametersList(source.lighterASTNode)?.let { _ ->
            reporter.reportOn(
                source,
                FirErrors.TYPE_PARAMETERS_NOT_ALLOWED,
                context
            )
        }
    }
}
