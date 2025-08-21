/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.typeParametersList
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.isLocal
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty

// KtExpression is the appropriate PsiElement type; local variables are used in increments/decrements of dot-qualified expressions.
object FirLocalVariableTypeParametersSyntaxChecker : FirDeclarationSyntaxChecker<FirProperty, KtExpression>() {
    override fun isApplicable(element: FirProperty, source: KtSourceElement): Boolean =
        source.kind !is KtFakeSourceElementKind && element.isLocal

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsi(
        element: FirProperty,
        source: KtPsiSourceElement,
        psi: KtExpression,
    ) {
        if (psi is KtProperty && psi.typeParameterList != null) {
            reporter.reportOn(source, FirErrors.LOCAL_VARIABLE_WITH_TYPE_PARAMETERS)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkLightTree(
        element: FirProperty,
        source: KtLightSourceElement,
    ) {
        val node = source.lighterASTNode
        if (node.tokenType != KtNodeTypes.PROPERTY) return
        source.treeStructure.typeParametersList(source.lighterASTNode)?.let { _ ->
            reporter.reportOn(source, FirErrors.LOCAL_VARIABLE_WITH_TYPE_PARAMETERS)
        }
    }
}
