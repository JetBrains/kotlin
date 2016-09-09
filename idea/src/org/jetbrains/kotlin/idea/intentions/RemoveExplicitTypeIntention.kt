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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStartOffsetIn
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class RemoveSetterParameterTypeInspection()
: IntentionBasedInspection<KtCallableDeclaration>(
        RemoveExplicitTypeIntention::class,
        { it -> RemoveExplicitTypeIntention.isSetterParameter(it) }
) {
    override val problemHighlightType = ProblemHighlightType.LIKE_UNUSED_SYMBOL

    override fun inspectionRange(element: KtCallableDeclaration) = (element as? KtParameter)?.typeReference?.let {
        val start = it.getStartOffsetIn(element)
        TextRange(start, start + it.endOffset - it.startOffset)
    }
}

class RemoveExplicitTypeIntention : SelfTargetingRangeIntention<KtCallableDeclaration>(
        KtCallableDeclaration::class.java,
        "Remove explicit type specification"
) {

    override fun applicabilityRange(element: KtCallableDeclaration): TextRange? {
        if (element.containingFile is KtCodeFragment) return null
        if (element.typeReference == null) return null

        if (element is KtParameter && (element.isLoopParameter || element.isSetterParameter)) {
            return element.textRange
        }

        val initializer = (element as? KtDeclarationWithInitializer)?.initializer ?: return null
        if (element !is KtProperty && (element !is KtNamedFunction || element.hasBlockBody())) return null

        return TextRange(element.startOffset, initializer.startOffset - 1)
    }

    override fun applyTo(element: KtCallableDeclaration, editor: Editor?) {
        element.typeReference = null
    }

    companion object {
        fun isSetterParameter(element: KtCallableDeclaration) =
                element is KtParameter && element.isSetterParameter

        private val KtParameter.isSetterParameter: Boolean get() = (parent?.parent as? KtPropertyAccessor)?.isSetter ?: false
    }
}