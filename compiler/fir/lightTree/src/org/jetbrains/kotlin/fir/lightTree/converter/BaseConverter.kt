/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.BaseFirBuilder
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.escapedStringToCharacter
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name

open class BaseConverter(
    session: FirSession,
    private val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    context: Context = Context()
) : BaseFirBuilder<LighterASTNode>(session, context) {
    protected val implicitType = FirImplicitTypeRefImpl(null)

    override val LighterASTNode.elementType: IElementType
        get() = this.tokenType

    override val LighterASTNode.asText: String
        get() = this.toString()

    override val LighterASTNode.unescapedValue: String
        get() {
            val escape = this.asText
            return escapedStringToCharacter(escape)?.toString()
                ?: escape.replace("\\", "").replace("u", "\\u")
        }

    override fun LighterASTNode.getReferencedNameAsName(): Name {
        return this.asText.nameAsSafeName()
    }

    override fun LighterASTNode.getLabelName(): String? {
        this.forEachChildren {
            when (it.tokenType) {
                KtNodeTypes.LABEL_QUALIFIER -> return it.asText.replaceFirst("@", "")
            }
        }

        return null
    }

    override fun LighterASTNode.getExpressionInParentheses(): LighterASTNode? {
        this.forEachChildren {
            if (it.isExpression()) return it
        }

        return null
    }

    override fun LighterASTNode.getChildNodeByType(type: IElementType): LighterASTNode? {
        return this.getChildNodesByType(type).firstOrNull()
    }

    override val LighterASTNode?.selectorExpression: LighterASTNode?
        get() {
            var isSelector = false
            this?.forEachChildren {
                when (it.tokenType) {
                    DOT, SAFE_ACCESS -> isSelector = true
                    else -> if (isSelector) return it
                }
            }
            return null
        }

    fun LighterASTNode.getParent(): LighterASTNode? {
        return tree.getParent(this)
    }

    fun LighterASTNode?.getChildNodesByType(type: IElementType): List<LighterASTNode> {
        return this?.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                type -> container += node
            }
        } ?: listOf()
    }

    fun LighterASTNode?.getChildrenAsArray(): Array<out LighterASTNode?> {
        if (this == null) return arrayOf()

        val kidsRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, kidsRef)
        return kidsRef.get()
    }

    protected inline fun LighterASTNode.forEachChildren(vararg skipTokens: KtToken, f: (LighterASTNode) -> Unit) {
        val kidsArray = this.getChildrenAsArray()
        for (kid in kidsArray) {
            if (kid == null) break
            val tokenType = kid.tokenType
            if (COMMENTS.contains(tokenType) || tokenType == WHITE_SPACE || tokenType == SEMICOLON || tokenType in skipTokens) continue
            f(kid)
        }
    }

    protected inline fun <T> LighterASTNode.forEachChildrenReturnList(f: (LighterASTNode, MutableList<T>) -> Unit): List<T> {
        val kidsArray = this.getChildrenAsArray()

        val container = mutableListOf<T>()
        for (kid in kidsArray) {
            if (kid == null) break
            val tokenType = kid.tokenType
            if (COMMENTS.contains(tokenType) || tokenType == WHITE_SPACE || tokenType == SEMICOLON) continue
            f(kid, container)
        }

        return container
    }
}