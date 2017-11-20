/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.isError

class SpecifyTypeExplicitlyInDestructuringAssignmentIntention : SelfTargetingRangeIntention<KtDestructuringDeclaration>(
        KtDestructuringDeclaration::class.java, "Specify all types explicitly in destructuring assignment"
), LowPriorityAction {

    override fun applicabilityRange(element: KtDestructuringDeclaration): TextRange? {
        if (element.containingFile is KtCodeFragment) return null
        val entries = element.noTypeReferenceEntries()
        if (entries.isEmpty()) return null
        if (entries.any { SpecifyTypeExplicitlyIntention.getTypeForDeclaration(it).isError }) return null
        return TextRange(element.startOffset, element.initializer?.let { it.startOffset - 1 } ?: element.endOffset)
    }

    override fun applyTo(element: KtDestructuringDeclaration, editor: Editor?) {
        val entries = element.noTypeReferenceEntries()
        if (editor != null && element.getParentOfType<KtParameterList>(false) == null)
            SpecifyTypeExplicitlyIntention.addTypeAnnotationWithTemplate(editor, entries.iterator())
        else
            entries.forEach {
                it.setType(SpecifyTypeExplicitlyIntention.getTypeForDeclaration(it))
            }
    }
}

private fun KtDestructuringDeclaration.noTypeReferenceEntries() = entries.filter { it.typeReference == null }
