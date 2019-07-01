/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

class ConvertOrdinaryPropertyToLazyIntention : SelfTargetingIntention<KtProperty>(
    KtProperty::class.java, "Convert to lazy property"
) {
    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        return !element.isVar && element.initializer != null && element.getter == null && !element.isLocal
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val initializer = element.initializer ?: return
        val psiFactory = KtPsiFactory(element)
        val newExpression = if (initializer is KtCallExpression && initializer.isCalling(FqName("kotlin.run"))) {
            initializer.calleeExpression?.replace(psiFactory.createExpression("lazy"))
            initializer
        } else {
            psiFactory.createExpressionByPattern("lazy { $0 }", initializer)
        }
        element.addAfter(psiFactory.createPropertyDelegate(newExpression), initializer)
        element.initializer = null
    }
}
