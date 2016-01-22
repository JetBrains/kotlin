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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class WithExpressionPrefixInsertHandler(val prefix: String) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        item.handleInsert(context)

        postHandleInsert(context)
    }

    fun postHandleInsert(context: InsertionContext) {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)
        psiDocumentManager.commitAllDocuments()

        val offset = context.startOffset
        val token = context.file.findElementAt(offset)!!
        var expression = token.getStrictParentOfType<KtExpression>() ?: return
        if (expression is KtSimpleNameExpression) {
            var parent = expression.getParent()
            if (parent is KtCallExpression && expression == parent.calleeExpression) {
                expression = parent
                parent = parent.getParent()
            }
            if (parent is KtDotQualifiedExpression && expression == parent.selectorExpression) {
                expression = parent
            }
        }

        context.document.insertString(expression.textRange.startOffset, prefix)
    }
}