/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

object RedundantSingleExpressionStringTemplateChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.source?.kind != FirFakeSourceElementKind.GeneratedToStringCallOnTemplateEntry) return
        if (expression !is FirFunctionCall) return
        if (
            expression.explicitReceiver?.typeRef?.coneType?.classId == StandardClassIds.String
            && expression.stringParentChildrenCount() == 1 // there is no more children in original string template
        ) {
            reporter.report(expression.source, REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE)
        }
    }

    private fun FirStatement.stringParentChildrenCount(): Int? {
        return when (val source = source) {
            is FirPsiSourceElement<*> -> {
                source.psi.stringParentChildrenCount()
            }
            is FirLightSourceElement -> {
                source.lighterASTNode.stringParentChildrenCount(source)
            }
            else -> null
        }
    }

    private fun PsiElement.stringParentChildrenCount(): Int? {
        if (parent is KtStringTemplateExpression) return parent?.children?.size
        return parent.stringParentChildrenCount()
    }

    private fun LighterASTNode.stringParentChildrenCount(source: FirLightSourceElement): Int? {
        val parent = source.treeStructure.getParent(this)
        return if (parent?.tokenType == KtNodeTypes.STRING_TEMPLATE) {
            val childrenOfParent = Ref<Array<LighterASTNode>>()
            source.treeStructure.getChildren(parent!!, childrenOfParent)
            childrenOfParent.get().filter { it is PsiBuilder.Marker }.size
        } else {
            parent?.stringParentChildrenCount(source)
        }
    }
}
