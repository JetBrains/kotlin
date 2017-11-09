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

import com.intellij.codeInsight.completion.CompletionService
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.KtFile

class KotlinCompletionCharFilter() : CharFilter() {
    companion object {
        val ACCEPT_OPENING_BRACE: Key<Unit> = Key("KotlinCompletionCharFilter.ACCEPT_OPENING_BRACE")

        val SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING: Key<Unit> = Key("KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING")
        val HIDE_LOOKUP_ON_COLON: Key<Unit> = Key("KotlinCompletionCharFilter.HIDE_LOOKUP_ON_COLON")

        val JUST_TYPING_PREFIX: Key<String> = Key("KotlinCompletionCharFilter.JUST_TYPING_PREFIX")
    }

    override fun acceptChar(c : Char, prefixLength : Int, lookup : Lookup) : Result? {
        if (lookup.psiFile !is KtFile) return null
        if (!lookup.isCompletion) return null
        val isAutopopup = CompletionService.getCompletionService().currentCompletion!!.isAutopopupCompletion

        if (Character.isJavaIdentifierPart(c) || c == '@') {
            return CharFilter.Result.ADD_TO_PREFIX
        }

        val currentItem = lookup.currentItem

        // do not accept items by special chars in some special positions such as in the very beginning of function literal where name of the first parameter can be
        if (isAutopopup && !lookup.isSelectionTouched && currentItem?.getUserData(SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING) != null) {
            return Result.HIDE_LOOKUP
        }

        if (c == ':') {
            return when {
                currentItem?.getUserData(HIDE_LOOKUP_ON_COLON) != null -> Result.HIDE_LOOKUP
                else -> CharFilter.Result.ADD_TO_PREFIX /* used in '::xxx'*/
            }
        }

        if (!lookup.isSelectionTouched) {
            currentItem?.putUserDataDeep(JUST_TYPING_PREFIX, lookup.itemPattern(currentItem))
        }

        return when (c) {
            '.' -> {
                if (prefixLength == 0 && isAutopopup && !lookup.isSelectionTouched) {
                    val caret = lookup.editor.caretModel.offset
                    if (caret > 0 && lookup.editor.document.charsSequence[caret - 1] == '.') {
                        return Result.HIDE_LOOKUP
                    }
                }
                Result.SELECT_ITEM_AND_FINISH_LOOKUP
            }

            '{' -> {
                if (currentItem?.getUserData(ACCEPT_OPENING_BRACE) != null)
                    Result.SELECT_ITEM_AND_FINISH_LOOKUP
                else
                    Result.HIDE_LOOKUP
            }

            ',', ' ', '(', '=', '!' -> Result.SELECT_ITEM_AND_FINISH_LOOKUP

            else -> CharFilter.Result.HIDE_LOOKUP
        }
    }
}
