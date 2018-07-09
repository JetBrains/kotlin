/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class ConvertPropertyGetterToInitializerIntention : SelfTargetingIntention<KtPropertyAccessor>(
    KtPropertyAccessor::class.java, "Convert property getter to initializer"
) {

    override fun isApplicableTo(element: KtPropertyAccessor, caretOffset: Int): Boolean {
        return element.isGetter && (element.parent as? KtProperty)?.hasInitializer() != true && element.singleExpression() != null
    }

    override fun applyTo(element: KtPropertyAccessor, editor: Editor?) {
        val property = element.parent as? KtProperty ?: return
        property.initializer = element.singleExpression()
        property.deleteChildRange(property.initializer?.nextSibling, element)
        editor?.caretModel?.moveToOffset(property.endOffset)
    }
}

private fun KtPropertyAccessor.singleExpression(): KtExpression? {
    val bodyExpression = this.bodyExpression
    return if (bodyExpression is KtBlockExpression)
        (bodyExpression.statements.singleOrNull() as? KtReturnExpression)?.returnedExpression
    else
        bodyExpression
}
