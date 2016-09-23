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
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

abstract class ReplaceMathMethodsWithKotlinNativeMethodsIntention(
        text: String, val replacedMethodName: String, val mathMethodName: String
) : SelfTargetingOffsetIndependentIntention<KtCallExpression>(KtCallExpression::class.java, text) {

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val target = element.getStrictParentOfType<KtDotQualifiedExpression>() ?: element
        val valueArguments = element.valueArguments
        val methodName = replacedMethodName
        val newExpression = KtPsiFactory(element).createExpressionByPattern("$0.$methodName($1)",
                                                                            valueArguments[0].text, valueArguments[1].text)
        target.replaced(newExpression)
    }

    override fun isApplicableTo(element: KtCallExpression) =
            element.calleeExpression?.text == mathMethodName &&
            element.valueArguments.size == 2 &&
            element.isMethodCall("java.lang.Math.${mathMethodName}")
}