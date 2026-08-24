/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.SyntaxElementTypeSet
import com.intellij.platform.syntax.element.SyntaxTokenTypes.BAD_CHARACTER
import com.intellij.platform.syntax.element.SyntaxTokenTypes.ERROR_ELEMENT
import com.intellij.platform.syntax.syntaxElementTypeSetOf
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.lightTree.converter.AbstractTreeRawFirBuilder
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.lexer.KtTokens.COMMENTS
import org.jetbrains.kotlin.kmp.lexer.KtTokens.DOT
import org.jetbrains.kotlin.kmp.lexer.KtTokens.SAFE_ACCESS
import org.jetbrains.kotlin.kmp.lexer.KtTokens.SEMICOLON
import org.jetbrains.kotlin.kmp.lexer.KtTokens.WHITE_SPACE
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree

abstract class AbstractMultiplatformParsingRawFirBuilder(
    baseSession: FirSession,
    val treeStructure: KotlinLightTreeStructure,
    context: Context<LightNode> = Context(),
) : AbstractTreeRawFirBuilder<LightNode, SyntaxElementType>(baseSession, context) {
    companion object {
        protected val ignoredTokens: SyntaxElementTypeSet = COMMENTS + syntaxElementTypeSetOf(
            WHITE_SPACE, SEMICOLON, ERROR_ELEMENT, BAD_CHARACTER,
        )
    }

    protected val tree: LightSyntaxTree
        get() = treeStructure.tree

    override val LightNode.elementType: SyntaxElementType
        get() = tree.getType(this)

    override fun SyntaxElementType.isPlus(): Boolean = this == KtTokens.PLUS
    override fun SyntaxElementType.isMinus(): Boolean = this == KtTokens.MINUS

    override fun SyntaxElementType.isIntegerConstant(): Boolean = this == KtTokens.INTEGER_LITERAL
    override fun SyntaxElementType.isFloatConstant(): Boolean = this == KtTokens.FLOAT_LITERAL
    override fun SyntaxElementType.isBooleanConstant(): Boolean = this == KtNodeTypes.BOOLEAN_CONSTANT
    override fun SyntaxElementType.isCharacterConstant(): Boolean = this == KtTokens.CHARACTER_LITERAL
    override fun SyntaxElementType.isNullConstant(): Boolean = this == KtNodeTypes.NULL

    override fun SyntaxElementType.isStringInterpolationPrefix(): Boolean = this == KtNodeTypes.STRING_INTERPOLATION_PREFIX
    override fun SyntaxElementType.isOpenQuote(): Boolean = this == KtTokens.OPEN_QUOTE
    override fun SyntaxElementType.isClosingQuote(): Boolean = this == KtTokens.CLOSING_QUOTE
    override fun SyntaxElementType.isLiteralStringTemplateEntry(): Boolean = this == KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY
    override fun SyntaxElementType.isEscapeStringTemplateEntry(): Boolean = this == KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY
    override fun SyntaxElementType.isShortStringTemplateEntry(): Boolean = this == KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY
    override fun SyntaxElementType.isLongStringTemplateEntry(): Boolean = this == KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY

    override fun SyntaxElementType.isArrayAccessExpression(): Boolean = this == KtNodeTypes.ARRAY_ACCESS_EXPRESSION
    override fun SyntaxElementType.isSafeAccessExpression(): Boolean = this == KtNodeTypes.SAFE_ACCESS_EXPRESSION
    override fun SyntaxElementType.isParenthesized(): Boolean = this == KtNodeTypes.PARENTHESIZED
    override fun SyntaxElementType.isLabeledExpression(): Boolean = this == KtNodeTypes.LABELED_EXPRESSION
    override fun SyntaxElementType.isAnnotatedExpression(): Boolean = this == KtNodeTypes.ANNOTATED_EXPRESSION

    val LightNode.tokenType: SyntaxElementType
        get() = tree.getType(this)

    override val LightNode.asText: String
        get() = tree.getText(this).toString()

    override fun LightNode.getLabelName(): String? {
        if (tokenType == KtNodeTypes.FUN) {
            return getParent()?.getLabelName()
        }
        this.forEachChildren {
            when (it.tokenType) {
                KtNodeTypes.LABEL_QUALIFIER -> return it.asText.replaceFirst("@", "").let(::unquoteIdentifier)
            }
        }

        return null
    }

    override fun LightNode.getChildNodeByType(type: SyntaxElementType): LightNode? {
        return tree.getChildren(this).firstOrNull { it.tokenType == type }
    }

    override fun LightNode.getModifierList(): LightNode? = getChildNodeByType(KtNodeTypes.MODIFIER_LIST)

    override fun LightNode.getVarargKeyword(): LightNode? = getChildNodeByType(KtTokens.VARARG_MODIFIER)

    override val LightNode?.receiverExpression: LightNode?
        get() {
            var candidate: LightNode? = null
            this?.forEachChildren {
                when (it.tokenType) {
                    DOT, SAFE_ACCESS -> return if (candidate?.tokenType != ERROR_ELEMENT) candidate else null
                    else -> candidate = it
                }
            }
            return null
        }

    override val LightNode?.selectorExpression: LightNode?
        get() {
            var isSelector = false
            this?.forEachChildren {
                when (it.tokenType) {
                    DOT, SAFE_ACCESS -> isSelector = true
                    else -> if (isSelector) return if (it.tokenType != ERROR_ELEMENT) it else null
                }
            }
            return null
        }

    override val LightNode?.indexExpressions: List<LightNode>?
        get() = this?.getLastChildExpression()?.let {
            tree.getChildren(it).filter { it.isExpression() }
        }

    protected fun LightNode.getParent(): LightNode? {
        return tree.getParent(this)
    }

    override fun LightNode.getFirstChildExpression(): LightNode? {
        forEachChildren {
            if (it.isExpression()) return it
        }

        return null
    }

    override fun LightNode.getLastChildExpression(): LightNode? {
        var result: LightNode? = null
        forEachChildren {
            if (it.isExpression()) {
                result = it
            }
        }

        return result
    }

    protected fun LightNode.isExpression(): Boolean {

    }

    protected inline fun LightNode.forEachChildren(f: (LightNode) -> Unit) {
        val kids = tree.getChildren(this)
        for (kid in kids) {
            if (ignoredTokens.contains(kid.tokenType)) continue
            f(kid)
        }
    }
}
