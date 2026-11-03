/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.lang.LighterASTNode
import com.intellij.lang.LighterASTTokenNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.ElementTypeUtils.isExpression
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.diagnostics.findChildByType
import org.jetbrains.kotlin.diagnostics.findChildrenByType
import org.jetbrains.kotlin.fir.FirSyntaxComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.resolve.source.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.toKtLightSourceElement
import org.jetbrains.kotlin.util.getChildren
import org.jetbrains.kotlin.util.getPreviousSibling

/**
 * [FirSyntaxComponent] for sessions whose sources are built by the LightTree raw FIR builder.
 */
@NoMutableState
object FirLightTreeSyntaxComponent : FirAbstractPsiSyntaxComponent() {
    override val parserMode: ParserMode
        get() = ParserMode.LightTree

    override fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement> {
        val source = this as? KtLightSourceElement ?: return emptyList()
        val tree = source.treeStructure
        return tree.findChildByType(source.lighterASTNode, KtNodeTypes.MODIFIER_LIST)
            ?.let { tree.findChildrenByType(it, KtNodeTypes.CONTEXT_PARAMETER_LIST) }
            ?.map { it.toKtLightSourceElement(tree) }
            .orEmpty()
    }

    override fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement? {
        fun LighterASTNode.findContextParameterList(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? {
            return when (tokenType) {
                KtNodeTypes.TYPE_REFERENCE, KtNodeTypes.NULLABLE_TYPE ->
                    tree.findChildByType(this, KtTokenSets.TYPE_ELEMENT_TYPES)?.findContextParameterList(tree)
                KtNodeTypes.FUNCTION_TYPE -> tree.findChildByType(this, KtNodeTypes.CONTEXT_PARAMETER_LIST)
                else -> null
            }
        }

        val source = this as? KtLightSourceElement ?: return null
        val tree = source.treeStructure
        return source.lighterASTNode.findContextParameterList(tree)?.toKtLightSourceElement(tree)
    }

    override fun KtSourceElement.findDefaultValueSource(): KtSourceElement? {
        val source = this as? KtLightSourceElement ?: return null
        val tree = source.treeStructure
        var defaultValueOffset = source.startOffset

        for (node in source.lighterASTNode.getChildren(tree)) {
            if (node.isExpression()) {
                return node.toKtLightSourceElement(
                    tree,
                    startOffset = defaultValueOffset,
                    endOffset = defaultValueOffset + node.textLength,
                )
            }
            defaultValueOffset += node.endOffset - node.startOffset
        }
        return null
    }

    override fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement? {
        val source = this as? KtLightSourceElement ?: return null
        val tree = source.treeStructure
        return tree.findChildByType(source.lighterASTNode, KtTokens.VAL_VAR)?.toKtLightSourceElement(tree)
    }

    override fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement? {
        val source = this as? KtLightSourceElement ?: return null
        val tree = source.treeStructure
        return source.lighterASTNode.getExplicitReceiverOfDotQualified(tree)?.toKtLightSourceElement(tree)
    }

    override fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement? {
        val source = this as? KtLightSourceElement ?: return null
        val tree = source.treeStructure
        val node = source.lighterASTNode.getChildren(tree).find {
            it.tokenType == KtTokens.IDENTIFIER && tree.toString(it) == name.identifier
        } ?: return null
        return node.toKtLightSourceElement(tree, startOffset = node.startOffset, endOffset = node.endOffset)
    }

    override fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int? {
        val source = this as? KtLightSourceElement ?: return null
        val tree = source.treeStructure
        var parent = tree.getParent(source.lighterASTNode)
        while (parent != null) {
            if (parent.tokenType == KtNodeTypes.STRING_TEMPLATE) {
                return parent.getChildren(tree).count { it is PsiBuilder.Marker }
            }
            parent = tree.getParent(parent)
        }
        return null
    }

    override fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean {
        val source = this as? KtLightSourceElement ?: return false
        val tree = source.treeStructure
        var parent = tree.getParent(source.lighterASTNode) ?: return false
        if (parent.tokenType == KtNodeTypes.LABELED_EXPRESSION) {
            parent = tree.getParent(parent) ?: return false
        }
        if (parent.tokenType == KtNodeTypes.LAMBDA_ARGUMENT) {
            // Only trivia separates the lambda from the rest of the call, so the walk stops at the first preceding element
            var prevSibling = parent.getPreviousSibling(tree)
            while (prevSibling is LighterASTTokenNode) {
                if (prevSibling.tokenType == TokenType.WHITE_SPACE && prevSibling.text.contains("\n")) {
                    return true
                }
                prevSibling = prevSibling.getPreviousSibling(tree)
            }
        }
        return false
    }
}
