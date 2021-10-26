/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.annotations
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.userType
import org.jetbrains.kotlin.fir.analysis.buildChildSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isUnderscore
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.psi.KtTypeProjection

object FirUnderscoredTypeArgumentSyntaxChecker : FirExpressionSyntaxChecker<FirFunctionCall, PsiElement>() {
    override fun isApplicable(element: FirFunctionCall, source: KtSourceElement): Boolean =
        element.typeArguments.isNotEmpty()

    override fun checkPsi(
        element: FirFunctionCall,
        source: KtPsiSourceElement,
        psi: PsiElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (typeProjection in element.typeArguments) {
            val psiTypeArgument = typeProjection.source?.psi as? KtTypeProjection ?: continue
            val typeReference = psiTypeArgument.typeReference ?: continue

            if (!typeReference.isPlaceholder) continue

            for (annotation in typeReference.annotationEntries) {
                reporter.reportOn(
                    annotation.toKtPsiSourceElement(), FirErrors.UNSUPPORTED,
                    "annotations on an underscored type argument", context
                )
            }
        }
    }

    override fun checkLightTree(
        element: FirFunctionCall,
        source: KtLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        for (typeProjection in element.typeArguments) {
            val lightTreeTypeArgument = typeProjection.source?.lighterASTNode ?: continue

            if (!source.treeStructure.userType(lightTreeTypeArgument).toString().isUnderscore) continue

            val annotations = source.treeStructure.annotations(lightTreeTypeArgument) ?: continue

            for (annotation in annotations) {
                reporter.reportOn(
                    source.buildChildSourceElement(annotation), FirErrors.UNSUPPORTED,
                    "annotations on an underscored type argument", context
                )
            }
        }
    }
}
