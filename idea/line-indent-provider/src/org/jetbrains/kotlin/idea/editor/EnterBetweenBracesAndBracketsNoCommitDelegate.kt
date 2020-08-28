/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.editorActions.enter.EnterBetweenBracesNoCommitDelegate
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens

class EnterBetweenBracesAndBracketsNoCommitDelegate : EnterBetweenBracesNoCommitDelegate() {
    override fun isCommentType(type: IElementType?): Boolean = type in KtTokens.COMMENTS

    override fun isBracePair(lBrace: Char, rBrace: Char): Boolean = super.isBracePair(lBrace, rBrace) || (lBrace == '[' && rBrace == ']')
}
