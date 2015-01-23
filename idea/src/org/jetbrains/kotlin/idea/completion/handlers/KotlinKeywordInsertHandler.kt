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

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.lexer.JetTokens.*

public object KotlinKeywordInsertHandler : InsertHandler<LookupElement> {
    private val NO_SPACE_AFTER = listOf(THIS_KEYWORD,
                                        SUPER_KEYWORD,
                                        FOR_KEYWORD,
                                        NULL_KEYWORD,
                                        TRUE_KEYWORD,
                                        FALSE_KEYWORD,
                                        BREAK_KEYWORD,
                                        CONTINUE_KEYWORD,
                                        IF_KEYWORD,
                                        ELSE_KEYWORD,
                                        WHILE_KEYWORD,
                                        DO_KEYWORD,
                                        TRY_KEYWORD,
                                        WHEN_KEYWORD,
                                        FILE_KEYWORD,
                                        CATCH_KEYWORD,
                                        FINALLY_KEYWORD,
                                        DYNAMIC_KEYWORD).map { it.getValue() }

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val keyword = item.getLookupString()
        if (keyword == FILE_KEYWORD.getValue()) {
            WithTailInsertHandler.colonTail().postHandleInsert(context, item)
        }
        else if (keyword !in NO_SPACE_AFTER) {
            WithTailInsertHandler.spaceTail().postHandleInsert(context, item)
        }
    }
}
