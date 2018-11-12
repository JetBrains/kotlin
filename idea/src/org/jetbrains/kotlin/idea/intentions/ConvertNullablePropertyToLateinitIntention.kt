/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class ConvertNullablePropertyToLateinitIntention : SelfTargetingIntention<KtProperty>(
    KtProperty::class.java, "Convert to lateinit var"
) {
    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        return !element.hasModifier(KtTokens.LATEINIT_KEYWORD)
                && element.isVar
                && element.typeReference?.typeElement is KtNullableType
                && element.initializer.isNullExpression()
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val typeReference: KtTypeReference = element.typeReference ?: return
        val notNullableType = element.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference]?.makeNotNullable() ?: return
        element.addModifier(KtTokens.LATEINIT_KEYWORD)
        element.setType(notNullableType)
        element.initializer = null
    }
}
