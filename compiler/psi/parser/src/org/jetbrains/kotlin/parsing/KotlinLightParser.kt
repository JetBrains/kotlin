/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
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
            if (errorListener != null) checkOrReportSyntaxErrors(it.root, it, errorListener)
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
     * literal itself, while the tokens surrounding it may sit arbitrarily far away in the tree. Take the tree of
     * `... * 'a' + 'b' * ...`, where `binary` is a binary expression node and every quoted node is a leaf token:
     *
     * ```
     *                  binary
     *       binary       '+'       binary
     *    ... '*' 'a'            'b' '*' ...
     * ```
     *
     * The leaf following `'a'` is the `'+'` hanging off the root, so reaching it means climbing out of the entire left
     * subtree, and reaching `'b'` after it means descending the leftmost path of the right one — once per literal in
     * the file. This walk instead visits every leaf exactly once and only has to remember the leaf it saw last, so
     * both neighbours of a literal come for free.
     *
     * The PSI frontend does not build its tree through this parser; there the same check is done by
     * `FirPrefixAndSuffixSyntaxChecker` with the standard `prevLeaf` / `nextLeaf`. Their cost is unclear, but the PSI
     * path is not performance-critical, so avoiding them is not worth it there.
     *
     * Recursion is emulated with an explicit stack to avoid stack overflows on deeply nested trees. A source such as
     * `val x = "a0" + "a1" + ... + "a9999"`, which machine-generated code produces regularly, nests one binary
     * expression per operand, so descending with one frame per level exhausts the stack (KT-88399).
     */
    private fun checkOrReportSyntaxErrors(
        root: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        errorListener: LightTreeParsingErrorListener,
    ) {
        // A `null` entry is a marker sitting under the children of a literal: popping it means that literal has been
        // visited in full, so the leaf that comes next is its suffix.
        val stack = ArrayDeque<LighterASTNode?>()
        stack.addLast(root)
        val ref = Ref<Array<LighterASTNode?>>()

        // The last leaf visited, which is the leaf preceding the node being visited in the document.
        var previousLeaf: LighterASTNode? = null
        // Whether a literal has just been visited in full, which makes the next leaf its suffix.
        var literalJustEnded = false

        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (node == null) {
                literalJustEnded = true
                continue
            }

            val tokenType = node.tokenType
            @OptIn(KtImplementationDetail::class)
            when (tokenType) {
                TokenType.ERROR_ELEMENT -> {
                    errorListener.onError(node, SyntaxErrorType.Syntax)
                }
                in KtLiteralPrefixAndSuffix.relevantLiteralTypes -> {
                    // Nodes are visited in document order, so the prefix of the literal is the leaf visited last. Its
                    // suffix is only known once the literal is behind us, which the marker pushed here announces.
                    previousLeaf?.let { checkWhitespaceRequired(it, errorListener, prefix = true) }
                    stack.addLast(null)
                }
            }

            ref.set(null)
            val count = tree.getChildren(node, ref)
            val childrenArray = ref.get()

            if (count == 0 || childrenArray == null) {
                // A leaf: it is the suffix of the literal that ended right before it, if there was one.
                if (literalJustEnded) {
                    literalJustEnded = false
                    checkWhitespaceRequired(node, errorListener, prefix = false)
                }
                previousLeaf = node
                continue
            }

            // Push in reverse so that the children are popped left to right, keeping the original document order.
            for (index in minOf(count, childrenArray.size) - 1 downTo 0) {
                stack.addLast(childrenArray[index] ?: continue)
            }
        }
    }

    /** Reports [leaf] if it is a token that may not be glued to the literal it sits next to. */
    private fun checkWhitespaceRequired(leaf: LighterASTNode, errorListener: LightTreeParsingErrorListener, prefix: Boolean) {
        @OptIn(KtImplementationDetail::class)
        if (KtLiteralPrefixAndSuffix.isProhibitedPrefixOrSuffix(leaf.tokenType)) {
            errorListener.onError(
                leaf,
                if (prefix) SyntaxErrorType.TrailingWhitespaceRequired else SyntaxErrorType.LeadingWhitespaceRequired
            )
        }
    }
}
