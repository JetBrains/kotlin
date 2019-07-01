/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
        KtDestructuringDeclaration::class.java, "Specify all types explicitly in destructuring declaration"
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
