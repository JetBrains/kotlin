/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
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
                (added as? KtPropertyAccessor)?.bodyBlockExpression?.statements?.firstOrNull()?.also {
                    editor?.caretModel?.moveToOffset(it.startOffset)
                }
            }
        }
        if (addSetter) {
            val expression = if (hasInitializer) psiFactory.createBlock("field = value") else psiFactory.createEmptyBody()
            val setter = psiFactory.createPropertySetter(expression)
            val added = element.add(setter)
            if (!hasInitializer && !addGetter) {
                (added as? KtPropertyAccessor)?.bodyBlockExpression?.lBrace?.also {
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

private fun createFamilyName(addGetter: Boolean, addSetter: Boolean): String = when {
    addGetter && addSetter -> "Add getter and setter"
    addGetter -> "Add getter"
    addSetter -> "Add setter"
    else -> throw AssertionError("At least one from (addGetter, addSetter) should be true")
}

class AddPropertyAccessorsIntention : AbstractAddAccessorsIntention(true, true), LowPriorityAction

class AddPropertyGetterIntention : AbstractAddAccessorsIntention(true, false)

class AddPropertySetterIntention : AbstractAddAccessorsIntention(false, true)
