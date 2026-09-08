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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsi(
        element: FirStatement,
        source: KtPsiSourceElement,
        psi: KtExpression,
    ) {
        psi.prevLeaf()?.let { checkLiteralPrefixOrSuffix(it) }
        psi.nextLeaf()?.let { checkLiteralPrefixOrSuffix(it) }
    }


    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkLightTree(
        element: FirStatement,
        source: KtLightSourceElement,
    ) {
        source.lighterASTNode.prevLeaf(source.treeStructure)
            ?.let { checkLiteralPrefixOrSuffix(it, source) }
        source.lighterASTNode.nextLeaf(source.treeStructure)
            ?.let { checkLiteralPrefixOrSuffix(it, source) }
    }

    /**
     * Returns the leaf adjacent to this node in the token stream: the one right after it when [forward] is `true`, the
     * one right before it otherwise. Returns `null` at the edges of the file.
     *
     * This mirrors `PsiTreeUtil.prevLeaf`/`nextLeaf`, which [checkPsi] relies on, so that both halves of the checker
     * report the same diagnostics (KT-88961).
     *
     * The walk up the tree is iterative on purpose. A chain such as `"a0" + "a1" + ...` nests one `BINARY_EXPRESSION`
     * per operand, so recursing once per level overflows the stack on generated sources (KT-88399).
     */
    private fun LighterASTNode.adjacentLeaf(
        treeStructure: FlyweightCapableTreeStructure<LighterASTNode>,
        forward: Boolean,
    ): LighterASTNode? {
        var node: LighterASTNode = this
        while (true) {
            val parent = treeStructure.getParent(node) ?: return null
            val children = parent.getChildren(treeStructure)
            val index = children.indexOf(node)
            val sibling = children.getOrNull(if (forward) index + 1 else index - 1)
            if (sibling == null) {
                // Nothing on that side of the parent: the adjacent leaf lives outside it, so keep climbing.
                node = parent
                continue
            }
            // The sibling can be a composite expression, so descend to the leaf that actually touches this node.
            var result: LighterASTNode = sibling
            while (true) {
                val resultChildren = result.getChildren(treeStructure)
                if (resultChildren.isEmpty()) return result
                result = if (forward) resultChildren.first() else resultChildren.last()
            }
        }
    }

    private fun LighterASTNode.prevLeaf(treeStructure: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? {
        return adjacentLeaf(treeStructure, forward = false)
    }

    private fun LighterASTNode.nextLeaf(treeStructure: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? {
        return adjacentLeaf(treeStructure, forward = true)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkLiteralPrefixOrSuffix(
        prefixOrSuffix: PsiElement,
    ) {
        if (illegalLiteralPrefixOrSuffix(prefixOrSuffix.node.elementType)) {
            report(prefixOrSuffix.toKtPsiSourceElement())
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkLiteralPrefixOrSuffix(
        prefixOrSuffix: LighterASTNode,
        source: KtSourceElement,
    ) {
        val elementType = prefixOrSuffix.tokenType ?: return
        if (illegalLiteralPrefixOrSuffix(elementType)) {
            report(prefixOrSuffix.toKtLightSourceElement(source.treeStructure))
        }
    }

    private fun illegalLiteralPrefixOrSuffix(elementType: IElementType): Boolean =
        (elementType === KtTokens.IDENTIFIER || elementType === KtTokens.INTEGER_LITERAL || elementType === KtTokens.FLOAT_LITERAL || elementType is KtKeywordToken)


    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun report(source: KtSourceElement) {
        reporter.reportOn(source, FirErrors.UNSUPPORTED, "Literals must be surrounded by whitespace.")
    }
}
