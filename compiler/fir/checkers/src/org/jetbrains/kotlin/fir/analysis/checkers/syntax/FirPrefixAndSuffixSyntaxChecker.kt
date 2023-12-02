/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.KtNodeTypes.BINARY_EXPRESSION
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.util.getChildren

object FirPrefixAndSuffixSyntaxChecker : FirExpressionSyntaxChecker<FirStatement, KtExpression>() {

    private val literalConstants = listOf(KtNodeTypes.CHARACTER_CONSTANT, KtNodeTypes.FLOAT_CONSTANT, KtNodeTypes.INTEGER_CONSTANT)

    override fun isApplicable(element: FirStatement, source: KtSourceElement): Boolean =
        source.kind !is KtFakeSourceElementKind && (source.elementType == KtNodeTypes.STRING_TEMPLATE || source.elementType in literalConstants)

    override fun checkPsi(
        element: FirStatement,
        source: KtPsiSourceElement,
        psi: KtExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        psi.prevLeaf()?.let { checkLiteralPrefixOrSuffix(it, context, reporter) }
        psi.nextLeaf()?.let { checkLiteralPrefixOrSuffix(it, context, reporter) }
    }


    override fun checkLightTree(
        element: FirStatement,
        source: KtLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        source.lighterASTNode.prevLeaf(source.treeStructure)
            ?.let { checkLiteralPrefixOrSuffix(it, source, context, reporter) }
        source.lighterASTNode.nextLeaf(source.treeStructure)
            ?.let { checkLiteralPrefixOrSuffix(it, source, context, reporter) }
    }

    private enum class Direction(val offset: Int) {
        PREVIOUS(-1),
        NEXT(1)
    }

    private fun LighterASTNode.getLeaf(
        direction: Direction,
        treeStructure: FlyweightCapableTreeStructure<LighterASTNode>,
    ): LighterASTNode? {
        val parent = treeStructure.getParent(this) ?: return null
        val children = parent.getChildren(treeStructure)
        val index = children.indexOf(this)
        val leaf = children.getOrNull(index - direction.offset)
        return when {
            // Necessary for finding the next leaf in complex binary expressions, for example 'a foo"asdsfsa"foo a'
            leaf == null && parent.tokenType == BINARY_EXPRESSION -> parent.getLeaf(direction, treeStructure)
            leaf == null -> return null
            else -> {
                // This is necessary to obtain the simplest node, as the found leaf can be a complex expression
                var result = leaf
                var resultChildren = leaf.getChildren(treeStructure)
                while (resultChildren.isNotEmpty()) {
                    result = if (direction == Direction.PREVIOUS) resultChildren.first() else resultChildren.last()
                    resultChildren = result.getChildren(treeStructure)
                }
                result
            }
        }
    }

    private fun LighterASTNode.prevLeaf(treeStructure: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? {
        return getLeaf(Direction.PREVIOUS, treeStructure)
    }

    private fun LighterASTNode.nextLeaf(treeStructure: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? {
        return getLeaf(Direction.NEXT, treeStructure)
    }

    private fun checkLiteralPrefixOrSuffix(
        prefixOrSuffix: PsiElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (illegalLiteralPrefixOrSuffix(prefixOrSuffix.node.elementType)) {
            report(prefixOrSuffix.toKtPsiSourceElement(), context, reporter)
        }
    }

    private fun checkLiteralPrefixOrSuffix(
        prefixOrSuffix: LighterASTNode,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val elementType = prefixOrSuffix.tokenType ?: return
        if (illegalLiteralPrefixOrSuffix(elementType)) {
            report(prefixOrSuffix.toKtLightSourceElement(source.treeStructure), context, reporter)
        }
    }

    private fun illegalLiteralPrefixOrSuffix(elementType: IElementType): Boolean =
        (elementType === KtTokens.IDENTIFIER || elementType === KtTokens.INTEGER_LITERAL || elementType === KtTokens.FLOAT_LITERAL || elementType is KtKeywordToken)


    private fun report(source: KtSourceElement, context: CheckerContext, reporter: DiagnosticReporter) {
        reporter.reportOn(source, FirErrors.UNSUPPORTED, "literal prefixes and suffixes", context)
    }
}
