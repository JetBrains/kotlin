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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.getSubjectToIntroduce
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceSubject
import org.jetbrains.kotlin.psi.KtWhenExpression

class IntroduceWhenSubjectInspection : IntentionBasedInspection<KtWhenExpression>(IntroduceWhenSubjectIntention::class)

class IntroduceWhenSubjectIntention : SelfTargetingRangeIntention<KtWhenExpression>(KtWhenExpression::class.java, "Introduce argument to 'when'") {
    override fun applicabilityRange(element: KtWhenExpression): TextRange? {
        val subject = element.getSubjectToIntroduce() ?: return null
        text = "Introduce '${subject.text}' as argument to 'when'"
        return element.whenKeyword.textRange
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        element.introduceSubject()
    }
}
