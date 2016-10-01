/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class ReplaceArrayEqualityOpWithArraysEqualsInspection :
        IntentionBasedInspection<KtBinaryExpression>(ReplaceArrayEqualityOpWithArraysEqualsIntention::class)

class ReplaceArrayEqualityOpWithArraysEqualsIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
        KtBinaryExpression::class.java,
        "Replace '==' with 'Arrays.equals'"
) {

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val project = element.project
        val right = element.right ?: return
        val left = element.left ?: return
        val factory = KtPsiFactory(project)
        val ktFile = element.getContainingKtFile()
        ktFile.resolveImportReference(FqName("java.util.Arrays")).firstOrNull()?.let {
            ImportInsertHelper.getInstance(project).importDescriptor(ktFile, it)
        }
        val expression = factory.createExpression("Arrays.equals(${left.text}, ${right.text})")
        element.replace(expression)
    }

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        if (element.operationToken != KtTokens.EQEQ) return false
        val right = element.right ?: return false
        val left = element.left ?: return false
        val rightResolvedCall = right.getResolvedCall(right.analyze()) ?: return false

        if (!rightResolvedCall.resolvedToArrayType()) return false
        val leftResolvedCall = left.getResolvedCall(left.analyze()) ?: return false
        return leftResolvedCall.resolvedToArrayType()
    }
}