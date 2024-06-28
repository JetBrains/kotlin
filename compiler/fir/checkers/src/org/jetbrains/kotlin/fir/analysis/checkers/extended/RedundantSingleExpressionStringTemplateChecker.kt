/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirStringConcatenationCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.util.getChildren

object RedundantSingleExpressionStringTemplateChecker : FirStringConcatenationCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStringConcatenationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        for (argumentExpression in expression.arguments) {
            if (argumentExpression.resolvedType.fullyExpandedClassId(context.session) == StandardClassIds.String &&
                argumentExpression.stringParentChildrenCount() == 1 // there is no more children in original string template
            ) {
                reporter.reportOn(argumentExpression.source, REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE, context)
            }
        }
    }

    private fun FirStatement.stringParentChildrenCount(): Int? {
        return when (val source = source) {
            is KtPsiSourceElement -> source.psi.stringParentChildrenCount()
            is KtLightSourceElement -> source.lighterASTNode.stringParentChildrenCount(source)
            null -> null
        }
    }

    private fun PsiElement.stringParentChildrenCount(): Int? {
        if (parent is KtStringTemplateExpression) return parent?.children?.size
        return parent.stringParentChildrenCount()
    }

    private fun LighterASTNode.stringParentChildrenCount(source: KtLightSourceElement): Int? {
        val parent = source.treeStructure.getParent(this)
        return if (parent != null && parent.tokenType == KtNodeTypes.STRING_TEMPLATE) {
            val childrenOfParent = parent.getChildren(source.treeStructure)
            childrenOfParent.filter { it is PsiBuilder.Marker }.size
        } else {
            parent?.stringParentChildrenCount(source)
        }
    }
}
