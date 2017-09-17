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
import org.jetbrains.kotlin.psi.createExpressionByPattern
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
        val ktFile = element.containingKtFile
        ktFile.resolveImportReference(FqName("java.util.Arrays")).firstOrNull()?.let {
            ImportInsertHelper.getInstance(project).importDescriptor(ktFile, it)
        }
        val template = buildString {
            if (element.operationToken == KtTokens.EXCLEQ) append("!")
            append("Arrays.equals($0, $1)")
        }
        val expression = factory.createExpressionByPattern(template, left, right)
        element.replace(expression)
    }

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        val operationToken = element.operationToken
        if (operationToken != KtTokens.EQEQ && operationToken != KtTokens.EXCLEQ) return false
        if (operationToken == KtTokens.EXCLEQ) text = "Replace '!=' with 'Arrays.equals'"
        val right = element.right ?: return false
        val left = element.left ?: return false
        val rightResolvedCall = right.getResolvedCall(right.analyze()) ?: return false

        if (!rightResolvedCall.resolvedToArrayType()) return false
        val leftResolvedCall = left.getResolvedCall(left.analyze()) ?: return false
        return leftResolvedCall.resolvedToArrayType()
    }
}