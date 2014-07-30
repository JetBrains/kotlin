/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion.handlers

import com.google.common.collect.Sets
import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetFunction
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lexer.JetTokens

public object JetKeywordInsertHandler : InsertHandler<LookupElement> {
    private val NO_SPACE_AFTER = setOf(JetTokens.THIS_KEYWORD.toString(),
                                       JetTokens.SUPER_KEYWORD.toString(),
                                       JetTokens.CAPITALIZED_THIS_KEYWORD.toString(),
                                       JetTokens.THIS_KEYWORD.toString(),
                                       JetTokens.FALSE_KEYWORD.toString(),
                                       JetTokens.NULL_KEYWORD.toString(),
                                       JetTokens.BREAK_KEYWORD.toString(),
                                       JetTokens.CONTINUE_KEYWORD.toString())

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val keyword = item.getLookupString()

        if (keyword in NO_SPACE_AFTER) return

        if (keyword == JetTokens.RETURN_KEYWORD.toString()) {
            val element = context.getFile().findElementAt(context.getStartOffset())
            if (element != null) {
                val jetFunction = PsiTreeUtil.getParentOfType(element, javaClass<JetFunction>())
                if (jetFunction != null && (!jetFunction.hasDeclaredReturnType() || JetPsiUtil.isVoidType(jetFunction.getReturnTypeRef()))) {
                    // No space for void function
                    return
                }
            }
        }

        // Add space after keyword
        context.setAddCompletionChar(false)
        TailType.SPACE.processTail(context.getEditor(), context.getTailOffset())
    }
}
