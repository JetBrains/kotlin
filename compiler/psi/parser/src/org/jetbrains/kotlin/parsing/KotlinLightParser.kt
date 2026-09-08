/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.lexer.KotlinLexer

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
            if (errorListener != null) reportErrors(it.root, it, errorListener)
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

    fun interface LightTreeParsingErrorListener {
        fun onError(startOffset: Int, endOffset: Int, message: String?)
    }

    /**
     * Reports every [TokenType.ERROR_ELEMENT] in the tree rooted at [root], in document order.
     *
     * Recursion is emulated with an explicit stack to avoid stack overflows on deeply nested trees. A source such as
     * `val x = "a0" + "a1" + ... + "a9999"`, which machine-generated code produces regularly, nests one binary
     * expression per operand, so descending with one frame per level exhausts the stack (KT-88399).
     */
    private fun reportErrors(
        root: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        errorListener: LightTreeParsingErrorListener,
    ) {
        val stack = ArrayDeque<LighterASTNode>()
        stack.addLast(root)
        val ref = Ref<Array<LighterASTNode?>>()

        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (node !== root && node.tokenType == TokenType.ERROR_ELEMENT) {
                val message = PsiBuilderImpl.getErrorMessage(node)
                errorListener.onError(node.startOffset, node.endOffset, message)
            }

            ref.set(null)
            val count = tree.getChildren(node, ref)
            val childrenArray = ref.get() ?: continue

            // Push in reverse so that the children are popped left to right, keeping the original document order.
            for (index in minOf(count, childrenArray.size) - 1 downTo 0) {
                stack.addLast(childrenArray[index] ?: continue)
            }
        }
    }
}
