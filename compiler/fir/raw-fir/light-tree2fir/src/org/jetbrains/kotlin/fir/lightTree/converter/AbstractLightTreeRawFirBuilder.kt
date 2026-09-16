/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.ElementTypeUtils.isExpression
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.NotToShareWithAA
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.kmp.utils.kmpId
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.util.getChildren

@OptIn(NotToShareWithAA::class)
abstract class AbstractLightTreeRawFirBuilder(
    baseSession: FirSession,
    val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    context: Context<LighterASTNode> = Context()
) : AbstractTreeRawFirBuilder<LighterASTNode, IElementType>(baseSession, context) {

    override fun LighterASTNode.toFirSourceElement(kind: KtFakeSourceElementKind?): KtLightSourceElement {
        val startOffset = tree.getStartOffset(this)
        val endOffset = tree.getEndOffset(this)
        return toKtLightSourceElement(tree, kind ?: KtRealSourceElementKind, startOffset, endOffset)
    }

    override fun KtSourceElement.toNode(): LighterASTNode {
        return (this as KtLightSourceElement).lighterASTNode
    }

    override val LighterASTNode.elementType: IElementType
        get() = this.tokenType

    override fun IElementType.typeToTokenId(): Int = kmpId()

    override val LighterASTNode.asText: String
        get() = this.toString()

    override fun LighterASTNode.getChildren(): List<LighterASTNode> {
        return getChildren(tree)
    }

    override val LighterASTNode?.receiverExpression: LighterASTNode?
        get() {
            var candidate: LighterASTNode? = null
            this?.forEachChildren {
                when (it.tokenType) {
                    DOT, SAFE_ACCESS -> return if (candidate?.elementType != TokenType.ERROR_ELEMENT) candidate else null
                    else -> candidate = it
                }
            }
            return null
        }

    override val LighterASTNode?.selectorExpression: LighterASTNode?
        get() {
            var isSelector = false
            this?.forEachChildren {
                when (it.tokenType) {
                    DOT, SAFE_ACCESS -> isSelector = true
                    else -> if (isSelector) {
                        return if (it.elementType != TokenType.ERROR_ELEMENT) it else null
                    }
                }
            }
            return null
        }

    override val LighterASTNode?.indexExpressions: List<LighterASTNode>?
        get() = this?.getLastChildExpression()?.getChildrenAsArray()?.filterNotNull()?.filter { it.isExpression() }

    override fun LighterASTNode.getParent(): LighterASTNode? {
        return tree.getParent(this)
    }

    override fun LighterASTNode?.getChildrenAsArray(): Array<out LighterASTNode?> {
        if (this == null) return arrayOf()

        val kidsRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, kidsRef)
        return kidsRef.get()
    }
}
