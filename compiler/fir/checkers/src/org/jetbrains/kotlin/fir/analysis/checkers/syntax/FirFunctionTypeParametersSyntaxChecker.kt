/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.nameIdentifier
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.typeParametersList
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.startOffset

object FirFunctionTypeParametersSyntaxChecker : FirDeclarationSyntaxChecker<FirSimpleFunction, KtFunction>() {
    override fun isApplicable(element: FirSimpleFunction, source: KtSourceElement): Boolean =
        source.kind !is KtFakeSourceElementKind

    override fun checkPsi(
        element: FirSimpleFunction,
        source: KtPsiSourceElement,
        psi: KtFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val typeParamsNode = psi.typeParameterList
        val nameNode = psi.nameIdentifier

        if (typeParamsNode != null && nameNode != null && typeParamsNode.startOffset > nameNode.startOffset) {
            reporter.reportOn(
                source,
                FirErrors.DEPRECATED_TYPE_PARAMETER_SYNTAX,
                context
            )
        }
    }

    override fun checkLightTree(
        element: FirSimpleFunction,
        source: KtLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val typeParamsNode = source.treeStructure.typeParametersList(source.lighterASTNode)
        val nameNode = source.treeStructure.nameIdentifier(source.lighterASTNode)
        if (typeParamsNode != null && nameNode != null && typeParamsNode.startOffset > nameNode.startOffset) {
            reporter.reportOn(
                source,
                FirErrors.DEPRECATED_TYPE_PARAMETER_SYNTAX,
                context
            )
        }
    }
}
