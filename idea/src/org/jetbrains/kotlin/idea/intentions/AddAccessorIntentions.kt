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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory

abstract class AbstractAddAccessorsIntention(
        private val addGetter: Boolean,
        private val addSetter: Boolean
) : SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, createFamilyName(addGetter, addSetter)) {

    override fun applicabilityRange(element: KtProperty): TextRange? {
        if (element.isLocal || element.isAbstract() || element.hasDelegate() ||
            element.hasModifier(KtTokens.LATEINIT_KEYWORD) ||
            element.hasModifier(KtTokens.CONST_KEYWORD)) {
            return null
        }
        val descriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return null
        if (descriptor.isHeader) return null

        if (addSetter && (!element.isVar || element.setter != null)) return null
        if (addGetter && element.getter != null) return null
        return element.nameIdentifier?.textRange
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val psiFactory = KtPsiFactory(element)
        if (addGetter) {
            val expression = psiFactory.createExpression("field")
            val getter = psiFactory.createPropertyGetter(expression)
            if (element.setter != null)
                element.addBefore(getter, element.setter)
            else
                element.add(getter)
        }
        if (addSetter) {
            val expression = psiFactory.createBlock("field = value")
            val setter = psiFactory.createPropertySetter(expression)
            element.add(setter)
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
