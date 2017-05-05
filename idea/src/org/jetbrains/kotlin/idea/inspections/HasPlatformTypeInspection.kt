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
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.intentions.isFlexibleRecursive
import org.jetbrains.kotlin.idea.quickfix.AddExclExclCallFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.types.TypeUtils
import javax.swing.JComponent

class HasPlatformTypeInspection(
        @JvmField var publicAPIOnly: Boolean = true,
        @JvmField var reportPlatformArguments: Boolean = false
) : IntentionBasedInspection<KtCallableDeclaration>(
        SpecifyTypeExplicitlyIntention::class,
        { element, inspection ->
            with(inspection as HasPlatformTypeInspection) {
                SpecifyTypeExplicitlyIntention.dangerousFlexibleTypeOrNull(element, this.publicAPIOnly, this.reportPlatformArguments) != null
            }
        }
) {

    override fun problemHighlightType(element: KtCallableDeclaration) = ProblemHighlightType.WEAK_WARNING

    override val problemText = "Declaration has type inferred from a platform call, which can lead to unchecked nullability issues. " +
                               "Specify type explicitly as nullable or non-nullable."

    override fun additionalFixes(element: KtCallableDeclaration): List<LocalQuickFix>? {
        val type = SpecifyTypeExplicitlyIntention.dangerousFlexibleTypeOrNull(element, publicAPIOnly, reportPlatformArguments) ?: return null

        if (TypeUtils.isNullableType(type)) {
            val expression = element.node.findChildByType(KtTokens.EQ)?.psi?.getNextSiblingIgnoringWhitespaceAndComments()
            if (expression != null &&
                (!reportPlatformArguments || !TypeUtils.makeNotNullable(type).isFlexibleRecursive())) {
                return listOf(IntentionWrapper(AddExclExclCallFix(expression), element.containingFile))
            }
        }

        return null
    }

    override fun inspectionTarget(element: KtCallableDeclaration) = element.nameIdentifier

    override fun createOptionsPanel(): JComponent? {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Apply only to public or protected members", "publicAPIOnly")
        panel.addCheckbox("Report for types with platform arguments", "reportPlatformArguments")
        return panel
    }
}