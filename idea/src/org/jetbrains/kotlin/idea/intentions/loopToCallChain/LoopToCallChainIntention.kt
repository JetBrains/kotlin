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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class LoopToCallChainInspection : IntentionBasedInspection<KtForExpression>(
        intention = LoopToCallChainIntention::class,
        problemText = "Loop can be replaced with stdlib operations")

class LoopToCallChainIntention : SelfTargetingRangeIntention<KtForExpression>(
        KtForExpression::class.java,
        "Replace with stdlib operations"
) {
    override fun applicabilityRange(element: KtForExpression): TextRange? {
        return if (match(element) != null) element.forKeyword.textRange else null
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val match = match(element)!!
        val result = convertLoop(element, match)
        editor?.moveCaret(result.startOffset)
    }
}