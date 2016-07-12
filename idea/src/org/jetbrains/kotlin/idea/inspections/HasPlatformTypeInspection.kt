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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.quickfix.AddExclExclCallFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getStartOffsetIn
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.isNullabilityFlexible
import javax.swing.JComponent

class HasPlatformTypeInspection(
        val intention: SpecifyTypeExplicitlyIntention = SpecifyTypeExplicitlyIntention(),
        @JvmField var publicAPIOnly: Boolean = true
) : IntentionBasedInspection<KtCallableDeclaration>(
        intention,
        { intention.dangerousFlexibleTypeOrNull(it, publicAPIOnly) != null }
) {

    override val problemHighlightType = ProblemHighlightType.WEAK_WARNING

    override val problemText = "Declaration has platform type. Make the type explicit to prevent subtle bugs."

    override fun additionalFixes(element: KtCallableDeclaration): List<LocalQuickFix>? {
        val type = intention.dangerousFlexibleTypeOrNull(element, publicAPIOnly) ?: return null

        if (type.isNullabilityFlexible()) {
            val expression = element.node.findChildByType(KtTokens.EQ)?.psi?.getNextSiblingIgnoringWhitespaceAndComments()
            if (expression != null) {
                return listOf(IntentionWrapper(AddExclExclCallFix(expression), element.containingFile))
            }
        }

        return null
    }

    override fun inspectionRange(element: KtCallableDeclaration) = element.nameIdentifier?.let {
        val start = it.getStartOffsetIn(element)
        TextRange(start, start + it.endOffset - it.startOffset)
    }

    override fun createOptionsPanel(): JComponent? {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Apply only to public or protected members", "publicAPIOnly")
        return panel
    }
}