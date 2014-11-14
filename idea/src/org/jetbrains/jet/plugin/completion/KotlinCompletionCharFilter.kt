/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.CharFilter.Result
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.util.Key
import org.jetbrains.jet.lang.descriptors.VariableDescriptor

public class KotlinCompletionCharFilter() : CharFilter() {
    class object {
        public val ACCEPT_OPENING_BRACE: Key<Boolean> = Key("KotlinCompletionCharFilter.ACCEPT_OPENNING_BRACE")
        public val ACCEPT_EQ: Key<Boolean> = Key("KotlinCompletionCharFilter.ACCEPT_EQ")

        public val SELECTED_ITEM_PREFIX: Key<String> = Key("KotlinCompletionCharFilter.SELECTED_ITEM_PREFIX")
    }

    override fun acceptChar(c : Char, prefixLength : Int, lookup : Lookup) : Result? {
        if (lookup.getPsiFile() !is JetFile) return null
        if (!lookup.isCompletion()) return null

        if (Character.isJavaIdentifierPart(c) || c == ':' /* used in '::xxx'*/) {
            return CharFilter.Result.ADD_TO_PREFIX
        }

        val currentItem = lookup.getCurrentItem()
        currentItem?.putUserData(SELECTED_ITEM_PREFIX, lookup.itemPattern(currentItem))

        return when (c) {
            '.' -> {
                //TODO: this heuristics better to be only used for auto-popup completion but I see no way to check this
                if (prefixLength == 0 && !lookup.isSelectionTouched()) {
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

            '=' -> {
                if (currentItem != null && currentItem.getUserData(ACCEPT_EQ) ?: false)
                    Result.SELECT_ITEM_AND_FINISH_LOOKUP
                else
                    Result.HIDE_LOOKUP //TODO: why not for others?
            }

            ',', ' ', '(' -> Result.SELECT_ITEM_AND_FINISH_LOOKUP

            else -> CharFilter.Result.HIDE_LOOKUP
        }
    }
}
