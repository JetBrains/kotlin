/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.isJs

class ConvertUnsafeCastCallToUnsafeCastIntention : SelfTargetingIntention<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java, "Convert to unsafe cast"
) {

    override fun isApplicableTo(element: KtDotQualifiedExpression, caretOffset: Int): Boolean {
        if (!element.platform.isJs()) return false
        if ((element.selectorExpression as? KtCallExpression)?.calleeExpression?.text != "unsafeCast") return false

        val fqName = element.resolveToCall()?.resultingDescriptor?.fqNameOrNull()?.asString() ?: return false
        if (fqName != "kotlin.js.unsafeCast") return false

        val type = element.callExpression?.typeArguments?.singleOrNull() ?: return false

        text = "Convert to '${element.receiverExpression.text} as ${type.text}'"
        return true
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val type = element.callExpression?.typeArguments?.singleOrNull() ?: return
        val newExpression = KtPsiFactory(element).createExpressionByPattern("$0 as $1", element.receiverExpression, type.text)
        element.replace(newExpression)
    }

}