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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

internal fun KtExpression.getArguments() = when (this) {
    is KtBinaryExpression -> this.left to this.right
    is KtDotQualifiedExpression -> this.receiverExpression to this.callExpression?.valueArguments?.singleOrNull()?.getArgumentExpression()
    else -> null
}

class ReplaceUntilWithRangeToIntention : SelfTargetingIntention<KtExpression>(KtExpression::class.java, "Replace with '..' operator") {
    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        if (element !is KtBinaryExpression && element !is KtDotQualifiedExpression) return false
        val fqName = element.getCallableDescriptor()?.fqNameUnsafe?.asString() ?: return false
        return fqName == "kotlin.ranges.until"
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        val args = element.getArguments() ?: return
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0..$1 - 1", args.first ?: return, args.second ?: return))
    }
}