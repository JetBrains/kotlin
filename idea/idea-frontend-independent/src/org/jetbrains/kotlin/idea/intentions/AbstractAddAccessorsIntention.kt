/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset

abstract class AbstractAddAccessorsIntention(
    protected val addGetter: Boolean,
    protected val addSetter: Boolean
) : SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, createFamilyName(addGetter, addSetter)) {

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val hasInitializer = element.hasInitializer()
        val psiFactory = KtPsiFactory(element)
        if (addGetter) {
            val expression = if (hasInitializer) psiFactory.createExpression("field") else psiFactory.createBlock("TODO()")
            val getter = psiFactory.createPropertyGetter(expression)
            val added = if (element.setter != null) {
                element.addBefore(getter, element.setter)
            } else {
                element.add(getter)
            }
            if (!hasInitializer) {
                (added as? KtPropertyAccessor)?.bodyBlockExpression?.statements?.firstOrNull()?.let {
                    editor?.caretModel?.moveToOffset(it.startOffset)
                }
            }
        }
        if (addSetter) {
            val expression = if (hasInitializer) psiFactory.createBlock("field = value") else psiFactory.createEmptyBody()
            val setter = psiFactory.createPropertySetter(expression)
            val added = element.add(setter)
            if (!hasInitializer && !addGetter) {
                (added as? KtPropertyAccessor)?.bodyBlockExpression?.lBrace?.let {
                    editor?.caretModel?.moveToOffset(it.startOffset + 1)
                }
            }
        }
    }
}

private fun createFamilyName(addGetter: Boolean, addSetter: Boolean): () -> String = when {
    addGetter && addSetter -> KotlinBundle.lazyMessage("text.add.getter.and.setter")
    addGetter -> KotlinBundle.lazyMessage("text.add.getter")
    addSetter -> KotlinBundle.lazyMessage("text.add.setter")
    else -> throw AssertionError("At least one from (addGetter, addSetter) should be true")
}

