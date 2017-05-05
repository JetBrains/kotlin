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
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class RemoveEmptyPrimaryConstructorInspection : IntentionBasedInspection<KtPrimaryConstructor>(
        RemoveEmptyPrimaryConstructorIntention::class
), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtPrimaryConstructor): ProblemHighlightType =
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveEmptyPrimaryConstructorIntention : SelfTargetingOffsetIndependentIntention<KtPrimaryConstructor>(KtPrimaryConstructor::class.java, "Remove empty primary constructor") {

    override fun applyTo(element: KtPrimaryConstructor, editor: Editor?) = element.delete()

    override fun isApplicableTo(element: KtPrimaryConstructor) = when {
        element.valueParameters.isNotEmpty() -> false
        element.annotations.isNotEmpty() -> false
        element.modifierList?.text?.isBlank() == false -> false
        element.containingClass()?.secondaryConstructors?.isNotEmpty() == true -> false
        else -> true
    }
}
