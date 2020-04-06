/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.util.hasJvmFieldAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset

abstract class AbstractAddAccessorsIntention(
    private val addGetter: Boolean,
    private val addSetter: Boolean
) : SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, createFamilyName(addGetter, addSetter)) {

    override fun applicabilityRange(element: KtProperty): TextRange? {
        if (element.isLocal || element.isAbstract() || element.hasDelegate() ||
            element.hasModifier(KtTokens.LATEINIT_KEYWORD) ||
            element.hasModifier(KtTokens.CONST_KEYWORD) ||
            element.hasJvmFieldAnnotation()
        ) {
            return null
        }
        val descriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return null
        if (descriptor.isExpect) return null

        val hasInitializer = element.hasInitializer()
        if (element.typeReference == null && !hasInitializer) return null
        if (addSetter && (!element.isVar || element.setter != null)) return null
        if (addGetter && element.getter != null) return null
        return if (hasInitializer) element.nameIdentifier?.textRange else element.textRange
    }

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

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val property = diagnostic.psiElement as? KtProperty ?: return null
            return if (property.isVar) {
                val getter = property.getter
                val setter = property.setter
                when {
                    getter == null && setter == null -> AddPropertyAccessorsIntention()
                    getter == null -> AddPropertyGetterIntention()
                    else -> AddPropertySetterIntention()
                }
            } else {
                AddPropertyGetterIntention()
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

class AddPropertyAccessorsIntention : AbstractAddAccessorsIntention(true, true), LowPriorityAction

class AddPropertyGetterIntention : AbstractAddAccessorsIntention(true, false)

class AddPropertySetterIntention : AbstractAddAccessorsIntention(false, true)
