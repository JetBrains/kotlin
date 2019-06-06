/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.resolvedToArrayType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class ReplaceArrayEqualityOpWithArraysEqualsInspection : AbstractApplicabilityBasedInspection<KtBinaryExpression>(
    KtBinaryExpression::class.java
) {
    override fun applyTo(element: KtBinaryExpression, project: Project, editor: Editor?) {
        val right = element.right ?: return
        val left = element.left ?: return
        val factory = KtPsiFactory(project)
        val template = buildString {
            if (element.operationToken == KtTokens.EXCLEQ) append("!")
            append("$0.contentEquals($1)")
        }
        element.replace(factory.createExpressionByPattern(template, left, right))
    }

    override fun isApplicable(element: KtBinaryExpression): Boolean {
        when (element.operationToken) {
            KtTokens.EQEQ, KtTokens.EXCLEQ -> {
            }
            else -> return false
        }
        val right = element.right
        val left = element.left
        if (right == null || left == null) return false
        val context = element.analyze()
        val rightResolvedCall = right.getResolvedCall(context)
        val leftResolvedCall = left.getResolvedCall(context)
        return rightResolvedCall?.resolvedToArrayType() == true && leftResolvedCall?.resolvedToArrayType() == true
    }

    override fun inspectionText(element: KtBinaryExpression) = "Dangerous array comparison"

    override val defaultFixText: String
        get() = "Replace with 'contentEquals'"

    override fun fixText(element: KtBinaryExpression): String = when (element.operationToken) {
        KtTokens.EQEQ -> "Replace '==' with 'contentEquals'"
        KtTokens.EXCLEQ -> "Replace '!=' with 'contentEquals'"
        else -> ""
    }
}
