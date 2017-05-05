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

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

class RemoveEmptySecondaryConstructorBodyInspection : IntentionBasedInspection<KtBlockExpression>(
        RemoveEmptySecondaryConstructorBodyIntention::class
), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtBlockExpression): ProblemHighlightType =
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveEmptySecondaryConstructorBodyIntention : SelfTargetingOffsetIndependentIntention<KtBlockExpression>(KtBlockExpression::class.java, "Remove empty constructor body") {

    override fun applyTo(element: KtBlockExpression, editor: Editor?) = element.delete()

    override fun isApplicableTo(element: KtBlockExpression): Boolean {
        if(element.parent !is KtSecondaryConstructor) return false
        if(element.statements.isNotEmpty()) return false

        return element.text.replace("{", "").replace("}", "").isBlank()
    }

}