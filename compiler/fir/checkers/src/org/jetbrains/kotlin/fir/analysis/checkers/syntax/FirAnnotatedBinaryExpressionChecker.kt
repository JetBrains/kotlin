/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.getChildrenArray
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.isStatementContainer

object FirAnnotatedBinaryExpressionChecker : FirExpressionSyntaxChecker<FirStatement, PsiElement>() {
    override fun isApplicable(element: FirStatement, source: KtSourceElement): Boolean {
        return source.kind is KtRealSourceElementKind && source.elementType == KtNodeTypes.BINARY_EXPRESSION && source.hasAnnotatedLhs
    }

    private val KtSourceElement.hasAnnotatedLhs: Boolean
        get() = (psi as? KtBinaryExpression)?.left is KtAnnotatedExpression
                || treeStructure.getChildrenArray(lighterASTNode).firstOrNull()?.tokenType == KtNodeTypes.ANNOTATED_EXPRESSION

    override fun checkPsi(
        element: FirStatement,
        source: KtPsiSourceElement,
        psi: PsiElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        var current = source.psi
        var parent = current.parent

        while (parent is KtBinaryExpression) {
            if (parent.left != current) {
                return
            }

            current = parent
            parent = parent.parent
        }

        if (isStatementContainer(parent)) {
            reporter.reportOn(source, FirErrors.ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE, context)
        }
    }

    override fun checkLightTree(
        element: FirStatement,
        source: KtLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        var current = source.lighterASTNode
        var parent: LighterASTNode? = source.treeStructure.getParent(current)

        while (parent?.tokenType == KtNodeTypes.BINARY_EXPRESSION) {
            if (source.treeStructure.getChildrenArray(parent).firstOrNull() != current) {
                return
            }

            current = parent
            parent = source.treeStructure.getParent(parent)
        }

        if (parent?.isStatementContainer == true) {
            reporter.reportOn(source, FirErrors.ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE, context)
        }
    }

    private val LighterASTNode.isStatementContainer: Boolean
        get() = tokenType == KtNodeTypes.BLOCK
                || tokenType == KtNodeTypes.WHEN_ENTRY
                || isContainerNodeForControlStructureBody

    private val LighterASTNode.isContainerNodeForControlStructureBody: Boolean
        get() = tokenType == KtNodeTypes.BODY
                || tokenType == KtNodeTypes.ELSE
                || tokenType == KtNodeTypes.THEN
}