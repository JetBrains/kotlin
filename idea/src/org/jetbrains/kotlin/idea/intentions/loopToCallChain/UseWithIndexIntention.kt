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
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

class UseWithIndexInspection : IntentionBasedInspection<KtForExpression>(UseWithIndexIntention::class)

class UseWithIndexIntention : SelfTargetingRangeIntention<KtForExpression>(
        KtForExpression::class.java,
        "Use withIndex() instead of manual index increment"
) {
    override fun applicabilityRange(element: KtForExpression): TextRange? {
        return if (matchIndexToIntroduce(element) != null) element.forKeyword.textRange else null
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val (indexVariable, initializationStatement, incrementExpression) = matchIndexToIntroduce(element)!!

        val factory = KtPsiFactory(element)
        val loopRange = element.loopRange!!
        val loopParameter = element.loopParameter!!

        val newLoopRange = factory.createExpressionByPattern("$0.withIndex()", loopRange)
        loopRange.replace(newLoopRange)

        val multiParameter = (factory.createExpressionByPattern("for(($0, $1) in x){}", indexVariable.nameAsSafeName, loopParameter.text) as KtForExpression).loopParameter!!
        loopParameter.replace(multiParameter)

        initializationStatement.delete()
        if (incrementExpression.parent is KtBlockExpression) {
            incrementExpression.delete()
        }
        else {
            removePlusPlus(incrementExpression)
        }
    }
}