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
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtPsiFactory

class RemoveAtmarkUsedAsAnnotationArgumentInspection : IntentionBasedInspection<KtAnnotatedExpression>(RemoveAtmarkUsedAsAnnotationArgumentIntention())

class RemoveAtmarkUsedAsAnnotationArgumentIntention : SelfTargetingOffsetIndependentIntention<KtAnnotatedExpression>(KtAnnotatedExpression::class.java, "Remove @ used as annotation argument") {
    override fun isApplicableTo(expression: KtAnnotatedExpression): Boolean {
        var parent = expression.parent
        while (parent != null) {
            if (parent is KtAnnotationEntry) return true
            parent = parent.parent
        }
        return false
    }

    override fun applyTo(expression: KtAnnotatedExpression, editor: Editor?) {
        val element = KtPsiFactory(expression.project).createExpression(expression.text.replaceFirst("@", ""))
        expression.replace(element)
    }
}