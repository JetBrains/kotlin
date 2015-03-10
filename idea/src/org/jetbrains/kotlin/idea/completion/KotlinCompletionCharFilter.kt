/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.CharFilter.Result
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.util.Key
import com.intellij.codeInsight.completion.CompletionService
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.prevLeafSkipWhitespacesAndComments
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetFunctionLiteral

public class KotlinCompletionCharFilter() : CharFilter() {
    default object {
        public val ACCEPT_OPENING_BRACE: Key<Boolean> = Key("KotlinCompletionCharFilter.ACCEPT_OPENNING_BRACE")

        public val JUST_TYPING_PREFIX: Key<String> = Key("KotlinCompletionCharFilter.JUST_TYPING_PREFIX")
    }

    override fun acceptChar(c : Char, prefixLength : Int, lookup : Lookup) : Result? {
        if (lookup.getPsiFile() !is JetFile) return null
        if (!lookup.isCompletion()) return null
        // it does not work in tests, so we use other way
//        val isAutopopup = CompletionService.getCompletionService().getCurrentCompletion().isAutopopupCompletion()
        val completionParameters = (CompletionService.getCompletionService().getCurrentCompletion() as CompletionProgressIndicator).getParameters()
        val isAutopopup = completionParameters.getInvocationCount() == 0

        if (Character.isJavaIdentifierPart(c) || c == '@') {
            return CharFilter.Result.ADD_TO_PREFIX
        }

        // do not accept items by special chars in the very beginning of function literal where name of the first parameter can be
        if (isAutopopup && !lookup.isSelectionTouched() && isInFunctionLiteralStart(completionParameters.getPosition())) {
            return Result.HIDE_LOOKUP
        }

        if (c == ':' /* used in '::xxx'*/) {
            return CharFilter.Result.ADD_TO_PREFIX
        }

        val currentItem = lookup.getCurrentItem()
        if (!lookup.isSelectionTouched()) {
            currentItem?.putUserData(JUST_TYPING_PREFIX, lookup.itemPattern(currentItem))
        }

        return when (c) {
            '.' -> {
                if (prefixLength == 0 && isAutopopup && !lookup.isSelectionTouched()) {
                    val caret = lookup.getEditor().getCaretModel().getOffset()
                    if (caret > 0 && lookup.getEditor().getDocument().getCharsSequence()[caret - 1] == '.') {
                        return Result.HIDE_LOOKUP
                    }
                }
                Result.SELECT_ITEM_AND_FINISH_LOOKUP
            }

            '{' -> {
                if (currentItem != null && currentItem.getUserData(ACCEPT_OPENING_BRACE) ?: false)
                    Result.SELECT_ITEM_AND_FINISH_LOOKUP
                else
                    Result.HIDE_LOOKUP
            }

            ',', ' ', '(', '=' -> Result.SELECT_ITEM_AND_FINISH_LOOKUP

            else -> CharFilter.Result.HIDE_LOOKUP
        }
    }

    private fun isInFunctionLiteralStart(position: PsiElement): Boolean {
        var prev = position.prevLeafSkipWhitespacesAndComments()
        if (prev?.getNode()?.getElementType() == JetTokens.LPAR) {
            prev = prev?.prevLeafSkipWhitespacesAndComments()
        }
        if (prev?.getNode()?.getElementType() != JetTokens.LBRACE) return false
        val functionLiteral = prev!!.getParent() as? JetFunctionLiteral ?: return false
        return functionLiteral.getLBrace() == prev
    }
}
