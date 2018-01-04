/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.TokenType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isUnit

fun provideLambdaImplicitHints(lambda: KtLambdaExpression): List<InlayInfo> {
    val lbrace = lambda.leftCurlyBrace
    if (!lbrace.isFollowedByNewLine()) {
        return emptyList()
    }
    val bindingContext = lambda.analyze(BodyResolveMode.PARTIAL)
    val functionDescriptor = bindingContext[BindingContext.FUNCTION, lambda.functionLiteral] ?: return emptyList()

    val implicitReceiver = functionDescriptor.extensionReceiverParameter
    if (implicitReceiver != null) {
        val text = buildString {
            append(TYPE_INFO_PREFIX)
            append("this: ")
            append(inlayHintsTypeRenderer.renderType(implicitReceiver.type))
        }
        return listOf(InlayInfo(text, lbrace.textRange.endOffset))
    }

    val singleParameter = functionDescriptor.valueParameters.singleOrNull()
    if (singleParameter != null && bindingContext[BindingContext.AUTO_CREATED_IT, singleParameter] == true) {
        val type = singleParameter.type
        if (type.isUnit()) return emptyList()
        val text = buildString {
            append(TYPE_INFO_PREFIX)
            append("it: ")
            append(inlayHintsTypeRenderer.renderType(type))
        }
        return listOf(InlayInfo(text, lbrace.textRange.endOffset))
    }
    return emptyList()
}

private fun ASTNode.isFollowedByNewLine(): Boolean {
    for (sibling in siblings()) {
        if (sibling.elementType != TokenType.WHITE_SPACE && sibling.psi !is PsiComment) {
            return false
        }
        if (sibling.elementType == TokenType.WHITE_SPACE && sibling.textContains('\n')) {
            return true
        }
    }
    return false
}
