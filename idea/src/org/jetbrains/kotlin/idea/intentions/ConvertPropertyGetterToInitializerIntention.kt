/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class ConvertPropertyGetterToInitializerIntention : SelfTargetingIntention<KtPropertyAccessor>(
    KtPropertyAccessor::class.java, "Convert property getter to initializer"
) {

    override fun isApplicableTo(element: KtPropertyAccessor, caretOffset: Int): Boolean {
        if (!element.isGetter || element.singleExpression() == null) return false

        val property = element.parent as? KtProperty ?: return false
        if (property.hasInitializer()
            || property.receiverTypeReference != null
            || property.containingClass()?.isInterface() == true
            || (property.descriptor as? PropertyDescriptor)?.isExpect == true
        ) return false

        return true
    }

    override fun applyTo(element: KtPropertyAccessor, editor: Editor?) {
        val property = element.parent as? KtProperty ?: return
        val commentSaver = CommentSaver(property)
        property.initializer = element.singleExpression()
        property.deleteChildRange(property.initializer?.nextSibling, element)
        editor?.caretModel?.moveToOffset(property.endOffset)
        commentSaver.restore(property)
    }
}

private fun KtPropertyAccessor.singleExpression(): KtExpression? {
    val bodyExpression = this.bodyExpression
    return if (bodyExpression is KtBlockExpression)
        (bodyExpression.statements.singleOrNull() as? KtReturnExpression)?.returnedExpression
    else
        bodyExpression
}
