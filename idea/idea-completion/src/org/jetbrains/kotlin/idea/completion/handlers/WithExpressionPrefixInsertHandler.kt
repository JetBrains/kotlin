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
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class WithExpressionPrefixInsertHandler(val prefix: String) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        item.handleInsert(context)

        postHandleInsert(context)
    }

    fun postHandleInsert(context: InsertionContext) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments()

        val offset = context.getStartOffset()
        val token = context.getFile().findElementAt(offset)!!
        var expression = token.getStrictParentOfType<JetExpression>() ?: return
        if (expression is JetSimpleNameExpression) {
            var parent = expression.getParent()
            if (parent is JetCallExpression && expression == parent.getCalleeExpression()) {
                expression = parent
                parent = parent.getParent()
            }
            if (parent is JetDotQualifiedExpression && expression == parent.getSelectorExpression()) {
                expression = parent
            }
        }

        context.getDocument().insertString(expression.getTextRange().getStartOffset(), prefix)
    }
}