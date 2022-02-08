/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.typeParametersList
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.psi.KtFunction

object FirAnonymousFunctionSyntaxChecker : FirDeclarationSyntaxChecker<FirAnonymousFunction, KtFunction>() {
    override fun checkPsi(
        element: FirAnonymousFunction,
        source: KtPsiSourceElement,
        psi: KtFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (psi.typeParameterList != null) {
            reporter.reportOn(
                source,
                FirErrors.TYPE_PARAMETERS_NOT_ALLOWED,
                context
            )
        }
    }

    override fun checkLightTree(
        element: FirAnonymousFunction,
        source: KtLightSourceElement,
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
