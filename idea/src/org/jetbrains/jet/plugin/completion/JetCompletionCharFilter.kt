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
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler
import com.intellij.openapi.util.Key

public open class JetCompletionCharFilter() : CharFilter() {
    class object {
        public val ACCEPT_OPENING_BRACE: Key<Boolean> = Key<Boolean>("JetCompletionCharFilter.ACCEPT_OPENNING_BRACE")
        public val ACCEPT_EQ: Key<Boolean> = Key<Boolean>("JetCompletionCharFilter.ACCEPT_EQ")
    }

    public override fun acceptChar(c : Char, prefixLength : Int, lookup : Lookup) : Result? {
        if (lookup.getPsiFile() !is JetFile) return null

        if (c == '.' && prefixLength == 0 && !lookup.isSelectionTouched()) {
            val caret = lookup.getEditor().getCaretModel().getOffset()
            if (caret > 0 && (lookup.getEditor().getDocument().getCharsSequence().charAt(caret - 1)) == '.') {
                return Result.HIDE_LOOKUP
            }
        }

        if (c == '{') {
            val currentItem = lookup.getCurrentItem()
            if (currentItem != null && currentItem.getUserData(ACCEPT_OPENING_BRACE) ?: false) {
                return Result.SELECT_ITEM_AND_FINISH_LOOKUP
            }
        }

        if (c == '=') {
            val currentItem = lookup.getCurrentItem()
            if (currentItem != null && currentItem.getUserData(ACCEPT_EQ) ?: false) {
                return Result.SELECT_ITEM_AND_FINISH_LOOKUP
            }
        }

        return null
    }
}
