/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.isJs
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils

class ConvertUnsafeCastToUnsafeCastCallIntention : SelfTargetingIntention<KtBinaryExpressionWithTypeRHS>(
    KtBinaryExpressionWithTypeRHS::class.java, "Convert to unsafeCast() call"
) {

    override fun isApplicableTo(element: KtBinaryExpressionWithTypeRHS, caretOffset: Int): Boolean {
        if (!element.platform.isJs()) return false

        if (element.operationReference.getReferencedNameElementType() != KtTokens.AS_KEYWORD) return false

        val right = element.right ?: return false
        val context = right.analyze(BodyResolveMode.PARTIAL)
        val type = context[BindingContext.TYPE, right] ?: return false
        if (TypeUtils.isNullableType(type)) return false

        text = "Convert to '${element.left.text}.unsafeCast<${right.text}>()'"
        return true
    }

    override fun applyTo(element: KtBinaryExpressionWithTypeRHS, editor: Editor?) {
        val right = element.right ?: return
        val newExpression = KtPsiFactory(element).createExpressionByPattern("$0.unsafeCast<$1>()", element.left, right)
        element.replace(newExpression)
    }

}