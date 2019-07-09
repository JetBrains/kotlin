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

package org.jetbrains.kotlin.idea.inspections.branchedTransformations

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.getSubjectToIntroduce
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.introduceSubject
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.psi.KtWhenExpression

class IntroduceWhenSubjectInspection : AbstractApplicabilityBasedInspection<KtWhenExpression>(KtWhenExpression::class.java) {

    override fun isApplicable(element: KtWhenExpression) = element.getSubjectToIntroduce() != null

    override fun inspectionHighlightRangeInElement(element: KtWhenExpression) = element.whenKeyword.textRangeIn(element)

    override fun inspectionText(element: KtWhenExpression) = "'when' with subject should be used"

    override val defaultFixText = "Introduce 'when' subject"

    override fun fixText(element: KtWhenExpression): String {
        val subject = element.getSubjectToIntroduce() ?: return ""
        return "Introduce '${subject.text}' as subject of 'when'"
    }

    override fun applyTo(element: KtWhenExpression, project: Project, editor: Editor?) {
        element.introduceSubject()
    }
}
