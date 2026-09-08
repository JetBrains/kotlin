/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtLiteralPrefixAndSuffix
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.psi.KtImplementationDetail

object KotlinLightParser {
    fun buildLightTree(
        code: CharSequence,
        sourceFile: KtSourceFile?,
        errorListener: LightTreeParsingErrorListener?,
    ): FlyweightCapableTreeStructure<LighterASTNode> {
        val builder = PsiBuilderFactory.getInstance().createBuilder(KotlinParserDefinition(), KotlinLexer(), code)
        return parse(
            builder,
            isScript = sourceFile?.let { FileUtilRt.getExtension(it.name) != KotlinFileType.EXTENSION } ?: false
        ).also {
            if (errorListener != null) checkSyntaxErrors(it.root, it, errorListener)
        }
    }

    fun parse(builder: PsiBuilder, isScript: Boolean): FlyweightCapableTreeStructure<LighterASTNode> {
        val ktParsing = KotlinParsing.createForTopLevelNonLazy(SemanticWhitespaceAwarePsiBuilderImpl(builder))
        if (isScript) {
            ktParsing.parseScript()
        } else {
            ktParsing.parseFile()
        }

        return builder.lightTree
    }

    enum class SyntaxErrorType {
        Syntax,
        TrailingWhitespaceRequired,
        LeadingWhitespaceRequired,
    }

    fun interface LightTreeParsingErrorListener {
        fun onError(node: LighterASTNode, syntaxErrorType: SyntaxErrorType)
    }

    /**
     * Reports the syntax problems of the tree rooted at [root], in document order: every [TokenType.ERROR_ELEMENT] the
     * parser left behind, and every literal glued to a token that may not touch it (see
     * [KtLiteralPrefixAndSuffix]).
     *
     * The literal check rides along with this walk instead of living in a FIR checker because a checker only gets the
     * literal itself, and reaching the leaf next to it means climbing the light tree back up once per literal, which
     * costs more than the single pass the parser already makes here. The PSI frontend does not build its tree through
     * this parser and is still covered by `FirPrefixAndSuffixSyntaxChecker`.
     *
     * Recursion is emulated with an explicit stack to avoid stack overflows on deeply nested trees. A source such as
     * `val x = "a0" + "a1" + ... + "a9999"`, which machine-generated code produces regularly, nests one binary
     * expression per operand, so descending with one frame per level exhausts the stack (KT-88399).
     */
    private fun checkSyntaxErrors(
        root: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        errorListener: LightTreeParsingErrorListener,
    ) {
        // `null` is a marker pushed underneath the children of a literal, so that popping it means the whole literal
        // has been visited and the leaf that comes next is its suffix.
        val stack = ArrayDeque<LighterASTNode?>()
        stack.addLast(root)
        val ref = Ref<Array<LighterASTNode?>>()

        // The last leaf popped so far, which is the one preceding the node being visited in the document.
        var previousLeaf: LighterASTNode? = null
        // Whether the next leaf to be popped is the one following a literal.
        var afterLiteral = false

        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (node == null) {
                afterLiteral = true
                continue
            }

            val tokenType = node.tokenType
            if (node !== root && tokenType == TokenType.ERROR_ELEMENT) {
                errorListener.onError(node, SyntaxErrorType.Syntax)
            }

            val isLiteral = tokenType in KtLiteralPrefixAndSuffix.literalElementTypes
            if (isLiteral) {
                // The prefix of a literal is the leaf right before it, and nodes are visited in document order.
                previousLeaf?.let { checkWhitespaceRequired(it, errorListener, prefix = true) }
            }

            ref.set(null)
            val count = tree.getChildren(node, ref)
            val childrenArray = ref.get()

            if (count == 0 || childrenArray == null) {
                // A leaf: it is the suffix of the literal that ended right before it, if there was one.
                if (afterLiteral) {
                    afterLiteral = false
                    checkWhitespaceRequired(node, errorListener, prefix = false)
                }
                previousLeaf = node
                continue
            }

            if (isLiteral) stack.addLast(null)

            // Push in reverse so that the children are popped left to right, keeping the original document order.
            for (index in minOf(count, childrenArray.size) - 1 downTo 0) {
                stack.addLast(childrenArray[index] ?: continue)
            }
        }
    }

    /** Reports [leaf] if it is a token that may not be glued to the literal it sits next to. */
    private fun checkWhitespaceRequired(leaf: LighterASTNode, errorListener: LightTreeParsingErrorListener, prefix: Boolean) {
        if (KtLiteralPrefixAndSuffix.isProhibitedPrefixOrSuffix(leaf.tokenType)) {
            errorListener.onError(
                leaf,
                if (prefix) SyntaxErrorType.TrailingWhitespaceRequired else SyntaxErrorType.LeadingWhitespaceRequired
            )
        }
    }
}
