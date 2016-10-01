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
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

abstract class ReplaceSizeCheckIntention(text: String) : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
        KtBinaryExpression::class.java, text) {

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val target = getTargetExpression(element)
        if (target !is KtDotQualifiedExpression) return
        val createExpression = KtPsiFactory(element).createExpression("${target.receiverExpression.text}.${getGenerateMethodSymbol()}")
        element.replaced(createExpression)
    }

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        val targetExpression = getTargetExpression(element) ?: return false
        return targetExpression.isSizeOrLength()
    }

    abstract fun getTargetExpression(element: KtBinaryExpression): KtExpression?

    abstract fun getGenerateMethodSymbol(): String
}