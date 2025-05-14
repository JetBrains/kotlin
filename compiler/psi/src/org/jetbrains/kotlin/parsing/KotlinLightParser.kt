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
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.lexer.KotlinLexer

object KotlinLightParser {
    fun buildLightTree(
        code: CharSequence,
        errorListener: LightTreeParsingErrorListener?,
    ): FlyweightCapableTreeStructure<LighterASTNode> {
        val builder = PsiBuilderFactory.getInstance().createBuilder(KotlinParserDefinition(), KotlinLexer(), code)
        return parse(builder).also {
            if (errorListener != null) reportErrors(it.root, it, errorListener)
        }
    }

    private fun parse(builder: PsiBuilder): FlyweightCapableTreeStructure<LighterASTNode> {
        val ktParsing = KotlinParsing.createForTopLevelNonLazy(SemanticWhitespaceAwarePsiBuilderImpl(builder))
        ktParsing.parseFile()

        return builder.lightTree
    }

    fun interface LightTreeParsingErrorListener {
        fun onError(startOffset: Int, endOffset: Int, message: String?)
    }

    private fun reportErrors(
        node: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        errorListener: LightTreeParsingErrorListener,
        ref: Ref<Array<LighterASTNode?>> = Ref<Array<LighterASTNode?>>(),
    ) {
        tree.getChildren(node, ref)
        val kidsArray = ref.get() ?: return

        for (kid in kidsArray) {
            if (kid == null) break
            val tokenType = kid.tokenType
            if (tokenType == TokenType.ERROR_ELEMENT) {
                val message = PsiBuilderImpl.getErrorMessage(kid)
                errorListener.onError(kid.startOffset, kid.endOffset, message)
            }

            ref.set(null)
            reportErrors(kid, tree, errorListener, ref)
        }
    }
}
