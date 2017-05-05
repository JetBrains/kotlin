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
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveEmptyClassBodyInspection :
        IntentionBasedInspection<KtClassBody>(RemoveEmptyClassBodyIntention::class), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtClassBody): ProblemHighlightType =
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveEmptyClassBodyIntention : SelfTargetingOffsetIndependentIntention<KtClassBody>(KtClassBody::class.java, "Remove empty class body") {
    override fun applyTo(element: KtClassBody, editor: Editor?) = element.delete()

    override fun isApplicableTo(element: KtClassBody): Boolean {
        element.getStrictParentOfType<KtObjectDeclaration>()?.let {
            if (it.isObjectLiteral()) return false
        }

        element.getStrictParentOfType<KtClass>()?.let {
            if (!it.isTopLevel() && it.getNextSiblingIgnoringWhitespaceAndComments() is KtSecondaryConstructor) return false
        }

        return element.text.replace("{", "").replace("}", "").isBlank()
    }
}